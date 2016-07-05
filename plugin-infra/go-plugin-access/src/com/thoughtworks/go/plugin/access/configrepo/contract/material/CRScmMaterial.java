package com.thoughtworks.go.plugin.access.configrepo.contract.material;

import com.thoughtworks.go.plugin.access.configrepo.ErrorCollection;
import org.apache.commons.collections.CollectionUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public abstract class CRScmMaterial extends CRMaterial implements SourceCodeMaterial {
    protected CRFilter filter;
    protected String destination;
    protected Boolean auto_update = true;

    public CRScmMaterial() {
    }
    public CRScmMaterial(String type,String materialName,String folder,boolean autoUpdate,boolean whitelist, String... filters)
    {
        super(type,materialName);
        this.destination = folder;
        this.filter = new CRFilter(Arrays.asList(filters),whitelist);
        this.auto_update = autoUpdate;
    }

    public CRScmMaterial(String type,String materialName, String folder, boolean autoUpdate,boolean whitelist, List<String> filter) {
        super(type,materialName);
        this.destination = folder;
        this.filter = new CRFilter(filter,whitelist);
        this.auto_update = autoUpdate;
    }
    public CRScmMaterial(String name,String folder,boolean autoUpdate,boolean whitelist,List<String> filter) {
        super(name);
        this.destination = folder;
        this.filter = new CRFilter(filter,whitelist);
        this.auto_update = autoUpdate;
    }

    public List<String> getFilterList() {
        if(filter == null)
            return null;
        return filter.getList();
    }

    public boolean isWhitelist() {
        if(this.filter != null)
            return this.filter.isWhitelist();
        return false;
    }

    protected void getCommonErrors(ErrorCollection errors, String parentLocation) {
        String location = getLocation(parentLocation);
        if(this.filter != null)
            this.filter.getErrors(errors,location);
    }

    public void setWhitelistNoCheck(String... filters) { //for tests
        this.filter.setWhitelistNoCheck(Arrays.asList(filters));
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

    public String getDestination()
    {
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
        if (filter != null ? !filter.equals(that.filter) : that.filter != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (destination != null ? destination.hashCode() : 0);
        result = 31 * result + (filter != null ? filter.hashCode() : 0);
        return result;
    }
}
