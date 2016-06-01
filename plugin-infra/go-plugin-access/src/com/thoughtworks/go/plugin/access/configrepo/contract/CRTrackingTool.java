package com.thoughtworks.go.plugin.access.configrepo.contract;

import com.thoughtworks.go.plugin.access.configrepo.ErrorCollection;
import com.thoughtworks.go.util.StringUtil;

public class CRTrackingTool extends CRBase {
    private String link;
    private String regex;

    public CRTrackingTool(){}
    public CRTrackingTool(String link, String regex) {
        this.link = link;
        this.regex = regex;
    }

    private void validateLink(ErrorCollection errors, String location) {
        if (link != null && !link.contains("${ID}")) {
            errors.addError(location, "Link must be a URL containing '${ID}'. Go will replace the string '${ID}' with the first matched group from the regex at run-time.");
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
        CRTrackingTool that = (CRTrackingTool) o;
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


    @Override
    public void getErrors(ErrorCollection errors, String parentLocation) {
        String location = getLocation(parentLocation);
        errors.checkMissing(location,"link",link);
        errors.checkMissing(location,"regex",regex);
        validateLink(errors,location);
    }

    @Override
    public String getLocation(String parent) {
        String myLocation = getLocation() == null ? parent : getLocation();
        return String.format("%s; Tracking tool",myLocation);
    }
}
