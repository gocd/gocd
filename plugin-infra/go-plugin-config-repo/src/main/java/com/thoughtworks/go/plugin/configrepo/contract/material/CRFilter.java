/*
 * Copyright 2020 ThoughtWorks, Inc.
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
    @SerializedName("whitelist")
    @Expose
    private List<String> whitelist = new ArrayList<>();

    public CRFilter(List<String> list, boolean whitelist) {
        if (whitelist)
            this.whitelist = list;
        else
            this.ignore = list;
    }

    @Override
    public void getErrors(ErrorCollection errors, String parentLocation) {
        if (this.isBlacklist() && this.isWhitelist()) {
            errors.addError(getLocation(parentLocation), "Material filter cannot contain both ignores and whitelist");
        }
    }

    @Override
    public String getLocation(String parent) {
        String myLocation = getLocation() == null ? parent : getLocation();
        return String.format("%s; Filter", myLocation);
    }

    public boolean isEmpty() {
        return (whitelist == null || whitelist.isEmpty()) && (ignore == null || ignore.isEmpty());
    }

    public boolean isWhitelist() {
        return whitelist != null && whitelist.size() > 0;
    }

    public List<String> getList() {
        if (isBlacklist())
            return ignore;
        else
            return whitelist;
    }

    private boolean isBlacklist() {
        return ignore != null && ignore.size() > 0;
    }

    public void setIgnore(List<String> ignore) {
        this.ignore = ignore;
        this.whitelist = null;
    }

    public void setWhitelist(List<String> whitelist) {
        this.ignore = null;
        this.whitelist = whitelist;
    }

    public void setWhitelistNoCheck(List<String> list) {
        this.whitelist = list;
    }

}
