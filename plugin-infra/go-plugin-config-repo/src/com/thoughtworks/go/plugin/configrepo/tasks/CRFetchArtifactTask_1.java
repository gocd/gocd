package com.thoughtworks.go.plugin.configrepo.tasks;

import com.thoughtworks.go.plugin.configrepo.ErrorCollection;
import org.apache.commons.lang.StringUtils;

public class CRFetchArtifactTask_1 extends CRTask_1 {

    public static final String TYPE_NAME = "fetchartifact";

    private String pipelineName;
    private String stage;
    private String job;
    private String source;
    private boolean sourceIsDir = true;
    private String destination;

    public CRFetchArtifactTask_1(){
        super(TYPE_NAME);
    }

    public CRFetchArtifactTask_1(String stage,String job,String source){
        super(TYPE_NAME);
        this.stage = stage;
        this.job = job;
        this.source = source;
    }

    @Override
    public void getErrors(ErrorCollection errors) {
        validateSource(errors);
        validateStage(errors);
        validateJob(errors);
    }

    private void validateJob(ErrorCollection errors) {
        if (StringUtils.isBlank(job)) {
            errors.add(this, "Job to fetch from is not specified in fetch artifact task");
        }
    }

    private void validateStage(ErrorCollection errors) {
        if (StringUtils.isBlank(stage)) {
            errors.add(this, "Stage to fetch from is not specified in fetch artifact task");
        }
    }

    private void validateSource(ErrorCollection errors) {
        if (StringUtils.isEmpty(source)) {
            errors.add(this, "Source path is not specified in fetch artifact task");
        }
    }

    public String getPipelineName() {
        return pipelineName;
    }

    public void setPipelineName(String pipelineName) {
        this.pipelineName = pipelineName;
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
        return sourceIsDir;
    }

    public void setSourceIsDirectory(boolean srcIsDirectory) {
        this.sourceIsDir = srcIsDirectory;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        //TODO: compare abstract tasks for correct implementation -jj

        CRFetchArtifactTask_1 fetchTask = (CRFetchArtifactTask_1) o;

        if(!super.equals(fetchTask))
            return false;

        if (destination != null ? !destination.equals(fetchTask.destination) : fetchTask.destination != null) {
            return false;
        }
        if (job != null ? !job.equals(fetchTask.job) : fetchTask.job != null) {
            return false;
        }
        if (pipelineName != null ? !pipelineName.equals(fetchTask.pipelineName) : fetchTask.pipelineName != null) {
            return false;
        }
        if (source != null ? !source.equals(fetchTask.source) : fetchTask.source != null) {
            return false;
        }
        if (sourceIsDir != fetchTask.sourceIsDir) {
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
        result = 31 * result + (pipelineName != null ? pipelineName.hashCode() : 0);
        result = 31 * result + (stage != null ? stage.hashCode() : 0);
        result = 31 * result + (job != null ? job.hashCode() : 0);
        result = 31 * result + (source != null ? source.hashCode() : 0);
        result = 31 * result + (destination != null ? destination.hashCode() : 0);
        return result;
    }
}
