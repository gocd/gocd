package com.thoughtworks.go.plugin.api;

import java.util.List;

public class GoPluginIdentifier {

    private String id;

    private String extension;
    private List<String> supportedVersions;

    public GoPluginIdentifier(String id, String extension, List<String> supportedVersions) {
        this.id = id;
        this.extension = extension;
        this.supportedVersions = supportedVersions;
    }

    public String getId() {
        return id;
    }

    public String getExtension() {
        return extension;
    }

    public List<String> getSupportedVersions() {
        return supportedVersions;
    }
}
