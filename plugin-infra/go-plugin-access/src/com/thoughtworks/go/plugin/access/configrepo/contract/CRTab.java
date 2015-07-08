package com.thoughtworks.go.plugin.access.configrepo.contract;

public class CRTab {
    private final String name;
    private final String path;

    public CRTab(String name, String path) {
        this.name = name;
        this.path = path;
    }

    public String getName() {
        return name;
    }

    public String getPath() {
        return path;
    }
}
