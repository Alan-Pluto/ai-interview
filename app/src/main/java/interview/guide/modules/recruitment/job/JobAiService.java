package interview.guide.modules.recruitment.job;

import interview.guide.common.ai.LlmProviderRegistry;
import interview.guide.common.ai.PromptSanitizer;
import interview.guide.common.ai.PromptSecurityConstants;
import interview.guide.common.ai.StructuredOutputInvoker;
import interview.guide.common.exception.ErrorCode;
import interview.guide.modules.recruitment.model.JobAiOutputs.ComplianceReviewOutput;
import interview.guide.modules.recruitment.model.JobAiOutputs.JobDraftOutput;
import interview.guide.modules.recruitment.model.JobAiOutputs.JobProfileOutput;
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
public class JobAiService {

  private static final String EMPTY_VALUE = "未填写";

  private final LlmProviderRegistry llmProviderRegistry;
  private final StructuredOutputInvoker structuredOutputInvoker;
  private final PromptSanitizer promptSanitizer;
  private final BeanOutputConverter<JobDraftOutput> jobDraftConverter;
  private final BeanOutputConverter<ComplianceReviewOutput> complianceConverter;
  private final BeanOutputConverter<JobProfileOutput> profileConverter;
  private final PromptTemplate jobChatSystemTemplate;
  private final PromptTemplate jobChatUserTemplate;
  private final PromptTemplate complianceSystemTemplate;
  private final PromptTemplate complianceUserTemplate;
  private final PromptTemplate profileSystemTemplate;
  private final PromptTemplate profileUserTemplate;

  public JobAiService(
      LlmProviderRegistry llmProviderRegistry,
      StructuredOutputInvoker structuredOutputInvoker,
      PromptSanitizer promptSanitizer,
      ResourceLoader resourceLoader
  ) throws IOException {
    this.llmProviderRegistry = llmProviderRegistry;
    this.structuredOutputInvoker = structuredOutputInvoker;
    this.promptSanitizer = promptSanitizer;
    this.jobDraftConverter = new BeanOutputConverter<>(JobDraftOutput.class) {};
    this.complianceConverter = new BeanOutputConverter<>(ComplianceReviewOutput.class) {};
    this.profileConverter = new BeanOutputConverter<>(JobProfileOutput.class) {};
    this.jobChatSystemTemplate = new PromptTemplate(loadPrompt(
        resourceLoader, "classpath:prompts/job-post-chat-system.st"));
    this.jobChatUserTemplate = new PromptTemplate(loadPrompt(
        resourceLoader, "classpath:prompts/job-post-chat-user.st"));
    this.complianceSystemTemplate = new PromptTemplate(loadPrompt(
        resourceLoader, "classpath:prompts/jd-compliance-review-system.st"));
    this.complianceUserTemplate = new PromptTemplate(loadPrompt(
        resourceLoader, "classpath:prompts/jd-compliance-review-user.st"));
    this.profileSystemTemplate = new PromptTemplate(loadPrompt(
        resourceLoader, "classpath:prompts/job-profile-extract-system.st"));
    this.profileUserTemplate = new PromptTemplate(loadPrompt(
        resourceLoader, "classpath:prompts/job-profile-extract-user.st"));
  }

  public JobDraftOutput generateJobDraft(JobDraftContext context, String provider) {
    var chatClient = llmProviderRegistry.getPlainChatClient(provider);
    String systemPrompt = jobChatSystemTemplate.render(Map.of(
        "format", jobDraftConverter.getFormat()
    ));
    String userPrompt = jobChatUserTemplate.render(Map.of(
        "title", safeText(context.title()),
        "department", safeText(context.department()),
        "location", safeText(context.location()),
        "employmentType", safeText(context.employmentType()),
        "seniority", safeText(context.seniority()),
        "currentJd", wrap("current-jd", context.currentJd()),
        "conversationHistory", wrap("conversation-history", context.conversationHistory()),
        "message", wrap("message", context.message())
    ));
    return structuredOutputInvoker.invoke(
        chatClient,
        systemPrompt,
        PromptSecurityConstants.DATA_BOUNDARY_INSTRUCTION + "\n" + userPrompt,
        jobDraftConverter,
        ErrorCode.AI_SERVICE_ERROR,
        "JD 生成失败：",
        "JD 生成",
        log
    );
  }

  public ComplianceReviewOutput reviewCompliance(String jdContent, String provider) {
    var chatClient = llmProviderRegistry.getPlainChatClient(provider);
    String systemPrompt = complianceSystemTemplate.render(Map.of(
        "format", complianceConverter.getFormat()
    ));
    String userPrompt = complianceUserTemplate.render(Map.of(
        "jdContent", wrap("jd", jdContent)
    ));
    return structuredOutputInvoker.invoke(
        chatClient,
        systemPrompt,
        PromptSecurityConstants.DATA_BOUNDARY_INSTRUCTION + "\n" + userPrompt,
        complianceConverter,
        ErrorCode.AI_SERVICE_ERROR,
        "JD 合规审查失败：",
        "JD 合规审查",
        log
    );
  }

  public JobProfileOutput extractProfile(String jdContent, String provider) {
    var chatClient = llmProviderRegistry.getPlainChatClient(provider);
    String systemPrompt = profileSystemTemplate.render(Map.of(
        "format", profileConverter.getFormat()
    ));
    String userPrompt = profileUserTemplate.render(Map.of(
        "jdContent", wrap("jd", jdContent)
    ));
    return structuredOutputInvoker.invoke(
        chatClient,
        systemPrompt,
        PromptSecurityConstants.DATA_BOUNDARY_INSTRUCTION + "\n" + userPrompt,
        profileConverter,
        ErrorCode.AI_SERVICE_ERROR,
        "岗位画像抽取失败：",
        "岗位画像抽取",
        log
    );
  }

  private String safeText(String value) {
    return value == null || value.isBlank() ? EMPTY_VALUE : value;
  }

  private String wrap(String label, String text) {
    String value = text == null || text.isBlank() ? EMPTY_VALUE : text;
    return promptSanitizer.wrapWithDelimiters(label, promptSanitizer.sanitize(value));
  }

  private String loadPrompt(ResourceLoader resourceLoader, String path) throws IOException {
    return resourceLoader.getResource(path).getContentAsString(StandardCharsets.UTF_8);
  }

  public record JobDraftContext(
      String title,
      String department,
      String location,
      String employmentType,
      String seniority,
      String currentJd,
      String conversationHistory,
      String message
  ) {}
}
