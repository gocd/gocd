package com.thoughtworks.go.plugin.api;

public class GoPluginIdentifier {

    private String id;

    private String extension;

    public GoPluginIdentifier(String id, String extension) {
        this.id = id;
        this.extension = extension;
    }

    public String getId() {
        return id;
    }

    public String getExtension() {
        return extension;
    }
}
