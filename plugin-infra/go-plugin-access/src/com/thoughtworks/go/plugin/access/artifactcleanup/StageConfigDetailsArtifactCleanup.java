package com.thoughtworks.go.plugin.access.artifactcleanup;

public class StageConfigDetailsArtifactCleanup {

    private String stageName;

    private String pipelineName;

    public StageConfigDetailsArtifactCleanup(String stageName, String pipelineName) {
        this.stageName = stageName;
        this.pipelineName = pipelineName;
    }

    public String getStageName() {
        return stageName;
    }

    public String getPipelineName() {
        return pipelineName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        StageConfigDetailsArtifactCleanup that = (StageConfigDetailsArtifactCleanup) o;

        if (!pipelineName.equals(that.pipelineName)) return false;
        if (!stageName.equals(that.stageName)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = stageName.hashCode();
        result = 31 * result + pipelineName.hashCode();
        return result;
    }
}
