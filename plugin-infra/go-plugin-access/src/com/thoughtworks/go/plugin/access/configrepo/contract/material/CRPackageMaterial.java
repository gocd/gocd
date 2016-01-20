package com.thoughtworks.go.plugin.access.configrepo.contract.material;


import com.thoughtworks.go.plugin.access.configrepo.ErrorCollection;
import com.thoughtworks.go.plugin.access.configrepo.contract.MissingConfigLinkedNode;
import com.thoughtworks.go.util.StringUtil;
import org.apache.commons.lang.StringUtils;

public class CRPackageMaterial extends CRMaterial {
    public static final String TYPE_NAME = "package";

    private String packageId;

    public CRPackageMaterial() {
        type = TYPE_NAME;
    }
    public CRPackageMaterial(String packageId)
    {
        type = TYPE_NAME;
        this.packageId = packageId;
    }
    public CRPackageMaterial(String material,String packageId)
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
        CRPackageMaterial that = (CRPackageMaterial) o;
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


    public String getPackageId() {
        return packageId;
    }

    public void setPackageId(String packageId) {
        this.packageId = packageId;
    }

    @Override
    public void getErrors(ErrorCollection errors, String parentLocation) {

    }

    @Override
    public String getLocation(String parent) {
        return null;
    }
}
