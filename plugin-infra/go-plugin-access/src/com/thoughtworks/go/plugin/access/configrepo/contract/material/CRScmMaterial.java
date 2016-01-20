package com.thoughtworks.go.plugin.access.configrepo.contract.material;

import org.apache.commons.collections.CollectionUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public abstract class CRScmMaterial extends CRMaterial {
    protected List<String> filter  = new ArrayList<String>();
    protected String destination;
    protected Boolean auto_update = true;

    public CRScmMaterial() {
    }
    public CRScmMaterial(String type,String materialName,String folder,boolean autoUpdate, String... filters)
    {
        super(type,materialName);
        this.destination = folder;
        this.filter = Arrays.asList(filters);
        this.auto_update = autoUpdate;
    }

    public CRScmMaterial(String type,String materialName, String folder, boolean autoUpdate, List<String> filter) {
        super(type,materialName);
        this.destination = folder;
        this.filter = filter;
        this.auto_update = autoUpdate;
    }

    public List<String> getFilter() {
        return filter;
    }

    public void setFilter(List<String> filter) {
        this.filter = filter;
    }

    public boolean isAutoUpdate() {
        return auto_update;
    }

    public void setAutoUpdate(boolean autoUpdate) {
        this.auto_update = autoUpdate;
    }

    public String getDirectory() {
        return destination;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        CRScmMaterial that = (CRScmMaterial)o;
        if(that == null)
            return  false;

        if(!super.equals(that))
            return false;

        if (!auto_update == that.auto_update) {
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
        int result = super.hashCode();
        result = 31 * result + (destination != null ? destination.hashCode() : 0);
        result = 31 * result + (filter != null ? filter.size() : 0);
        return result;
    }

}
