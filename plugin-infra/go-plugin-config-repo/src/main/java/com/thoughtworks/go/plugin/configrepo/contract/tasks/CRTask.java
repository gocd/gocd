/*
 * Copyright 2021 ThoughtWorks, Inc.
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
package com.thoughtworks.go.plugin.configrepo.contract.tasks;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.thoughtworks.go.plugin.configrepo.contract.CRBase;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public abstract class CRTask extends CRBase {
    @SerializedName("run_if")
    @Expose
    private CRRunIf runIf;
    @SerializedName("on_cancel")
    @Expose
    private CRTask onCancel;

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    @SerializedName("type")
    @Expose
    protected String type;

    public CRTask() {
        this(null, null, null);
    }

    public CRTask(String type, CRRunIf runIf, CRTask onCancel) {
        this.type = type;
        this.runIf = runIf;
        this.onCancel = onCancel;
    }
}
