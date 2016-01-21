package com.thoughtworks.go.plugin.access.configrepo.contract;

import com.thoughtworks.go.plugin.access.configrepo.ErrorCollection;
import com.thoughtworks.go.util.StringUtil;

public class CRArtifact extends CRBase {
    private String source;
    private String destination;
    private CRArtifactType type;

    public CRArtifact(){}
    public CRArtifact(String src, String dest,CRArtifactType type) {
        this.source = src;
        this.destination = dest;
        this.type = type;
    }

    public CRArtifact(String src, String dest) {
        this.source = src;
        this.destination = dest;
        this.type = CRArtifactType.build;
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
        return this == other || other != null && other instanceof CRArtifact && equals((CRArtifact) other);
    }

    private boolean equals(CRArtifact other) {
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

    public CRArtifactType getType() {
        return type;
    }

    public void setType(CRArtifactType type) {
        this.type = type;
    }

    @Override
    public void getErrors(ErrorCollection errors, String parentLocation) {
        String location = getLocation(parentLocation);
        errors.checkMissing(location,"type",type);
        errors.checkMissing(location,"source",source);
    }

    @Override
    public String getLocation(String parent) {
        String myLocation = getLocation() == null ? parent : getLocation();
        return String.format("%s; Artifacts",myLocation);
    }
}
