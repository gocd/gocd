package com.thoughtworks.go.plugin.access.configrepo.contract;

import com.thoughtworks.go.plugin.access.configrepo.ErrorCollection;
import com.thoughtworks.go.util.StringUtil;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class CRParseResult {
    private Collection<CREnvironment> environments = new ArrayList<>();
    private Collection<CRPipeline> pipelines = new ArrayList<>();
    private ErrorCollection errors;

    public CRParseResult(){}
    public CRParseResult(Collection<CREnvironment> environments,Collection<CRPipeline> pipelines,ErrorCollection errors){
        this.environments = environments;
        this.pipelines = pipelines;
        this.errors = errors;
    }

    public CRParseResult(ErrorCollection errors) {
        this.errors = errors;
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

    public ErrorCollection getErrors() {
        return errors;
    }

    public void setErrors(ErrorCollection errors) {
        this.errors = errors;
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }
}
