package com.thoughtworks.go.plugin.access.configrepo.contract;

public class CRMingle {
    private final String baseUrl;
    private final String projectId;
    private final String mql;

    public CRMingle(String baseUrl, String projectId, String mql) {
        this.baseUrl = baseUrl;
        this.projectId = projectId;
        this.mql = mql;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public String getProjectId() {
        return projectId;
    }

    public String getMql() {
        return mql;
    }
}
