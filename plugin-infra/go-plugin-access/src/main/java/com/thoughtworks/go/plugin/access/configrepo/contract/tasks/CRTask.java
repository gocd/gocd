/*
 * Copyright 2017 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.thoughtworks.go.plugin.access.configrepo.contract.tasks;

import com.thoughtworks.go.plugin.access.configrepo.contract.CRBase;

public abstract class CRTask extends CRBase {
    private CRRunIf run_if;
    private CRTask on_cancel;
    protected String type;

    public CRTask(CRRunIf runIf,CRTask onCancel)
    {
        this.run_if = runIf;
        this.on_cancel = onCancel;
    }

    public CRTask() {}
    public CRTask(String type)
    {
        this.type = type;
    }

    public CRTask(String type, CRRunIf runIf, CRTask onCancel) {
        this.type = type;
        this.run_if = runIf;
        this.on_cancel = onCancel;
    }

    public CRRunIf getRunIf() {
        return run_if;
    }

    public CRTask getOnCancel() {
        return on_cancel;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        CRTask that = (CRTask) o;

        if (type != null ? !type.equals(that.type) : that.type != null) {
            return false;
        }
        if (run_if != null ? !run_if.equals(that.run_if) : that.run_if != null) {
            return false;
        }
        if (on_cancel != null ? !on_cancel.equals(that.on_cancel) : that.on_cancel != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = type != null ? type.hashCode() : 0;
        result = 31 * result + (run_if != null ? run_if.hashCode() : 0);
        result = 31 * result + (on_cancel != null ? on_cancel.hashCode() : 0);
        return result;
    }

    public void setRunIf(CRRunIf runIf) {
        this.run_if = runIf;
    }

    public void setOn_cancel(CRTask on_cancel) {
        this.on_cancel = on_cancel;
    }
}
