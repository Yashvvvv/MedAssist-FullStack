package com.medassist.ai;

import com.medassist.common.config.AppProperties;
import com.medassist.common.error.ApiException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * Google Gemini over plain REST (no SDK: one endpoint, fewer dependencies, easy to read).
 * JSON mode ({@code responseMimeType: application/json}) makes the model return parseable
 * JSON instead of prose. The key travels in a header, never in the URL, so it cannot
 * leak into access logs.
 */
@Slf4j
@Component
public class GeminiClient implements AiClient {

    private final RestClient client;
    private final AppProperties.Ai props;

    public GeminiClient(AppProperties app, RestClient.Builder builder) {
        this.props = app.ai();
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5_000);
        factory.setReadTimeout((int) props.timeout().toMillis());
        this.client = builder.baseUrl(props.baseUrl()).requestFactory(factory).build();
    }

    @Override
    public boolean isConfigured() {
        return props.enabled();
    }

    @Override
    public String generateJson(String prompt, List<Image> images) {
        if (!isConfigured()) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "AI_NOT_CONFIGURED",
                    "AI features are not configured on this server (GEMINI_API_KEY missing)");
        }
        List<Map<String, Object>> parts = new ArrayList<>();
        parts.add(Map.of("text", prompt));
        for (Image img : images) {
            parts.add(Map.of("inline_data", Map.of("mime_type", img.mimeType(),
                    "data", Base64.getEncoder().encodeToString(img.bytes()))));
        }
        Map<String, Object> body = Map.of(
                "contents", List.of(Map.of("role", "user", "parts", parts)),
                "generationConfig", Map.of("responseMimeType", "application/json", "temperature", 0.2));
        try {
            JsonNode res = client.post()
                    .uri("/models/{model}:generateContent", props.model())
                    .header("x-goog-api-key", props.geminiApiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);
            JsonNode text = res == null ? null : res.at("/candidates/0/content/parts/0/text");
            if (text == null || text.isMissingNode() || text.asString().isBlank()) {
                String reason = res == null ? "empty" : res.at("/promptFeedback/blockReason").asString("no content");
                throw new ApiException(HttpStatus.BAD_GATEWAY, "AI_NO_ANSWER", "The AI returned no answer (" + reason + ")");
            }
            return text.asString();
        } catch (RestClientResponseException e) {
            log.warn("Gemini HTTP {}: {}", e.getStatusCode().value(), e.getResponseBodyAsString());
            HttpStatus status = e.getStatusCode().value() == 429 ? HttpStatus.TOO_MANY_REQUESTS : HttpStatus.BAD_GATEWAY;
            throw new ApiException(status, "AI_UPSTREAM_ERROR", "The AI service is unavailable right now, try again");
        } catch (RestClientException e) {
            log.warn("Gemini call failed: {}", e.getMessage());
            throw new ApiException(HttpStatus.BAD_GATEWAY, "AI_UPSTREAM_ERROR", "The AI service did not respond in time");
        }
    }
}
