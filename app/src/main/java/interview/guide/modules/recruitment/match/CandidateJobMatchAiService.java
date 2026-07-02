package interview.guide.modules.recruitment.match;

import interview.guide.common.ai.LlmProviderRegistry;
import interview.guide.common.ai.PromptSanitizer;
import interview.guide.common.ai.PromptSecurityConstants;
import interview.guide.common.ai.StructuredOutputInvoker;
import interview.guide.common.exception.ErrorCode;
import interview.guide.modules.recruitment.model.CandidateMatchAiOutput;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class CandidateJobMatchAiService {

  private final LlmProviderRegistry llmProviderRegistry;
  private final StructuredOutputInvoker structuredOutputInvoker;
  private final PromptSanitizer promptSanitizer;
  private final BeanOutputConverter<CandidateMatchAiOutput> outputConverter;
  private final PromptTemplate systemPromptTemplate;
  private final PromptTemplate userPromptTemplate;

  public CandidateJobMatchAiService(
      LlmProviderRegistry llmProviderRegistry,
      StructuredOutputInvoker structuredOutputInvoker,
      PromptSanitizer promptSanitizer,
      ResourceLoader resourceLoader
  ) throws IOException {
    this.llmProviderRegistry = llmProviderRegistry;
    this.structuredOutputInvoker = structuredOutputInvoker;
    this.promptSanitizer = promptSanitizer;
    this.outputConverter = new BeanOutputConverter<>(CandidateMatchAiOutput.class) {};
    this.systemPromptTemplate = new PromptTemplate(loadPrompt(
        resourceLoader, "classpath:prompts/candidate-job-match-system.st"));
    this.userPromptTemplate = new PromptTemplate(loadPrompt(
        resourceLoader, "classpath:prompts/candidate-job-match-user.st"));
  }

  public CandidateMatchAiOutput match(MatchContext context, String provider) {
    var chatClient = llmProviderRegistry.getPlainChatClient(provider);
    String systemPrompt = systemPromptTemplate.render(Map.of(
        "format", outputConverter.getFormat()
    ));
    String userPrompt = userPromptTemplate.render(Map.of(
        "jobTitle", safe(context.jobTitle()),
        "jdContent", wrap("jd", context.jdContent()),
        "jobProfile", wrap("job-profile", context.jobProfile()),
        "resumeText", wrap("resume", context.resumeText())
    ));
    return structuredOutputInvoker.invoke(
        chatClient,
        systemPrompt,
        PromptSecurityConstants.DATA_BOUNDARY_INSTRUCTION + "\n" + userPrompt,
        outputConverter,
        ErrorCode.AI_SERVICE_ERROR,
        "简历匹配分析失败：",
        "简历 JD 匹配",
        log
    );
  }

  private String safe(String value) {
    return value == null || value.isBlank() ? "未填写" : value;
  }

  private String wrap(String label, String value) {
    String text = value == null || value.isBlank() ? "未填写" : value;
    return promptSanitizer.wrapWithDelimiters(label, promptSanitizer.sanitize(text));
  }

  private String loadPrompt(ResourceLoader resourceLoader, String path) throws IOException {
    return resourceLoader.getResource(path).getContentAsString(StandardCharsets.UTF_8);
  }

  public record MatchContext(
      String jobTitle,
      String jdContent,
      String jobProfile,
      String resumeText
  ) {}
}
