package com.thoughtworks.go.plugin.configrepo;

import com.thoughtworks.go.util.StringUtil;

public class CRMingle_1 extends CRBase {
    private String baseUrl;
    private String projectId;
    private String mql;

    public CRMingle_1(){}

    public CRMingle_1(String baseUrl,String projectId){
        this.baseUrl = baseUrl;
        this.projectId = projectId;
    }
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        CRMingle_1 that = (CRMingle_1) o;

        if (baseUrl != null ? !baseUrl.equals(that.baseUrl) : that.baseUrl != null) {
            return false;
        }
        if (mql != null ? !mql.equals(that.mql) : that.mql != null) {
            return false;
        }
        if (projectId != null ? !projectId.equals(that.projectId) : that.projectId != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = baseUrl != null ? baseUrl.hashCode() : 0;
        result = 31 * result + (projectId != null ? projectId.hashCode() : 0);
        result = 31 * result + (mql != null ? mql.hashCode() : 0);
        return result;
    }

    @Override
    public void getErrors(ErrorCollection errors) {
        validateBaseUrl(errors);
        validateProjectId(errors);
    }

    private void validateProjectId(ErrorCollection errors) {
        if (StringUtil.isBlank(projectId)) {
            errors.add(this, "Mingle has no project id set");
        }
    }

    private void validateBaseUrl(ErrorCollection errors) {
        if (StringUtil.isBlank(baseUrl)) {
            errors.add(this, "Mingle has no base url set");
        }
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public String getMql() {
        return mql;
    }

    public void setMql(String mql) {
        this.mql = mql;
    }
}
