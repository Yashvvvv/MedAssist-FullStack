package com.medassist.ai;

import java.util.List;

/**
 * The one seam between our code and an LLM provider. Production uses {@link GeminiClient};
 * tests swap in a fake, so no test ever calls a paid API.
 */
public interface AiClient {

    /** Asks for a JSON answer. Returns the raw JSON text the model produced. */
    String generateJson(String prompt, List<Image> images);

    boolean isConfigured();

    record Image(byte[] bytes, String mimeType) {
    }
}
