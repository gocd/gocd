package com.thoughtworks.go.presentation.pipelinehistory;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by t-madevl on 7/11/2016.
 */
public class MatchedPipelineRevision {
    private String revision;
    private String fingerprint;
    private String pipelineName;
    private String pipelineLabel;

    public String getRevision() {
        return revision;
    }

    public void setRevision(String revision) {
        this.revision = revision;
    }

    public String getFingerprint() {
        return fingerprint;
    }

    public void setFingerprint(String fingerprint) {
        this.fingerprint = fingerprint;
    }

    public String getPipelineName() {
        return pipelineName;
    }

    public void setPipelineName(String pipelineName) {
        this.pipelineName = pipelineName;
    }

    public String getPipelineLabel() {
        return pipelineLabel;
    }

    public void setPipelineLabel(String pipelineLabel) {
        this.pipelineLabel = pipelineLabel;
    }

    public Map<String, String> toJson() {
        Map<String, String> json = new LinkedHashMap<>();
        json.put("revision", revision);
        json.put("fingerprint", fingerprint);
        json.put("pipelineName", pipelineName);
        json.put("pipelineLabel", pipelineLabel);
        return json;
    }

}
