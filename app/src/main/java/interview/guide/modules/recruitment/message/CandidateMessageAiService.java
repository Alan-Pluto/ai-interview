package interview.guide.modules.recruitment.message;

import interview.guide.common.ai.LlmProviderRegistry;
import interview.guide.common.ai.PromptSanitizer;
import interview.guide.common.ai.PromptSecurityConstants;
import interview.guide.common.ai.StructuredOutputInvoker;
import interview.guide.common.exception.ErrorCode;
import interview.guide.modules.recruitment.model.CandidateMessageAiOutput;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class CandidateMessageAiService {

  private final LlmProviderRegistry llmProviderRegistry;
  private final StructuredOutputInvoker structuredOutputInvoker;
  private final PromptSanitizer promptSanitizer;
  private final BeanOutputConverter<CandidateMessageAiOutput> outputConverter;
  private final PromptTemplate systemPromptTemplate;
  private final PromptTemplate userPromptTemplate;

  public CandidateMessageAiService(
      LlmProviderRegistry llmProviderRegistry,
      StructuredOutputInvoker structuredOutputInvoker,
      PromptSanitizer promptSanitizer,
      ResourceLoader resourceLoader
  ) throws IOException {
    this.llmProviderRegistry = llmProviderRegistry;
    this.structuredOutputInvoker = structuredOutputInvoker;
    this.promptSanitizer = promptSanitizer;
    this.outputConverter = new BeanOutputConverter<>(CandidateMessageAiOutput.class) {};
    this.systemPromptTemplate = new PromptTemplate(loadPrompt(
        resourceLoader, "classpath:prompts/candidate-message-system.st"));
    this.userPromptTemplate = new PromptTemplate(loadPrompt(
        resourceLoader, "classpath:prompts/candidate-message-user.st"));
  }

  public CandidateMessageAiOutput generate(MessageContext context, String provider) {
    var chatClient = llmProviderRegistry.getPlainChatClient(provider);
    String systemPrompt = systemPromptTemplate.render(Map.of(
        "format", outputConverter.getFormat()
    ));
    Map<String, Object> variables = new HashMap<>();
    variables.put("messageType", safe(context.messageType()));
    variables.put("channel", safe(context.channel()));
    variables.put("stage", safe(context.stage()));
    variables.put("tone", safe(context.tone()));
    variables.put("jobTitle", safe(context.jobTitle()));
    variables.put("extraInstruction", safe(context.extraInstruction()));
    variables.put("jdContent", wrap("jd", context.jdContent()));
    variables.put("resumeText", wrap("resume", context.resumeText()));
    variables.put("matchReport", wrap("match-report", context.matchReport()));
    variables.put("interviewGuide", wrap("interview-guide", context.interviewGuide()));
    variables.put("currentDraft", wrap("current-draft", context.currentDraft()));
    variables.put("conversationHistory", wrap("conversation-history", context.conversationHistory()));
    String userPrompt = userPromptTemplate.render(variables);
    return structuredOutputInvoker.invoke(
        chatClient,
        systemPrompt,
        PromptSecurityConstants.DATA_BOUNDARY_INSTRUCTION + "\n" + userPrompt,
        outputConverter,
        ErrorCode.AI_SERVICE_ERROR,
        "候选人沟通文案生成失败：",
        "候选人沟通文案生成",
        log
    );
  }

  private String safe(String value) {
    return value == null || value.isBlank() ? "未填写" : value;
  }

  private String wrap(String label, String value) {
    String text = value == null || value.isBlank() ? "未提供" : value;
    return promptSanitizer.wrapWithDelimiters(label, promptSanitizer.sanitize(text));
  }

  private String loadPrompt(ResourceLoader resourceLoader, String path) throws IOException {
    return resourceLoader.getResource(path).getContentAsString(StandardCharsets.UTF_8);
  }

  public record MessageContext(
      String messageType,
      String channel,
      String stage,
      String tone,
      String jobTitle,
      String jdContent,
      String resumeText,
      String matchReport,
      String interviewGuide,
      String currentDraft,
      String conversationHistory,
      String extraInstruction
  ) {}
}
