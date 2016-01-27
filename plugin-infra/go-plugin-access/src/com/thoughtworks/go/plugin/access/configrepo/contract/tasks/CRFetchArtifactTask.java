package com.thoughtworks.go.plugin.access.configrepo.contract.tasks;

import com.thoughtworks.go.plugin.access.configrepo.ErrorCollection;

public class CRFetchArtifactTask extends CRTask  {

    public static final String TYPE_NAME = "fetch";

    private String pipeline;
    private String stage;
    private String job;
    private String source;
    // when missing, will be false, which is default - source is directory
    private boolean is_source_a_file;
    private String destination;

    public CRFetchArtifactTask(){
        super(TYPE_NAME);
    }

    public CRFetchArtifactTask(String stage,String job,String source){
        super(TYPE_NAME);
        this.stage = stage;
        this.job = job;
        this.source = source;
    }
    public CRFetchArtifactTask(CRRunIf runIf, CRTask onCancel,
                               String pipelineName,String stage,String job,
                               String source, String destination,boolean sourceIsDir) {
        super(runIf, onCancel);
        this.pipeline = pipelineName;
        this.stage = stage;
        this.job = job;
        this.source = source;
        this.is_source_a_file = !sourceIsDir;
        this.destination = destination;
    }

    public String getPipelineName() {
        return pipeline;
    }

    public void setPipelineName(String pipelineName) {
        this.pipeline = pipelineName;
    }

    public String getStage() {
        return stage;
    }

    public void setStage(String stage) {
        this.stage = stage;
    }

    public String getJob() {
        return job;
    }

    public void setJob(String job) {
        this.job = job;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String dest) {
        this.destination = dest;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String src) {
        this.source = src;
    }

    public boolean sourceIsDirectory() {
        return !is_source_a_file;
    }

    public void setSourceIsDirectory(boolean srcIsDirectory) {
        this.is_source_a_file = !srcIsDirectory;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        CRFetchArtifactTask fetchTask = (CRFetchArtifactTask) o;

        if(!super.equals(fetchTask))
            return false;

        if (destination != null ? !destination.equals(fetchTask.destination) : fetchTask.destination != null) {
            return false;
        }
        if (job != null ? !job.equals(fetchTask.job) : fetchTask.job != null) {
            return false;
        }
        if (pipeline != null ? !pipeline.equals(fetchTask.pipeline) : fetchTask.pipeline != null) {
            return false;
        }
        if (source != null ? !source.equals(fetchTask.source) : fetchTask.source != null) {
            return false;
        }
        if (is_source_a_file != fetchTask.is_source_a_file) {
            return false;
        }
        if (stage != null ? !stage.equals(fetchTask.stage) : fetchTask.stage != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (pipeline != null ? pipeline.hashCode() : 0);
        result = 31 * result + (stage != null ? stage.hashCode() : 0);
        result = 31 * result + (job != null ? job.hashCode() : 0);
        result = 31 * result + (source != null ? source.hashCode() : 0);
        result = 31 * result + (destination != null ? destination.hashCode() : 0);
        return result;
    }

    @Override
    public void getErrors(ErrorCollection errors, String parentLocation) {
        String location = getLocation(parentLocation);
        errors.checkMissing(location,"source",source);
        errors.checkMissing(location,"stage",stage);
        errors.checkMissing(location,"job",job);
    }

    @Override
    public String getLocation(String parent) {
        String myLocation = getLocation() == null ? parent : getLocation();
        String pipe = getPipelineName() != null ? getPipelineName() : "unknown pipeline";
        String stage = getStage() != null ? getStage() : "unknown stage";
        String job = getJob() != null ? getJob() : "unknown job";

        return String.format("%s; fetch artifacts task from %s %s %s",myLocation,pipe,stage,job);
    }
}
