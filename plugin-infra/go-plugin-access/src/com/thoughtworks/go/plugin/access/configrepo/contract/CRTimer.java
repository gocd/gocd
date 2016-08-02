package com.thoughtworks.go.plugin.access.configrepo.contract;

import com.thoughtworks.go.plugin.access.configrepo.ErrorCollection;
import com.thoughtworks.go.util.StringUtil;

public class CRTimer extends CRBase {
    private String spec;
    private Boolean only_on_changes;

    public CRTimer() {}
    public CRTimer(String timerSpec)
    {
        this.spec = timerSpec;
    }
    public CRTimer(String timerSpec, Boolean onlyOnChanges) {
        this.spec = timerSpec;
        this.only_on_changes = onlyOnChanges;
    }

    public String getTimerSpec() {
        return spec;
    }

    public void setTimerSpec(String timerSpec) {
        this.spec = timerSpec;
    }

    public Boolean isOnlyOnChanges() {
        return only_on_changes;
    }

    public void setOnlyOnChanges(boolean onlyOnChanges) {
        this.only_on_changes = onlyOnChanges;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        CRTimer that = (CRTimer) o;

        if (spec != null ? !spec.equals(that.spec) : that.spec != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return spec != null ? spec.hashCode() : 0;
    }

    @Override
    public void getErrors(ErrorCollection errors, String parentLocation) {
        String location = this.getLocation(parentLocation);
        errors.checkMissing(location,"spec",spec);
    }

    @Override
    public String getLocation(String parent) {
        String myLocation = getLocation() == null ? parent : getLocation();
        return String.format("%s; Timer",myLocation);
    }
}
