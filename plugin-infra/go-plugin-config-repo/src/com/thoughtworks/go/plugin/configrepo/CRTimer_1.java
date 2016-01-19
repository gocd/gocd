package com.thoughtworks.go.plugin.configrepo;

import com.thoughtworks.go.util.StringUtil;

public class CRTimer_1 extends CRBase {
    private String spec;
    private boolean only_on_changes;

    public CRTimer_1() {}
    public CRTimer_1(String timerSpec)
    {
        this.spec = timerSpec;
    }

    public String getTimerSpec() {
        return spec;
    }

    public void setTimerSpec(String timerSpec) {
        this.spec = timerSpec;
    }

    public boolean isOnlyOnChanges() {
        return only_on_changes;
    }

    public void setOnlyOnChanges(boolean onlyOnChanges) {
        this.only_on_changes = onlyOnChanges;
    }

    @Override
    public void getErrors(ErrorCollection errors) {
        validateTimerSpec(errors);
    }

    private void validateTimerSpec(ErrorCollection errors) {
        if (StringUtil.isBlank(spec)) {
            errors.add(this, "Timer has no cron expression set");
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

        CRTimer_1 that = (CRTimer_1) o;

        if (spec != null ? !spec.equals(that.spec) : that.spec != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return spec != null ? spec.hashCode() : 0;
    }
}
