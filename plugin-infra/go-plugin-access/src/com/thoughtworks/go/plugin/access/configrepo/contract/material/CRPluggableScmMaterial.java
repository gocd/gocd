package com.thoughtworks.go.plugin.access.configrepo.contract.material;

import com.thoughtworks.go.plugin.access.configrepo.ErrorCollection;
import org.apache.commons.collections.CollectionUtils;

import java.util.Arrays;
import java.util.List;


public class CRPluggableScmMaterial extends CRMaterial {
    public static final String TYPE_NAME = "pluggablescm";

    private String scm_id;
    protected String destination;
    private List<String> filter;

    public CRPluggableScmMaterial(){
        type = TYPE_NAME;
    }
    public CRPluggableScmMaterial(String materialName,String scmId,String folder,String... filters)
    {
        super(TYPE_NAME,materialName);
        this.scm_id = scmId;
        this.destination = folder;
        this.filter = Arrays.asList(filters);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        CRPluggableScmMaterial that = (CRPluggableScmMaterial)o;
        if(that == null)
            return  false;

        if(!super.equals(that))
            return false;

        if (scm_id != null ? !scm_id.equals(that.scm_id) : that.scm_id != null) {
            return false;
        }
        if (destination != null ? !destination.equals(that.destination) : that.destination != null) {
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
        result = 31 * result + (scm_id != null ? scm_id.hashCode() : 0);
        result = 31 * result + (destination != null ? destination.hashCode() : 0);
        result = 31 * result + (filter != null ? filter.size() : 0);
        return result;
    }

    @Override
    public String typeName() {
        return TYPE_NAME;
    }

    public void addFilter(String pattern)
    {
        this.filter.add(pattern);
    }

    public String getScmId() {
        return scm_id;
    }

    public void setScmId(String scmId) {
        this.scm_id = scmId;
    }

    public List<String> getFilter() {
        return filter;
    }

    public void setFilter(List<String> filter) {
        this.filter = filter;
    }

    public String getDirectory() {
        return destination;
    }

    @Override
    public void getErrors(ErrorCollection errors, String parentLocation) {
        String location = getLocation(parentLocation);
        errors.checkMissing(location,"scm_id", scm_id);
    }

    @Override
    public String getLocation(String parent) {
        String myLocation = getLocation() == null ? parent : getLocation();
        String name = getName() == null ? "" : getName();
        String url = getScmId() != null ? getScmId() : "unknown";
        return String.format("%s; Pluggable SCM material %s ID: %s",myLocation,name,url);
    }
}
