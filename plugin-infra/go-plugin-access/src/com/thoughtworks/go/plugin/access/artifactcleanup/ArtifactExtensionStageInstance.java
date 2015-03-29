package com.thoughtworks.go.plugin.access.artifactcleanup;

import java.util.ArrayList;
import java.util.List;

public class ArtifactExtensionStageInstance {

    private long id;

    private String pipelineName;

    private int pipelineCounter;

    private String stageName;

    private String stageCounter;

    private List<String> includePaths = new ArrayList<String>();

    private List<String> excludePaths = new ArrayList<String>();

    public ArtifactExtensionStageInstance(long id, String pipelineName, int pipelineCounter, String stageName, String stageCounter) {
        this.id = id;
        this.pipelineName = pipelineName;
        this.pipelineCounter = pipelineCounter;
        this.stageName = stageName;
        this.stageCounter = stageCounter;
    }

    public long getId() {
        return id;
    }

    public String getPipelineName() {
        return pipelineName;
    }

    public int getPipelineCounter() {
        return pipelineCounter;
    }

    public String getStageName() {
        return stageName;
    }

    public String getStageCounter() {
        return stageCounter;
    }

    public List<String> getIncludePaths() {
        return includePaths;
    }

    public List<String> getExcludePaths() {
        return excludePaths;
    }

}
