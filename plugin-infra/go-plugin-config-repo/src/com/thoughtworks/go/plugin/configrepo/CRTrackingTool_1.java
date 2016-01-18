package com.thoughtworks.go.plugin.configrepo;

import com.thoughtworks.go.util.StringUtil;

public class CRTrackingTool_1 extends CRBase {
    private String link;
    private String regex;

    public CRTrackingTool_1(){}
    public CRTrackingTool_1(String link, String regex) {
        this.link = link;
        this.regex = regex;
    }

    @Override
    public void getErrors(ErrorCollection errors) {
        validateLink(errors);
        validateRegex(errors);
    }

    private void validateRegex(ErrorCollection errors) {
        if (StringUtil.isBlank(regex)) {
            errors.add(this, "Tracking tool regex not set");
        }
    }

    private void validateLink(ErrorCollection errors) {
        if (StringUtil.isBlank(link)) {
            errors.add(this, "Tracking tool link not set");
        }
        if (link != null && !link.contains("${ID}")) {
            errors.add(this, "Link must be a URL containing '${ID}'. Go will replace the string '${ID}' with the first matched group from the regex at run-time.");
        }
    }
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CRTrackingTool_1 that = (CRTrackingTool_1) o;
        if (link != null ? !link.equals(that.link) : that.link != null) {
            return false;
        }
        return !(regex != null ? !regex.equals(that.regex) : that.regex != null);

    }

    @Override
    public int hashCode() {
        int result = link != null ? link.hashCode() : 0;
        result = 31 * result + (regex != null ? regex.hashCode() : 0);
        return result;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public String getRegex() {
        return regex;
    }

    public void setRegex(String regex) {
        this.regex = regex;
    }


}
