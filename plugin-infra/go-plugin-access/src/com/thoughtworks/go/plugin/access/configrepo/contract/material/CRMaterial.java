package com.thoughtworks.go.plugin.access.configrepo.contract.material;

public abstract class CRMaterial {
    private String name;

    public CRMaterial(String name)
    {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
