package com.thoughtworks.go.plugin.access.configrepo.contract.material;


public class CRPackageMaterial extends CRMaterial {
    private String packageId;

    public CRPackageMaterial(String name) {
        super(name);
    }

    public CRPackageMaterial(String name, String packageId) {
        super(name);
        this.packageId = packageId;
    }

    public String getPackageId() {
        return packageId;
    }
}
