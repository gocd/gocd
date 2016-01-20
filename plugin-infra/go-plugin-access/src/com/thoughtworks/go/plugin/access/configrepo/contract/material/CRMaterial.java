package com.thoughtworks.go.plugin.access.configrepo.contract.material;

import com.thoughtworks.go.plugin.access.configrepo.contract.CRBase;
import com.thoughtworks.go.plugin.access.configrepo.contract.CRValidatable;

import java.util.HashSet;

public abstract class CRMaterial extends CRBase {
    private String materialName;
    protected String type;

    public CRMaterial() {
    }

    public CRMaterial(String name) {
        this.materialName = name;
    }
    public CRMaterial(String type,String name) {
        this.type = type;
        this.materialName = name;
    }

    public String getName() {
        return materialName;
    }

    public void setName(String name) {
        this.materialName = name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        CRMaterial that = (CRMaterial) o;

        if (materialName != null ? !materialName.equals(that.materialName) : that.materialName != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = materialName != null ? materialName.hashCode() : 0;
        return result;
    }


    public abstract String typeName();

    public String validateNameUniqueness(HashSet<String> keys) {
        if(this.getName() == null)
            return String.format("Material has no name when there is more than one material in pipeline");
        else if(keys.contains(this.getName()))
            return String.format("Material named %s is defined more than once",this.getName());
        else
            keys.add(this.getName());
        return null;
    }
}
