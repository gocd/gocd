package com.thoughtworks.go.plugin.configrepo;

import com.thoughtworks.go.util.StringUtil;

public class CRArtifact_1 extends CRBase {
    private String source;
    private String destination;

    public CRArtifact_1(){}
    public CRArtifact_1(String src, String dest) {
        this.source = src;
        this.destination = dest;
    }

    @Override
    public void getErrors(ErrorCollection errors) {
        validateSource(errors);
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
        return !(source != null ? !source.equals(other.source) : other.source != null) ;

    }

    public int hashCode() {
        int result = 0;
        result = 31 * result + (source != null ? source.hashCode() : 0);
        result = 31 * result + (destination != null ? destination.hashCode() : 0);
        return result;
    }
}
