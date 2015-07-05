package com.thoughtworks.go.plugin.configrepo.material;

import com.thoughtworks.go.plugin.configrepo.ErrorCollection;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;

import java.util.Arrays;
import java.util.List;


public class CRPluggableScmMaterial_1 extends CRMaterial_1 {
    public static final String TYPE_NAME = "pluggablescm";

    private String scmId;
    protected String folder;
    private List<String> filter;

    public CRPluggableScmMaterial_1(){
        type = TYPE_NAME;
    }
    public CRPluggableScmMaterial_1(String materialName,String scmId,String folder,String... filters)
    {
        super(TYPE_NAME,materialName);
        this.scmId = scmId;
        this.folder = folder;
        this.filter = Arrays.asList(filters);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        CRPluggableScmMaterial_1 that = (CRPluggableScmMaterial_1)o;
        if(that == null)
            return  false;

        if(!super.equals(that))
            return false;

        if (scmId != null ? !scmId.equals(that.scmId) : that.scmId != null) {
            return false;
        }
        if (folder != null ? !folder.equals(that.folder) : that.folder != null) {
            return false;
        }

        if (filter != null ? !CollectionUtils.isEqualCollection(this.filter, that.filter) : that.filter != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = (this.getName() != null ? this.getName().hashCode() : 0);
        result = 31 * result + (scmId != null ? scmId.hashCode() : 0);
        result = 31 * result + (folder != null ? folder.hashCode() : 0);
        result = 31 * result + (filter != null ? filter.size() : 0);
        return result;
    }

    @Override
    public String typeName() {
        return TYPE_NAME;
    }

    @Override
    public void getErrors(ErrorCollection errors) {
        validateScmId(errors);
    }

    private void validateScmId(ErrorCollection errors) {
        if (StringUtils.isBlank(scmId)) {
            errors.add(this,"Scm repository not set. Please specify a scm repository");
        }
    }

    public void addFilter(String pattern)
    {
        this.filter.add(pattern);
    }

    public String getScmId() {
        return scmId;
    }

    public void setScmId(String scmId) {
        this.scmId = scmId;
    }

    public List<String> getFilter() {
        return filter;
    }

    public void setFilter(List<String> filter) {
        this.filter = filter;
    }

    public String getDirectory() {
        return folder;
    }
}
