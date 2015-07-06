package com.thoughtworks.go.plugin.configrepo.material;

import org.apache.commons.collections.CollectionUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public abstract class CRScmMaterial_1 extends CRMaterial_1 {
    protected List<String> filter  = new ArrayList<String>();
    protected String folder;
    protected boolean autoUpdate = true;

    public CRScmMaterial_1() {
    }
    public CRScmMaterial_1(String type,String materialName,String folder,boolean autoUpdate, String... filters)
    {
        super(type,materialName);
        this.folder = folder;
        this.filter = Arrays.asList(filters);
        this.autoUpdate = autoUpdate;
    }

    public List<String> getFilter() {
        return filter;
    }

    public void setFilter(List<String> filter) {
        this.filter = filter;
    }

    public boolean isAutoUpdate() {
        return autoUpdate;
    }

    public void setAutoUpdate(boolean autoUpdate) {
        this.autoUpdate = autoUpdate;
    }

    public String getDirectory() {
        return folder;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        CRScmMaterial_1 that = (CRScmMaterial_1)o;
        if(that == null)
            return  false;

        if(!super.equals(that))
            return false;

        if (!autoUpdate == that.autoUpdate) {
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
        int result = super.hashCode();
        result = 31 * result + (folder != null ? folder.hashCode() : 0);
        result = 31 * result + (filter != null ? filter.size() : 0);
        return result;
    }

}
