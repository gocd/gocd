package com.thoughtworks.go.plugin.access.configrepo.contract;

import com.thoughtworks.go.plugin.access.configrepo.ErrorCollection;
import com.thoughtworks.go.util.StringUtil;

public class CRMingle extends CRBase {
    private String base_url;
    private String project_identifier;
    private String mql_grouping_conditions;

    public CRMingle(){}

    public CRMingle(String baseUrl,String projectId){
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

        CRMingle that = (CRMingle) o;

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

    @Override
    public void getErrors(ErrorCollection errors, String parentLocation) {
        String location = getLocation(parentLocation);
        errors.checkMissing(location,"project_identifier",project_identifier);
        errors.checkMissing(location,"base_url",base_url);
    }

    @Override
    public String getLocation(String parent) {
        String myLocation = getLocation() == null ? parent : getLocation();
        return String.format("%s; Mingle",myLocation);
    }
}
