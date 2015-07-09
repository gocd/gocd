package com.thoughtworks.go.plugin.configrepo;

import org.apache.commons.collections.CollectionUtils;

import java.util.*;

public class CRPipelineGroup_1 extends CRBase {
    private String name;
    private List<CRPipeline_1> pipelines = new ArrayList<>();

    public CRPipelineGroup_1(){}

    public CRPipelineGroup_1(String name){
        this.name = name;
    }
    public CRPipelineGroup_1(String name,CRPipeline_1... pipelines){
        this.name = name;
        this.pipelines = Arrays.asList(pipelines);
    }
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<CRPipeline_1> getPipelines() {
        return pipelines;
    }

    public void setPipelines(List<CRPipeline_1> pipelines) {
        this.pipelines = pipelines;
    }

    @Override
    public void getErrors(ErrorCollection errors) {

    }


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        CRPipelineGroup_1 that = (CRPipelineGroup_1)o;
        if(that == null)
            return  false;

        if (name != null ? !name.equals(that.getName()) : that.getName() != null) {
            return false;
        }

        if (pipelines != null)
        {
            if(that.pipelines == null)
                return false;
            if(this.pipelines.size() != that.pipelines.size())
                return false;

            for(int i = 0; i < pipelines.size();i++)
            {
                if(!this.pipelines.get(i).equals(that.pipelines.get(i)))
                    return false;
            }
        }
        else if(that.pipelines != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = (name != null ? name.hashCode() : 0);
        result = 31 * result + (pipelines != null ? pipelines.size() : 0);
        return result;
    }

    public String validateNameUniqueness(HashSet<String> keys) {
        if(keys.contains(this.getName()))
            return String.format("Pipeline group %s is defined more than once",this.getName());
        else
            keys.add(this.getName());
        return null;
    }
}
