package com.thoughtworks.go.plugin.access.configrepo.contract;

public class CRPluginConfiguration {
    private final String id;
    private final String version;

    public CRPluginConfiguration(String id,String version)
    {
        this.id = id;
        this.version = version;
    }

    public String getId() {
        return id;
    }

    public String getVersion() {
        return version;
    }
}
