package com.thoughtworks.go.plugin.configrepo;

import com.thoughtworks.go.util.StringUtil;

public class CRArtifact_1 extends CRBase {
    private String source;
    private String destination;
    private String type;

    public CRArtifact_1(){}
    public CRArtifact_1(String src, String dest,String type) {
        this.source = src;
        this.destination = dest;
        this.type = type;
    }

    @Override
    public void getErrors(ErrorCollection errors) {
        validateSource(errors);
        validateType(errors);
    }

    private void validateType(ErrorCollection errors) {
        if (StringUtil.isBlank(type)) {
            errors.add(this, "Artifact type is not set");
        }
        else if(!type.contentEquals("build") && !type.contentEquals("test"))
        {
            errors.add(this, "Artifact type must be 'build' or 'test'");
        }
    }

    private void validateSource(ErrorCollection errors) {
        if (StringUtil.isBlank(source)) {
            errors.add(this, "Artifact source is not set");
        }
    }

    public String getSource() {
        return source;
    }

    public void setSource(String src) {
        this.source = src;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public boolean equals(Object other) {
        return this == other || other != null && other instanceof CRArtifact_1 && equals((CRArtifact_1) other);
    }

    private boolean equals(CRArtifact_1 other) {
        if (destination != null ? !destination.equals(other.destination) : other.destination != null) {
            return false;
        }
        if (type != null ? !type.equals(other.type) : other.type != null) {
            return false;
        }
        return !(source != null ? !source.equals(other.source) : other.source != null) ;

    }

    public int hashCode() {
        int result = 0;
        result = 31 * result + (source != null ? source.hashCode() : 0);
        result = 31 * result + (destination != null ? destination.hashCode() : 0);
        result = 31 * result + (type != null ? type.hashCode() : 0);
        return result;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
