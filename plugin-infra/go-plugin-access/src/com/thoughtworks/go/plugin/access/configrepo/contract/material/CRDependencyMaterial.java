package com.thoughtworks.go.plugin.access.configrepo.contract.material;

public class CRDependencyMaterial extends CRMaterial {
    private String pipelineName ;
    private String stageName ;

    public CRDependencyMaterial(String name, String pipelineName, String stageName)
    {
        super(name);
        this.pipelineName = pipelineName;
        this.stageName = stageName;
    }

    public String getPipelineName() {
        return pipelineName;
    }

    public String getStageName() {
        return stageName;
    }
}
