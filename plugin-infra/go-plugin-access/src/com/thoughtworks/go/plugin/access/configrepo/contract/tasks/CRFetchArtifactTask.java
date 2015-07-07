package com.thoughtworks.go.plugin.access.configrepo.contract.tasks;

public class CRFetchArtifactTask extends CRTask  {
    private final String pipelineName;
    private final String stage;
    private final String job;
    private final String source;
    private final boolean sourceIsDir;
    private final String destination;

    public CRFetchArtifactTask(CRRunIf runIf, CRTask onCancel,
                               String pipelineName,String stage,String job,
                               String source, String destination,boolean sourceIsDir) {
        super(runIf, onCancel);
        this.pipelineName = pipelineName;
        this.stage = stage;
        this.job = job;
        this.source = source;
        this.sourceIsDir = sourceIsDir;
        this.destination = destination;
    }

    public String getPipelineName() {
        return pipelineName;
    }

    public String getStage() {
        return stage;
    }

    public String getJob() {
        return job;
    }

    public String getSource() {
        return source;
    }

    public boolean sourceIsDirectory() {
        return sourceIsDir;
    }

    public String getDestination() {
        return destination;
    }
}
