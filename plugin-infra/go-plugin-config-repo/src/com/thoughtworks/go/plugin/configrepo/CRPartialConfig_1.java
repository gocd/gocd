package com.thoughtworks.go.plugin.configrepo;

import org.apache.commons.collections.CollectionUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

public class CRPartialConfig_1 extends CRBase {
    private Collection<CREnvironment_1> environments;
    private Collection<CRPipeline_1> pipelines;

    public CRPartialConfig_1(){
        environments = new ArrayList<CREnvironment_1>();
        pipelines = new ArrayList<>();
    }


    @Override
    public void getErrors(ErrorCollection errors) {
        this.validateEnvironmentNameUniqueness(errors);
    }

    private void validateEnvironmentNameUniqueness(ErrorCollection errors) {
        HashSet<String> keys = new HashSet<>();
        for(CREnvironment_1 environment : environments)
        {
            String error = environment.validateNameUniqueness(keys);
            if(error != null)
                errors.add(this,error);
        }
    }

    public void addEnvironment(CREnvironment_1 environment) {
        this.environments.add(environment);
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        CRPartialConfig_1 that = (CRPartialConfig_1)o;
        if(that == null)
            return  false;

        if (environments != null ? !CollectionUtils.isEqualCollection(this.environments, that.environments) : that.environments != null) {
            return false;
        }
        // for some reason collection utils were not using equals impl of pipeline config.
        if (pipelines != null ? !arePipelinesEqual(this.pipelines, that.pipelines) : that.pipelines != null) {
            return false;
        }

        return true;
    }

    private boolean arePipelinesEqual(Collection<CRPipeline_1> pipelines, Collection<CRPipeline_1> pipelines1) {
        if(pipelines.size() != pipelines1.size())
            return false;
        for(CRPipeline_1 p1 : pipelines)
        {
            if(!pipelines1.contains(p1))
                return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int result = 0;
        result = 31 * result + (environments != null ? environments.hashCode() : 0);
        result = 31 * result + (pipelines != null ? pipelines.hashCode() : 0);
        return result;
    }

    public Collection<CREnvironment_1> getEnvironments() {
        return environments;
    }

    public void addPipeline(CRPipeline_1 pipeline) {
        this.pipelines.add(pipeline);
    }

    public Collection<CRPipeline_1> getPipelines() {
        return pipelines;
    }
}
