package com.thoughtworks.go.plugin.api;

import java.util.List;

public class GoPluginIdentifier {

    private String id;

    private String extension;

    private List<String> supportedExtensionVersions;

    public GoPluginIdentifier(String id, String extension, List<String> supportedExtensionVersions) {
        this.id = id;
        this.extension = extension;
        this.supportedExtensionVersions = supportedExtensionVersions;
    }

    public String getId() {
        return id;
    }

    public String getExtension() {
        return extension;
    }

    public List<String> getSupportedExtensionVersions() {
        return supportedExtensionVersions;
    }
}
