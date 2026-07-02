package interview.guide.modules.recruitment.guide;

import interview.guide.common.ai.LlmProviderRegistry;
import interview.guide.common.ai.PromptSanitizer;
import interview.guide.common.ai.PromptSecurityConstants;
import interview.guide.common.ai.StructuredOutputInvoker;
import interview.guide.common.exception.ErrorCode;
import interview.guide.modules.recruitment.model.InterviewGuideAiOutput;
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
public class InterviewGuideAiService {

  private final LlmProviderRegistry llmProviderRegistry;
  private final StructuredOutputInvoker structuredOutputInvoker;
  private final PromptSanitizer promptSanitizer;
  private final BeanOutputConverter<InterviewGuideAiOutput> outputConverter;
  private final PromptTemplate systemPromptTemplate;
  private final PromptTemplate userPromptTemplate;

  public InterviewGuideAiService(
      LlmProviderRegistry llmProviderRegistry,
      StructuredOutputInvoker structuredOutputInvoker,
      PromptSanitizer promptSanitizer,
      ResourceLoader resourceLoader
  ) throws IOException {
    this.llmProviderRegistry = llmProviderRegistry;
    this.structuredOutputInvoker = structuredOutputInvoker;
    this.promptSanitizer = promptSanitizer;
    this.outputConverter = new BeanOutputConverter<>(InterviewGuideAiOutput.class) {};
    this.systemPromptTemplate = new PromptTemplate(loadPrompt(
        resourceLoader, "classpath:prompts/interview-guide-generate-system.st"));
    this.userPromptTemplate = new PromptTemplate(loadPrompt(
        resourceLoader, "classpath:prompts/interview-guide-generate-user.st"));
  }

  public InterviewGuideAiOutput generate(GuideContext context, String provider) {
    var chatClient = llmProviderRegistry.getPlainChatClient(provider);
    String systemPrompt = systemPromptTemplate.render(Map.of(
        "format", outputConverter.getFormat()
    ));
    String userPrompt = userPromptTemplate.render(Map.of(
        "mode", safe(context.mode()),
        "interviewRound", safe(context.interviewRound()),
        "durationMinutes", String.valueOf(context.durationMinutes()),
        "jobTitle", safe(context.jobTitle()),
        "jdContent", wrap("jd", context.jdContent()),
        "jobProfile", wrap("job-profile", context.jobProfile()),
        "resumeText", wrap("resume", context.resumeText()),
        "matchReport", wrap("match-report", context.matchReport())
    ));
    return structuredOutputInvoker.invoke(
        chatClient,
        systemPrompt,
        PromptSecurityConstants.DATA_BOUNDARY_INSTRUCTION + "\n" + userPrompt,
        outputConverter,
        ErrorCode.AI_SERVICE_ERROR,
        "面试题纲生成失败：",
        "面试题纲生成",
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

  public record GuideContext(
      String mode,
      String interviewRound,
      Integer durationMinutes,
      String jobTitle,
      String jdContent,
      String jobProfile,
      String resumeText,
      String matchReport
  ) {}
}
