package com.thoughtworks.go.plugin.access.configrepo.contract;

public class CRPropertyGenerator {
    private final String name;
    private final String src;
    private final String xpath;

    public CRPropertyGenerator(String name, String src, String xpath) {
        this.name = name;
        this.src = src;
        this.xpath = xpath;
    }

    public String getName() {
        return name;
    }

    public String getSrc() {
        return src;
    }

    public String getXpath() {
        return xpath;
    }
}
