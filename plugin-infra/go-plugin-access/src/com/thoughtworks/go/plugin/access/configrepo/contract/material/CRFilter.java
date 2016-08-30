package com.thoughtworks.go.plugin.access.configrepo.contract.material;

import com.thoughtworks.go.plugin.access.configrepo.ErrorCollection;
import com.thoughtworks.go.plugin.access.configrepo.contract.CRBase;
import org.apache.commons.collections.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

public class CRFilter extends CRBase {
    private List<String> ignore  = new ArrayList<String>();
    private List<String> whitelist  = new ArrayList<String>();

    public CRFilter(List<String> list,boolean whitelist) {
        if(whitelist)
            this.whitelist = list;
        else
            this.ignore = list;
    }

    @Override
    public void getErrors(ErrorCollection errors, String parentLocation) {
        if (this.isBlacklist() && this.isWhitelist()) {
            errors.addError(getLocation(parentLocation), "Material filter cannot contain both ignores and whitelist");
        }
    }

    @Override
    public String getLocation(String parent) {
        String myLocation = getLocation() == null ? parent : getLocation();
        return String.format("%s; Filter",myLocation);
    }

    public boolean isEmpty() { return (whitelist == null || whitelist.isEmpty()) && (ignore == null || ignore.isEmpty()); }

    public boolean isWhitelist() {
        return whitelist != null && whitelist.size() > 0;
    }

    public List<String> getList() {
        if(isBlacklist())
            return ignore;
        else
            return whitelist;
    }

    private boolean isBlacklist() {
        return ignore != null && ignore.size() > 0;
    }

    public void setIgnore(List<String> ignore) {
        this.ignore = ignore;
        this.whitelist = null;
    }

    public void setWhitelist(List<String> whitelist) {
        this.ignore = null;
        this.whitelist = whitelist;
    }

    public void setWhitelistNoCheck(List<String> list) {
        this.whitelist = list;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        CRFilter that = (CRFilter)o;
        if(that == null)
            return  false;

        if (ignore != null ? !CollectionUtils.isEqualCollection(this.ignore, that.ignore) : that.ignore != null) {
            return false;
        }
        if (whitelist != null ? !CollectionUtils.isEqualCollection(this.whitelist, that.whitelist) : that.whitelist != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = 32;
        result = 31 * result + (ignore != null ? ignore.size() : 0);
        result = 31 * result + (whitelist != null ? whitelist.size() : 0);
        return result;
    }

}
