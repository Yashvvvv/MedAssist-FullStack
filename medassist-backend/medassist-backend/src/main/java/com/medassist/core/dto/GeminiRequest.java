package com.medassist.core.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class GeminiRequest {

    @JsonProperty("contents")
    private List<Content> contents;

    @JsonProperty("generationConfig")
    private GenerationConfig generationConfig;

    @JsonProperty("safetySettings")
    private List<SafetySetting> safetySettings;

    public GeminiRequest() {}

    public GeminiRequest(List<Content> contents) {
        this.contents = contents;
        this.generationConfig = new GenerationConfig();
        this.safetySettings = createDefaultSafetySettings();
    }

    public List<Content> getContents() {
        return contents;
    }

    public void setContents(List<Content> contents) {
        this.contents = contents;
    }

    public GenerationConfig getGenerationConfig() {
        return generationConfig;
    }

    public void setGenerationConfig(GenerationConfig generationConfig) {
        this.generationConfig = generationConfig;
    }

    public List<SafetySetting> getSafetySettings() {
        return safetySettings;
    }

    public void setSafetySettings(List<SafetySetting> safetySettings) {
        this.safetySettings = safetySettings;
    }

    private List<SafetySetting> createDefaultSafetySettings() {
        return List.of(
            new SafetySetting("HARM_CATEGORY_HATE_SPEECH", "BLOCK_MEDIUM_AND_ABOVE"),
            new SafetySetting("HARM_CATEGORY_DANGEROUS_CONTENT", "BLOCK_MEDIUM_AND_ABOVE"),
            new SafetySetting("HARM_CATEGORY_SEXUALLY_EXPLICIT", "BLOCK_MEDIUM_AND_ABOVE"),
            new SafetySetting("HARM_CATEGORY_HARASSMENT", "BLOCK_MEDIUM_AND_ABOVE")
        );
    }

    public static class Content {
        @JsonProperty("parts")
        private List<Part> parts;

        public Content() {}

        public Content(List<Part> parts) {
            this.parts = parts;
        }

        public List<Part> getParts() {
            return parts;
        }

        public void setParts(List<Part> parts) {
            this.parts = parts;
        }
    }

    public static class Part {
        @JsonProperty("text")
        private String text;

        @JsonProperty("inline_data")
        private InlineData inlineData;

        public Part() {}

        public Part(String text) {
            this.text = text;
        }

        public Part(InlineData inlineData) {
            this.inlineData = inlineData;
        }

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }

        public InlineData getInlineData() {
            return inlineData;
        }

        public void setInlineData(InlineData inlineData) {
            this.inlineData = inlineData;
        }
    }

    public static class InlineData {
        @JsonProperty("mime_type")
        private String mimeType;

        @JsonProperty("data")
        private String data;

        public InlineData() {}

        public InlineData(String mimeType, String data) {
            this.mimeType = mimeType;
            this.data = data;
        }

        public String getMimeType() {
            return mimeType;
        }

        public void setMimeType(String mimeType) {
            this.mimeType = mimeType;
        }

        public String getData() {
            return data;
        }

        public void setData(String data) {
            this.data = data;
        }
    }

    public static class GenerationConfig {
        @JsonProperty("temperature")
        private double temperature = 0.1;

        @JsonProperty("topK")
        private int topK = 32;

        @JsonProperty("topP")
        private double topP = 1.0;

        @JsonProperty("maxOutputTokens")
        private int maxOutputTokens = 4096;

        @JsonProperty("stopSequences")
        private List<String> stopSequences = List.of();

        public double getTemperature() {
            return temperature;
        }

        public void setTemperature(double temperature) {
            this.temperature = temperature;
        }

        public int getTopK() {
            return topK;
        }

        public void setTopK(int topK) {
            this.topK = topK;
        }

        public double getTopP() {
            return topP;
        }

        public void setTopP(double topP) {
            this.topP = topP;
        }

        public int getMaxOutputTokens() {
            return maxOutputTokens;
        }

        public void setMaxOutputTokens(int maxOutputTokens) {
            this.maxOutputTokens = maxOutputTokens;
        }

        public List<String> getStopSequences() {
            return stopSequences;
        }

        public void setStopSequences(List<String> stopSequences) {
            this.stopSequences = stopSequences;
        }
    }

    public static class SafetySetting {
        @JsonProperty("category")
        private String category;

        @JsonProperty("threshold")
        private String threshold;

        public SafetySetting() {}

        public SafetySetting(String category, String threshold) {
            this.category = category;
            this.threshold = threshold;
        }

        public String getCategory() {
            return category;
        }

        public void setCategory(String category) {
            this.category = category;
        }

        public String getThreshold() {
            return threshold;
        }

        public void setThreshold(String threshold) {
            this.threshold = threshold;
        }
    }
}
