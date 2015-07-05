package com.thoughtworks.go.plugin.configrepo.material;

import com.thoughtworks.go.plugin.configrepo.ErrorCollection;
import com.thoughtworks.go.util.StringUtil;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;

public class CRGitMaterial_1 extends CRScmMaterial_1 {
    public static final String TYPE_NAME = "git";

    private String url;
    private String branch;

    public CRGitMaterial_1()
    {
        type = TYPE_NAME;
    }

    public CRGitMaterial_1(String materialName, String folder, boolean autoUpdate,String url,String branch, String... filters) {
        super(TYPE_NAME, materialName, folder, autoUpdate, filters);
        this.url = url;
        this.branch = branch;
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
            errors.add(this, "Git repository URL is not specified");
        }
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getBranch() {
        return branch;
    }

    public void setBranch(String branch) {
        this.branch = branch;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        CRGitMaterial_1 that = (CRGitMaterial_1)o;
        if(that == null)
            return  false;

        if(!super.equals(that))
            return false;

        if (url != null ? !url.equals(that.url) : that.url != null) {
            return false;
        }
        if (branch != null ? !branch.equals(that.branch) : that.branch != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (url != null ? url.hashCode() : 0);
        result = 31 * result + (branch != null ? branch.hashCode() : 0);
        return result;
    }
}
