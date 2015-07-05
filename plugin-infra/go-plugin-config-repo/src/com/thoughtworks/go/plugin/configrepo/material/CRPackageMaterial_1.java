package com.thoughtworks.go.plugin.configrepo.material;


import com.thoughtworks.go.plugin.configrepo.ErrorCollection;
import org.apache.commons.lang.StringUtils;

public class CRPackageMaterial_1 extends CRMaterial_1 {
    public static final String TYPE_NAME = "package";

    private String packageId;

    public CRPackageMaterial_1() {
        type = TYPE_NAME;
    }
    public CRPackageMaterial_1(String packageId)
    {
        type = TYPE_NAME;
        this.packageId = packageId;
    }
    public CRPackageMaterial_1(String material,String packageId)
    {
        super(TYPE_NAME,material);
        this.packageId = packageId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CRPackageMaterial_1 that = (CRPackageMaterial_1) o;
        if(!super.equals(that))
            return false;

        if (packageId != null ? !packageId.equals(that.packageId) : that.packageId != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (packageId != null ? packageId.hashCode() : 0);
        return result;
    }

    @Override
    public String typeName() {
        return TYPE_NAME;
    }

    @Override
    public void getErrors(ErrorCollection errors) {
        validatePackageId(errors);
    }

    private void validatePackageId(ErrorCollection errors) {
        if (StringUtils.isBlank(packageId)) {
            errors.add(this,"Package repository not set. Please select a repository and package");
        }
    }

    public String getPackageId() {
        return packageId;
    }

    public void setPackageId(String packageId) {
        this.packageId = packageId;
    }
}
