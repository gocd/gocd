package com.thoughtworks.go.plugin.access.configrepo.contract;

import com.thoughtworks.go.plugin.access.configrepo.ErrorCollection;
import com.thoughtworks.go.util.StringUtil;

public class CRPropertyGenerator extends CRBase {
    private String name;
    private String source;
    private String xpath;

    public CRPropertyGenerator(String name,String src,String xpath)
    {
        this.name = name;
        this.source = src;
        this.xpath = xpath;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSrc() {
        return source;
    }

    public void setSrc(String src) {
        this.source = src;
    }

    public String getXpath() {
        return xpath;
    }

    public void setXpath(String xpath) {
        this.xpath = xpath;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        CRPropertyGenerator that = (CRPropertyGenerator) o;

        if (name != null ? !name.equals(that.name) : that.name != null) {
            return false;
        }
        if (source != null ? !source.equals(that.source) : that.source != null) {
            return false;
        }
        if (xpath != null ? !xpath.equals(that.xpath) : that.xpath != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (source != null ? source.hashCode() : 0);
        result = 31 * result + (xpath != null ? xpath.hashCode() : 0);
        return result;
    }

    @Override
    public void getErrors(ErrorCollection errors, String parentLocation) {
        String location = this.getLocation(parentLocation);
        errors.checkMissing(location,"name",name);
        errors.checkMissing(location,"source",source);
        errors.checkMissing(location,"xpath",xpath);
    }

    @Override
    public String getLocation(String parent) {
        String myLocation = getLocation() == null ? parent : getLocation();
        String name = this.getName() == null ? "unknown name" : this.name;
        return String.format("%s; Property generator (%s)",myLocation,name);
    }
}
