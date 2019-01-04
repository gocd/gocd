/*
 * Copyright 2018 ThoughtWorks, Inc.
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

package com.thoughtworks.go.plugin.configrepo.contract;

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
