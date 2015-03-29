package com.thoughtworks.go.plugin.access.artifactcleanup;

public class ArtifactExtensionStageConfiguration {

    private String pipelineName;

    private String stageName;

    public ArtifactExtensionStageConfiguration(String pipelineName, String stageName) {
        this.pipelineName = pipelineName;
        this.stageName = stageName;
    }

    public String getPipelineName() {
        return pipelineName;
    }

    public String getStageName() {
        return stageName;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ArtifactExtensionStageConfiguration that = (ArtifactExtensionStageConfiguration) o;

        if (pipelineName != null ? !pipelineName.equals(that.pipelineName) : that.pipelineName != null) return false;
        if (stageName != null ? !stageName.equals(that.stageName) : that.stageName != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = pipelineName != null ? pipelineName.hashCode() : 0;
        result = 31 * result + (stageName != null ? stageName.hashCode() : 0);
        return result;
    }
}
