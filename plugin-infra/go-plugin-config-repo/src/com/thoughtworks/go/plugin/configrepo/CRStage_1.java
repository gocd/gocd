package com.thoughtworks.go.plugin.configrepo;

import com.thoughtworks.go.util.StringUtil;
import org.apache.commons.collections.CollectionUtils;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;

public class CRStage_1 extends CRBase {
    private String name;
    private boolean fetchMaterials = true;
    private boolean artifactCleanupProhibited;
    private boolean cleanWorkingDir;
    private CRApproval_1 approval ;
    private Collection<CREnvironmentVariable_1> environmentVariables = new ArrayList<>();
    private Collection<CRJob_1> jobs = new ArrayList<>();

    public CRStage_1()
    {
    }

    public CRStage_1(String name,CRJob_1... jobs)
    {
        this.name = name;
        this.jobs = Arrays.asList(jobs);
    }

    public void addEnvironmentVariable(String key,String value){
        CREnvironmentVariable_1 variable = new CREnvironmentVariable_1(key);
        variable.setValue(value);
        this.environmentVariables.add(variable);
    }
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        CRStage_1 that = (CRStage_1) o;

        if (fetchMaterials != that.fetchMaterials) {
            return false;
        }
        if (artifactCleanupProhibited != that.artifactCleanupProhibited) {
            return false;
        }
        if (cleanWorkingDir != that.cleanWorkingDir) {
            return false;
        }
        if (approval != null ? !approval.equals(that.approval) : that.approval != null) {
            return false;
        }
        if (jobs != null ? !CollectionUtils.isEqualCollection(jobs, that.jobs) : that.jobs != null) {
            return false;
        }
        if (environmentVariables != null ? !CollectionUtils.isEqualCollection(environmentVariables,that.environmentVariables) : that.environmentVariables != null) {
            return false;
        }
        if (name != null ? !name.equals(that.name) : that.name != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (fetchMaterials ? 1 : 0);
        result = 31 * result + (artifactCleanupProhibited ? 1 : 0);
        result = 31 * result + (cleanWorkingDir ? 1 : 0);
        result = 31 * result + (approval != null ? approval.hashCode() : 0);
        result = 31 * result + (environmentVariables != null ? environmentVariables.size() : 0);
        result = 31 * result + (jobs != null ? jobs.size() : 0);
        return result;
    }

    @Override
    public void getErrors(ErrorCollection errors) {
        validateName(errors);
        validateAtLeastOneJob(errors);
        validateEnvironmentVariableUniqueness(errors);
        validateJobNameUniqueness(errors);
        if(approval != null)
            approval.getErrors(errors);

    }

    private void validateJobNameUniqueness(ErrorCollection errors) {
        if(this.jobs == null)
            return;
        HashSet<String> keys = new HashSet<>();
        for(CRJob_1 var : jobs)
        {
            String error = var.validateNameUniqueness(keys);
            if(error != null)
                errors.add(this,error);
        }
    }

    private void validateEnvironmentVariableUniqueness(ErrorCollection errors) {
        HashSet<String> keys = new HashSet<>();
        for(CREnvironmentVariable_1 var : environmentVariables)
        {
            String error = var.validateNameUniqueness(keys);
            if(error != null)
                errors.add(this,error);
        }
    }

    private void validateAtLeastOneJob(ErrorCollection errors) {
        if(this.jobs == null || this.jobs.isEmpty())
            errors.add(this,"Stage has no jobs");
    }

    private void validateName(ErrorCollection errors) {
        if (StringUtil.isBlank(name)) {
            errors.add(this, "Stage name not set");
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isFetchMaterials() {
        return fetchMaterials;
    }

    public void setFetchMaterials(boolean fetchMaterials) {
        this.fetchMaterials = fetchMaterials;
    }

    public boolean isArtifactCleanupProhibited() {
        return artifactCleanupProhibited;
    }

    public void setArtifactCleanupProhibited(boolean artifactCleanupProhibited) {
        this.artifactCleanupProhibited = artifactCleanupProhibited;
    }

    public boolean isCleanWorkingDir() {
        return cleanWorkingDir;
    }

    public void setCleanWorkingDir(boolean cleanWorkingDir) {
        this.cleanWorkingDir = cleanWorkingDir;
    }

    public CRApproval_1 getApproval() {
        return approval;
    }

    public void setApproval(CRApproval_1 approval) {
        this.approval = approval;
    }

    public Collection<CREnvironmentVariable_1> getEnvironmentVariables() {
        return environmentVariables;
    }

    public void setEnvironmentVariables(Collection<CREnvironmentVariable_1> environmentVariables) {
        this.environmentVariables = environmentVariables;
    }

    public Collection<CRJob_1> getJobs() {
        return jobs;
    }

    public void setJobs(Collection<CRJob_1> jobs) {
        this.jobs = jobs;
    }
}
