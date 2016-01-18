package com.thoughtworks.go.plugin.configrepo;

import com.thoughtworks.go.util.StringUtil;

public class CRMingle_1 extends CRBase {
    private String base_url;
    private String project_identifier;
    private String mql_grouping_conditions;

    public CRMingle_1(){}

    public CRMingle_1(String baseUrl,String projectId){
        this.base_url = baseUrl;
        this.project_identifier = projectId;
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

        if (base_url != null ? !base_url.equals(that.base_url) : that.base_url != null) {
            return false;
        }
        if (mql_grouping_conditions != null ? !mql_grouping_conditions.equals(that.mql_grouping_conditions) : that.mql_grouping_conditions != null) {
            return false;
        }
        if (project_identifier != null ? !project_identifier.equals(that.project_identifier) : that.project_identifier != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = base_url != null ? base_url.hashCode() : 0;
        result = 31 * result + (project_identifier != null ? project_identifier.hashCode() : 0);
        result = 31 * result + (mql_grouping_conditions != null ? mql_grouping_conditions.hashCode() : 0);
        return result;
    }

    @Override
    public void getErrors(ErrorCollection errors) {
        validateBaseUrl(errors);
        validateProjectId(errors);
    }

    private void validateProjectId(ErrorCollection errors) {
        if (StringUtil.isBlank(project_identifier)) {
            errors.add(this, "Mingle has no project id set");
        }
    }

    private void validateBaseUrl(ErrorCollection errors) {
        if (StringUtil.isBlank(base_url)) {
            errors.add(this, "Mingle has no base url set");
        }
    }

    public String getBaseUrl() {
        return base_url;
    }

    public void setBaseUrl(String baseUrl) {
        this.base_url = baseUrl;
    }

    public String getProjectIdentifier() {
        return project_identifier;
    }

    public void setProjectIdentifier(String project_identifier) {
        this.project_identifier = project_identifier;
    }

    public String getMqlGroupingConditions() {
        return mql_grouping_conditions;
    }

    public void setMqlGroupingConditions(String mql_grouping_conditions) {
        this.mql_grouping_conditions = mql_grouping_conditions;
    }
}
