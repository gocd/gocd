package com.thoughtworks.go.plugin.configrepo;

import com.thoughtworks.go.util.StringUtil;

public class CRTimer_1 extends CRBase {
    private String timerSpec;
    private boolean onlyOnChanges;

    public CRTimer_1() {}
    public CRTimer_1(String timerSpec)
    {
        this.timerSpec = timerSpec;
    }

    public String getTimerSpec() {
        return timerSpec;
    }

    public void setTimerSpec(String timerSpec) {
        this.timerSpec = timerSpec;
    }

    public boolean isOnlyOnChanges() {
        return onlyOnChanges;
    }

    public void setOnlyOnChanges(boolean onlyOnChanges) {
        this.onlyOnChanges = onlyOnChanges;
    }

    @Override
    public void getErrors(ErrorCollection errors) {
        validateTimerSpec(errors);
    }

    private void validateTimerSpec(ErrorCollection errors) {
        if (StringUtil.isBlank(timerSpec)) {
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

        if (timerSpec != null ? !timerSpec.equals(that.timerSpec) : that.timerSpec != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return timerSpec != null ? timerSpec.hashCode() : 0;
    }
}
