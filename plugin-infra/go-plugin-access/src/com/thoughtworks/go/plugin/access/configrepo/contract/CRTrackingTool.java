package com.thoughtworks.go.plugin.access.configrepo.contract;

public class CRTrackingTool {
    private final String link;
    private final String regex;

    public CRTrackingTool(String link, String regex) {
        this.link = link;
        this.regex = regex;
    }

    public String getLink() {
        return link;
    }

    public String getRegex() {
        return regex;
    }
}
