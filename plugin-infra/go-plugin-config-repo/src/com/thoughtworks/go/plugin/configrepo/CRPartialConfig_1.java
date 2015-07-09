package com.thoughtworks.go.plugin.configrepo;

import org.apache.commons.collections.CollectionUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

public class CRPartialConfig_1 extends CRBase {
    private Collection<CREnvironment_1> environments;
    private Collection<CRPipelineGroup_1> groups;

    public CRPartialConfig_1(){
        environments = new ArrayList<CREnvironment_1>();
        groups = new ArrayList<>();
    }


    @Override
    public void getErrors(ErrorCollection errors) {
        this.validateEnvironmentNameUniqueness(errors);
        this.validateGroupNameUniqueness(errors);
    }

    private void validateGroupNameUniqueness(ErrorCollection errors) {
        HashSet<String> keys = new HashSet<>();
        for(CRPipelineGroup_1 group : groups)
        {
            String error = group.validateNameUniqueness(keys);
            if(error != null)
                errors.add(this,error);
        }
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
        if (groups != null ? !CollectionUtils.isEqualCollection(this.groups, that.groups) : that.groups != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = 0;
        result = 31 * result + (environments != null ? environments.hashCode() : 0);
        result = 31 * result + (groups != null ? groups.hashCode() : 0);
        return result;
    }

    public Collection<CREnvironment_1> getEnvironments() {
        return environments;
    }

    public void addGroup(CRPipelineGroup_1 group) {
        this.groups.add(group);
    }

    public Collection<CRPipelineGroup_1> getGroups() {
        return groups;
    }
}
