package com.thoughtworks.go.plugin.access.configrepo.contract;

public class CRArtifact {
    private final String src;
    private final String dest;

    public CRArtifact(String src, String dest) {
        this.src = src;
        this.dest = dest;
    }

    public String getSrc() {
        return src;
    }

    public String getDest() {
        return dest;
    }
}
