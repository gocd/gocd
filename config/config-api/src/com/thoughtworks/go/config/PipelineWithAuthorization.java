package com.thoughtworks.go.config;

public class PipelineWithAuthorization {
    private final CaseInsensitiveString pipelineName;
    private final boolean canUserEditPipeline;

    public PipelineWithAuthorization(CaseInsensitiveString pipelineName, boolean canUserEditPipeline) {
        this.pipelineName = pipelineName;
        this.canUserEditPipeline = canUserEditPipeline;
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

        PipelineWithAuthorization that = (PipelineWithAuthorization) o;

        if (canUserEditPipeline != that.canUserEditPipeline) return false;
        return pipelineName.equals(that.pipelineName);
    }

    @Override
    public int hashCode() {
        int result = pipelineName.hashCode();
        result = 31 * result + (canUserEditPipeline ? 1 : 0);
        return result;
    }
}
