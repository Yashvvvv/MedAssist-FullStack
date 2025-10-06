package com.medassist.core.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class MedicineAnalysisRequest {

    @JsonProperty("query")
    private String query;

    @JsonProperty("image_data")
    private String imageData;

    @JsonProperty("image_mime_type")
    private String imageMimeType;

    @JsonProperty("analysis_type")
    private AnalysisType analysisType;

    @JsonProperty("include_safety_info")
    private boolean includeSafetyInfo = true;

    @JsonProperty("include_interactions")
    private boolean includeInteractions = true;

    public enum AnalysisType {
        TEXT_QUERY,
        IMAGE_ANALYSIS,
        COMBINED
    }

    public MedicineAnalysisRequest() {}

    public MedicineAnalysisRequest(String query, AnalysisType analysisType) {
        this.query = query;
        this.analysisType = analysisType;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public String getImageData() {
        return imageData;
    }

    public void setImageData(String imageData) {
        this.imageData = imageData;
    }

    public String getImageMimeType() {
        return imageMimeType;
    }

    public void setImageMimeType(String imageMimeType) {
        this.imageMimeType = imageMimeType;
    }

    public AnalysisType getAnalysisType() {
        return analysisType;
    }

    public void setAnalysisType(AnalysisType analysisType) {
        this.analysisType = analysisType;
    }

    public boolean isIncludeSafetyInfo() {
        return includeSafetyInfo;
    }

    public void setIncludeSafetyInfo(boolean includeSafetyInfo) {
        this.includeSafetyInfo = includeSafetyInfo;
    }

    public boolean isIncludeInteractions() {
        return includeInteractions;
    }

    public void setIncludeInteractions(boolean includeInteractions) {
        this.includeInteractions = includeInteractions;
    }
}
