package com.thoughtworks.go.plugin.access.configrepo.messages;

import com.thoughtworks.go.plugin.access.configrepo.contract.CREnvironment;
import com.thoughtworks.go.plugin.access.configrepo.contract.CRError;
import com.thoughtworks.go.plugin.access.configrepo.contract.CRPipeline;
import com.thoughtworks.go.plugin.access.configrepo.ErrorCollection;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ParseDirectoryResponseMessage {
    private String target_version;
    private Collection<CREnvironment> environments = new ArrayList<>();
    private Collection<CRPipeline> pipelines = new ArrayList<>();
    private List<CRError> errors = new ArrayList<>();

    public boolean hasErrors() {
        return errors != null && !errors.isEmpty();
    }

    public String getTargetVersion() {
        return target_version;
    }

    public void setTargetVersion(String target_version) {
        this.target_version = target_version;
    }

    public void validateResponse(ErrorCollection errors) {
        String location = "Plugin response message";
        errors.checkMissing(location,"target_version",target_version);
        for(CRPipeline pipeline : pipelines)
        {
            pipeline.getErrors(errors,location);
        }
        for(CREnvironment environment : environments)
        {
            environment.getErrors(errors,location);
        }
    }

    public Collection<CREnvironment> getEnvironments() {
        return environments;
    }

    public void setEnvironments(Collection<CREnvironment> environments) {
        this.environments = environments;
    }

    public Collection<CRPipeline> getPipelines() {
        return pipelines;
    }

    public void setPipelines(Collection<CRPipeline> pipelines) {
        this.pipelines = pipelines;
    }

    public List<CRError> getPluginErrors() {
        return errors;
    }
}
