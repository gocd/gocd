package com.thoughtworks.go.plugin.configrepo.messages;

import com.thoughtworks.go.plugin.configrepo.CREnvironment_1;
import com.thoughtworks.go.plugin.configrepo.CRError_1;
import com.thoughtworks.go.plugin.configrepo.CRPipeline_1;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ParseDirectoryResponseMessage_1 {
    private Collection<CREnvironment_1> environments = new ArrayList<>();
    private Collection<CRPipeline_1> pipelines = new ArrayList<>();
    private List<CRError_1> errors = new ArrayList<>();

    public boolean hasErrors() {
        return errors != null && !errors.isEmpty();
    }

    public List<CRError_1> getErrors() {
        return errors;
    }

    public Collection<CREnvironment_1> getEnvironments() {
        return environments;
    }

    public void setEnvironments(Collection<CREnvironment_1> environments) {
        this.environments = environments;
    }

    public Collection<CRPipeline_1> getPipelines() {
        return pipelines;
    }

    public void setPipelines(Collection<CRPipeline_1> pipelines) {
        this.pipelines = pipelines;
    }
}
