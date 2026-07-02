package interview.guide.modules.llmprovider.dto;

public record TtsConfigRequest(
    String url,
    String model,
    String apiKey,
    String voice,
    String format,
    Integer sampleRate,
    String mode,
    String languageType,
    Float speechRate,
    Integer volume
) {}
