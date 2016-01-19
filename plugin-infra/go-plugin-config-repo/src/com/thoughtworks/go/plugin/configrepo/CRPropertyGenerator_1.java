package com.thoughtworks.go.plugin.configrepo;

import com.thoughtworks.go.util.StringUtil;

public class CRPropertyGenerator_1 extends CRBase {
    private String name;
    private String source;
    private String xpath;

    public CRPropertyGenerator_1(String name,String src,String xpath)
    {
        this.name = name;
        this.source = src;
        this.xpath = xpath;
    }

    @Override
    public void getErrors(ErrorCollection errors) {
        validateName(errors);
        validateSource(errors);
        validateXPath(errors);
    }

    private void validateXPath(ErrorCollection errors) {
        if (StringUtil.isBlank(xpath)) {
            errors.add(this, "Property generator xpath is not set");
        }
    }

    private void validateSource(ErrorCollection errors) {
        if (StringUtil.isBlank(source)) {
            errors.add(this, "Property generator source is not set");
        }
    }

    private void validateName(ErrorCollection errors) {
        if (StringUtil.isBlank(name)) {
            errors.add(this, "Property generator name is not set");
        }
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

        CRPropertyGenerator_1 that = (CRPropertyGenerator_1) o;

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
}
