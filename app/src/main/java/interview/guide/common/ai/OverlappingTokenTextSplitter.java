package interview.guide.common.ai;

import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.EncodingRegistry;
import com.knuddels.jtokkit.api.EncodingType;
import com.knuddels.jtokkit.api.IntArrayList;
import org.springframework.ai.transformer.splitter.TextSplitter;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.List;

/**
 * 支持滑动窗口重叠的 Token 级别文本分块器。
 *
 * <p>与 Spring AI 的 {@code TokenTextSplitter} 不同，本实现使用滑动窗口，
 * 使相邻 chunk 之间共享 {@code chunkOverlap} 个 token，保证跨块边界的上下文连贯性。
 *
 * <h3>使用示例</h3>
 * <pre>{@code
 * TextSplitter splitter = OverlappingTokenTextSplitter.builder()
 *     .withChunkSize(500)
 *     .withChunkOverlap(50)
 *     .build();
 * List<Document> chunks = splitter.apply(List.of(document));
 * }</pre>
 *
 * @see org.springframework.ai.transformer.splitter.TokenTextSplitter
 */
public class OverlappingTokenTextSplitter extends TextSplitter {

  private static final int DEFAULT_CHUNK_SIZE = 500;
  private static final int DEFAULT_CHUNK_OVERLAP = 50;
  private static final int MIN_CHUNK_LENGTH_TO_EMBED = 5;
  private static final int MAX_NUM_CHUNKS = 10000;
  private static final EncodingType DEFAULT_ENCODING_TYPE = EncodingType.CL100K_BASE;

  private final EncodingRegistry registry = Encodings.newLazyEncodingRegistry();
  private final Encoding encoding;
  private final int chunkSize;
  private final int chunkOverlap;
  private final int minChunkLengthToEmbed;
  private final int maxNumChunks;

  private OverlappingTokenTextSplitter(EncodingType encodingType, int chunkSize, int chunkOverlap,
      int minChunkLengthToEmbed, int maxNumChunks) {
    Assert.notNull(encodingType, "encodingType must not be null");
    Assert.isTrue(chunkSize > 0, "chunkSize must be positive");
    Assert.isTrue(chunkOverlap >= 0, "chunkOverlap must be non-negative");
    Assert.isTrue(chunkOverlap < chunkSize, "chunkOverlap must be less than chunkSize");
    this.encoding = this.registry.getEncoding(encodingType);
    this.chunkSize = chunkSize;
    this.chunkOverlap = chunkOverlap;
    this.minChunkLengthToEmbed = minChunkLengthToEmbed;
    this.maxNumChunks = maxNumChunks;
  }

  public static Builder builder() {
    return new Builder();
  }

  @Override
  protected List<String> splitText(String text) {
    if (text == null || text.trim().isEmpty()) {
      return new ArrayList<>();
    }

    List<Integer> tokens = getEncodedTokens(text);
    if (tokens.isEmpty()) {
      return new ArrayList<>();
    }

    // 短文本不需要切分
    if (tokens.size() <= chunkSize) {
      String trimmed = text.trim();
      if (trimmed.length() >= minChunkLengthToEmbed) {
        return List.of(trimmed);
      }
      return new ArrayList<>();
    }

    List<String> chunks = new ArrayList<>();
    int stride = chunkSize - chunkOverlap; // 窗口步长
    int numChunks = 0;
    int start = 0;

    while (start < tokens.size() && numChunks < maxNumChunks) {
      int end = Math.min(start + chunkSize, tokens.size());
      List<Integer> chunkTokens = tokens.subList(start, end);
      String chunkText = decodeTokens(chunkTokens).trim();

      if (!chunkText.isEmpty() && chunkText.length() >= minChunkLengthToEmbed) {
        chunks.add(chunkText);
        numChunks++;
      }

      // 最后一窗不足 stride 时退出，避免产生无意义的尾部碎片
      if (end == tokens.size()) {
        break;
      }

      start += stride;
    }

    return chunks;
  }

  private List<Integer> getEncodedTokens(String text) {
    Assert.notNull(text, "Text must not be null");
    return this.encoding.encode(text).boxed();
  }

  private String decodeTokens(List<Integer> tokens) {
    Assert.notNull(tokens, "Tokens must not be null");
    var tokensIntArray = new IntArrayList(tokens.size());
    tokens.forEach(tokensIntArray::add);
    return this.encoding.decode(tokensIntArray);
  }

  public static final class Builder {

    private EncodingType encodingType = DEFAULT_ENCODING_TYPE;
    private int chunkSize = DEFAULT_CHUNK_SIZE;
    private int chunkOverlap = DEFAULT_CHUNK_OVERLAP;
    private int minChunkLengthToEmbed = MIN_CHUNK_LENGTH_TO_EMBED;
    private int maxNumChunks = MAX_NUM_CHUNKS;

    private Builder() {
    }

    public Builder withEncodingType(EncodingType encodingType) {
      this.encodingType = encodingType;
      return this;
    }

    public Builder withChunkSize(int chunkSize) {
      this.chunkSize = chunkSize;
      return this;
    }

    public Builder withChunkOverlap(int chunkOverlap) {
      this.chunkOverlap = chunkOverlap;
      return this;
    }

    public Builder withMinChunkLengthToEmbed(int minChunkLengthToEmbed) {
      this.minChunkLengthToEmbed = minChunkLengthToEmbed;
      return this;
    }

    public Builder withMaxNumChunks(int maxNumChunks) {
      this.maxNumChunks = maxNumChunks;
      return this;
    }

    public OverlappingTokenTextSplitter build() {
      return new OverlappingTokenTextSplitter(
          this.encodingType, this.chunkSize, this.chunkOverlap,
          this.minChunkLengthToEmbed, this.maxNumChunks);
    }
  }
}
