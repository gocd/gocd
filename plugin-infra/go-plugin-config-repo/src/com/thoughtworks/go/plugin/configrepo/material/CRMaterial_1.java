package com.thoughtworks.go.plugin.configrepo.material;

import com.thoughtworks.go.plugin.configrepo.CRBase;
import com.thoughtworks.go.plugin.configrepo.ErrorCollection;

public abstract class CRMaterial_1 extends CRBase {
    private String materialName;
    protected String type;

    public CRMaterial_1() {
    }

    public CRMaterial_1(String name) {
        this.materialName = name;
    }
    public CRMaterial_1(String type,String name) {
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

        CRMaterial_1 that = (CRMaterial_1) o;

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
}
