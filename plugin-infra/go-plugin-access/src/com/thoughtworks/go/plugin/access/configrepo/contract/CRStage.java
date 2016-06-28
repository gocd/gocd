package com.thoughtworks.go.plugin.access.configrepo.contract;

import com.thoughtworks.go.plugin.access.configrepo.ErrorCollection;
import org.apache.commons.collections.CollectionUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;

public class CRStage extends CRBase {
    private String name;
    private boolean fetch_materials = true;
    private boolean never_cleanup_artifacts;
    private boolean clean_working_directory;
    private CRApproval approval ;
    private Collection<CREnvironmentVariable> environment_variables = new ArrayList<>();
    private Collection<CRJob> jobs = new ArrayList<>();

    public CRStage(String name, boolean fetchMaterials, boolean artifactCleanupProhibited,
                   boolean cleanWorkingDir, CRApproval approval,
                   Collection<CREnvironmentVariable> environmentVariables, Collection<CRJob> jobs) {
        this.name = name;
        this.fetch_materials = fetchMaterials;
        this.never_cleanup_artifacts = artifactCleanupProhibited;
        this.clean_working_directory = cleanWorkingDir;
        this.approval = approval;
        this.environment_variables = environmentVariables;
        this.jobs = jobs;
    }

    public CRStage()
    {
    }
    public CRStage(String name)
    {
        this.name = name;
    }
    public CRStage(String name,CRJob... jobs)
    {
        this.name = name;
        this.jobs = Arrays.asList(jobs);
    }

    public void addEnvironmentVariable(String key,String value){
        CREnvironmentVariable variable = new CREnvironmentVariable(key);
        variable.setValue(value);
        this.environment_variables.add(variable);
    }
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        CRStage that = (CRStage) o;

        if (fetch_materials != that.fetch_materials) {
            return false;
        }
        if (never_cleanup_artifacts != that.never_cleanup_artifacts) {
            return false;
        }
        if (clean_working_directory != that.clean_working_directory) {
            return false;
        }
        if (approval != null ? !approval.equals(that.approval) : that.approval != null) {
            return false;
        }
        if (jobs != null ? !CollectionUtils.isEqualCollection(jobs, that.jobs) : that.jobs != null) {
            return false;
        }
        if (environment_variables != null ? !CollectionUtils.isEqualCollection(environment_variables,that.environment_variables) : that.environment_variables != null) {
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
        result = 31 * result + (fetch_materials ? 1 : 0);
        result = 31 * result + (never_cleanup_artifacts ? 1 : 0);
        result = 31 * result + (clean_working_directory ? 1 : 0);
        result = 31 * result + (approval != null ? approval.hashCode() : 0);
        result = 31 * result + (environment_variables != null ? environment_variables.size() : 0);
        result = 31 * result + (jobs != null ? jobs.size() : 0);
        return result;
    }

    private void validateJobNameUniqueness(ErrorCollection errors, String location) {
        if(this.jobs == null)
            return;
        HashSet<String> keys = new HashSet<>();
        for(CRJob var : jobs)
        {
            String error = var.validateNameUniqueness(keys);
            if(error != null)
                errors.addError(location,error);
        }
    }

    private void validateEnvironmentVariableUniqueness(ErrorCollection errors, String location) {
        HashSet<String> keys = new HashSet<>();
        for(CREnvironmentVariable var : environment_variables)
        {
            String error = var.validateNameUniqueness(keys);
            if(error != null)
                errors.addError(location,error);
        }
    }

    private void validateAtLeastOneJob(ErrorCollection errors, String location) {
        if(this.jobs == null || this.jobs.isEmpty())
            errors.addError(location,"Stage has no jobs");
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isFetchMaterials() {
        return fetch_materials;
    }

    public void setFetchMaterials(boolean fetchMaterials) {
        this.fetch_materials = fetchMaterials;
    }

    public boolean isArtifactCleanupProhibited() {
        return never_cleanup_artifacts;
    }

    public void setArtifactCleanupProhibited(boolean artifactCleanupProhibited) {
        this.never_cleanup_artifacts = artifactCleanupProhibited;
    }

    public boolean isCleanWorkingDir() {
        return clean_working_directory;
    }

    public void setCleanWorkingDir(boolean cleanWorkingDir) {
        this.clean_working_directory = cleanWorkingDir;
    }

    public CRApproval getApproval() {
        return approval;
    }

    public void setApproval(CRApproval approval) {
        this.approval = approval;
    }

    public Collection<CREnvironmentVariable> getEnvironmentVariables() {
        return environment_variables;
    }

    public void setEnvironmentVariables(Collection<CREnvironmentVariable> environmentVariables) {
        this.environment_variables = environmentVariables;
    }

    public Collection<CRJob> getJobs() {
        return jobs;
    }

    public void setJobs(Collection<CRJob> jobs) {
        this.jobs = jobs;
    }

    @Override
    public void getErrors(ErrorCollection errors, String parentLocation) {
        String location = this.getLocation(parentLocation);
        errors.checkMissing(location,"name",name);
        validateAtLeastOneJob(errors,location);
        validateEnvironmentVariableUniqueness(errors,location);
        validateJobNameUniqueness(errors,location);
        if(approval != null)
            approval.getErrors(errors,location);
        if(jobs != null)
        {
            for(CRJob job : jobs) {
                job.getErrors(errors,location);
            }
        }
    }

    @Override
    public String getLocation(String parent) {
        String myLocation = getLocation() == null ? parent : getLocation();
        String stage = getName() == null ? "unknown name" : getName();
        return String.format("%s; Stage (%s)",myLocation,stage);
    }

    public String validateNameUniqueness(HashSet<String> keys) {
        if(keys.contains(this.getName()))
            return String.format("Stage named %s is defined more than once",this.getName());
        else
            keys.add(this.getName());
        return null;
    }

    public void addJob(CRJob crJob) {
        this.jobs.add(crJob);
    }
}
