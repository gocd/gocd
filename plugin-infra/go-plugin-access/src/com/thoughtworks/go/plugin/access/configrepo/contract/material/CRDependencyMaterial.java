package com.thoughtworks.go.plugin.access.configrepo.contract.material;

import com.thoughtworks.go.plugin.access.configrepo.ErrorCollection;

public class CRDependencyMaterial extends CRMaterial {
    public static final String TYPE_NAME = "dependency";
    private String pipeline;
    private String stage;

    public CRDependencyMaterial() {
        type = TYPE_NAME;
    }

    public CRDependencyMaterial(String name,String pipelineName,String stageName) {
        super(TYPE_NAME,name);
        this.pipeline = pipelineName;
        this.stage = stageName;
    }
    public CRDependencyMaterial(String pipelineName,String stageName) {
        type = TYPE_NAME;
        this.pipeline = pipelineName;
        this.stage = stageName;
    }

    public String getPipelineName() {
        return pipeline;
    }

    public void setPipelineName(String pipelineName) {
        this.pipeline = pipelineName;
    }

    public String getStageName() {
        return stage;
    }

    public void setStageName(String stageName) {
        this.stage = stageName;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CRDependencyMaterial that = (CRDependencyMaterial) o;
        if(!super.equals(that))
            return false;

        if (pipeline != null ? !pipeline.equals(that.pipeline) : that.pipeline != null) {
            return false;
        }
        if (stage != null ? !stage.equals(that.stage) : that.stage != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (pipeline != null ? pipeline.hashCode() : 0);
        result = 31 * result + (stage != null ? stage.hashCode() : 0);
        return result;
    }

    @Override
    public String typeName() {
        return TYPE_NAME;
    }

    @Override
    public void getErrors(ErrorCollection errors, String parentLocation) {
        String location = getLocation(parentLocation);
        errors.checkMissing(location,"pipeline",pipeline);
        errors.checkMissing(location,"stage",stage);
    }

    @Override
    public String getLocation(String parent) {
        String myLocation = getLocation() == null ? parent : getLocation();
        String name = getName() == null ? "" : getName();
        String pipe = getPipelineName() != null ? getPipelineName() : "unknown pipeline";
        String stage = getStageName() != null ? getStageName() : "unknown stage";
        return String.format("%s; Dependency material %s on %s/%s",myLocation,name,pipe,stage);
    }
}
