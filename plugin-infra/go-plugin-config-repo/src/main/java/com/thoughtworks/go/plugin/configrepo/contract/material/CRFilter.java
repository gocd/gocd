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
package com.thoughtworks.go.plugin.configrepo.contract.material;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.thoughtworks.go.plugin.configrepo.contract.CRBase;
import com.thoughtworks.go.plugin.configrepo.contract.ErrorCollection;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class CRFilter extends CRBase {
    @SerializedName("ignore")
    @Expose
    private List<String> ignore = new ArrayList<>();
    @SerializedName("includes")
    @Expose
    private List<String> includes = new ArrayList<>();

    public CRFilter(List<String> list, boolean isFilterInverted) {
        if (isFilterInverted)
            this.includes = list;
        else
            this.ignore = list;
    }

    @Override
    public void getErrors(ErrorCollection errors, String parentLocation) {
        if (this.isIgnored() && this.isIncluded()) {
            errors.addError(getLocation(parentLocation), "Material filter cannot contain both ignores and includes");
        }
    }

    @Override
    public String getLocation(String parent) {
        String myLocation = getLocation() == null ? parent : getLocation();
        return String.format("%s; Filter", myLocation);
    }

    public boolean isEmpty() {
        return (includes == null || includes.isEmpty()) && (ignore == null || ignore.isEmpty());
    }

    public boolean isIncluded() {
        return includes != null && includes.size() > 0;
    }

    public List<String> getList() {
        if (isIgnored())
            return ignore;
        else
            return includes;
    }

    private boolean isIgnored() {
        return ignore != null && ignore.size() > 0;
    }

    public void setIgnore(List<String> ignore) {
        this.ignore = ignore;
        this.includes = null;
    }

    public void setIncludesNoCheck(List<String> list) {
        this.includes = list;
    }

}
