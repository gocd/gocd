package com.thoughtworks.go.plugin.access.artifactcleanup;

import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.unmodifiableList;

public class StageDetailsArtifactCleanup {

    private long id;

    private String stageName;

    private String pipelineName;

    private int stageCounter;

    private int pipelineCounter;

    private List<String> artifactsPathsToBeDeleted;

    private List<String> artifactsPathsToBeRetained;


    public StageDetailsArtifactCleanup(long id, String pipelineName, int pipelineCounter, String stageName, int stageCounter) {
        this.id = id;
        this.pipelineCounter = pipelineCounter;
        this.stageName = stageName;
        this.pipelineName = pipelineName;
        this.stageCounter = stageCounter;
        artifactsPathsToBeDeleted = new ArrayList<String>();
        artifactsPathsToBeRetained = new ArrayList<String>();
    }

    public StageDetailsArtifactCleanup(long id, String pipelineName, int pipelineCounter, String stageName, int stageCounter, List<String> artifactPath, boolean exclude) {
        this(id,pipelineName,pipelineCounter,stageName,stageCounter);
        if (exclude) {
            artifactsPathsToBeRetained = artifactPath;
        } else {
            artifactsPathsToBeDeleted = artifactPath;
        }
    }

    public long getId() {
        return id;
    }

    public String getStageName() {
        return stageName;
    }

    public String getPipelineName() {
        return pipelineName;
    }

    public List<String> getArtifactsPathsToBeDeleted() {
        return unmodifiableList(artifactsPathsToBeDeleted);
    }

    public List<String> getArtifactsPathsToBeRetained() {
        return unmodifiableList(artifactsPathsToBeRetained);
    }

    public int getStageCounter() {
        return stageCounter;
    }

    public int getPipelineCounter() {
        return pipelineCounter;
    }
}
