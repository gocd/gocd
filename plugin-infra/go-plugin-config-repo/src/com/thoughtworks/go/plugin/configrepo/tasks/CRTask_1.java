package com.thoughtworks.go.plugin.configrepo.tasks;

import com.thoughtworks.go.plugin.configrepo.CRBase;
import com.thoughtworks.go.plugin.configrepo.ErrorCollection;
import org.apache.commons.lang.StringUtils;

public abstract class CRTask_1 extends CRBase {
    private String type;
    private CRRunIf_1 runIf;
    private CRTask_1 onCancel;

    public CRTask_1() {}
    public CRTask_1(String type)
    {
        this.type = type;
    }

    public CRTask_1(String type, CRRunIf_1 runIf, CRTask_1 onCancel) {
        this.type = type;
        this.runIf = runIf;
        this.onCancel = onCancel;
    }

    public void validateOnCancel(ErrorCollection errors) {
        if(this.onCancel != null)
            this.onCancel.getErrors(errors);
    }
    public void validateType(ErrorCollection errors) {
        if (StringUtils.isBlank(type)) {
            errors.add(this, "Task must have specified type");
        }
    }

    public CRRunIf_1 getRunIf() {
        return runIf;
    }

    public void setRunIf(CRRunIf_1 runIf) {
        this.runIf = runIf;
    }

    public CRTask_1 getOnCancel() {
        return onCancel;
    }

    public void setOnCancel(CRTask_1 onCancel) {
        this.onCancel = onCancel;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        CRTask_1 that = (CRTask_1) o;

        if (type != null ? !type.equals(that.type) : that.type != null) {
            return false;
        }
        if (runIf != null ? !runIf.equals(that.runIf) : that.runIf != null) {
            return false;
        }
        if (onCancel != null ? !onCancel.equals(that.onCancel) : that.onCancel != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = type != null ? type.hashCode() : 0;
        result = 31 * result + (runIf != null ? runIf.hashCode() : 0);
        result = 31 * result + (onCancel != null ? onCancel.hashCode() : 0);
        return result;
    }

    public String typeName() {
        return type;
    }
}
