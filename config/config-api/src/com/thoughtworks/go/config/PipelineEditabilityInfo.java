package com.thoughtworks.go.config;

public class PipelineEditabilityInfo {
    private final CaseInsensitiveString pipelineName;
    private final boolean canUserEditPipeline;
    private boolean isOriginLocal;

    public PipelineEditabilityInfo(CaseInsensitiveString pipelineName, boolean canUserEditPipeline, boolean isOriginLocal) {
        this.pipelineName = pipelineName;
        this.canUserEditPipeline = canUserEditPipeline;
        this.isOriginLocal = isOriginLocal;
    }

    public CaseInsensitiveString getPipelineName() {
        return pipelineName;
    }

    public boolean canUserEditPipeline() {
        return canUserEditPipeline;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PipelineEditabilityInfo that = (PipelineEditabilityInfo) o;

        if (canUserEditPipeline != that.canUserEditPipeline) return false;
        if (isOriginLocal != that.isOriginLocal) return false;
        return pipelineName != null ? pipelineName.equals(that.pipelineName) : that.pipelineName == null;
    }

    @Override
    public int hashCode() {
        int result = pipelineName != null ? pipelineName.hashCode() : 0;
        result = 31 * result + (canUserEditPipeline ? 1 : 0);
        result = 31 * result + (isOriginLocal ? 1 : 0);
        return result;
    }

    public boolean isOriginLocal() {
        return isOriginLocal;
    }
}
