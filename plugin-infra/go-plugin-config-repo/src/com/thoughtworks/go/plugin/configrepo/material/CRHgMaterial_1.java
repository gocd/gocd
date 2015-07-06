package com.thoughtworks.go.plugin.configrepo.material;

import com.thoughtworks.go.plugin.configrepo.ErrorCollection;
import org.apache.commons.lang.StringUtils;

public class CRHgMaterial_1 extends CRScmMaterial_1 {
    public static final String TYPE_NAME = "hg";

    private String url;

    public CRHgMaterial_1()
    {
        type = TYPE_NAME;
    }

    public CRHgMaterial_1(String materialName, String folder, boolean autoUpdate,String url, String... filters) {
        super(TYPE_NAME, materialName, folder, autoUpdate, filters);
        this.url = url;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        CRHgMaterial_1 that = (CRHgMaterial_1)o;
        if(that == null)
            return  false;

        if(!super.equals(that))
            return false;

        if (url != null ? !url.equals(that.url) : that.url != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (url != null ? url.hashCode() : 0);
        return result;
    }

    @Override
    public String typeName() {
        return TYPE_NAME;
    }

    @Override
    public void getErrors(ErrorCollection errors) {
        validateUrl(errors);
    }

    private void validateUrl(ErrorCollection errors) {
        if (StringUtils.isBlank(url)) {
            errors.add(this, "Hg repository URL is not specified");
        }
    }


    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
