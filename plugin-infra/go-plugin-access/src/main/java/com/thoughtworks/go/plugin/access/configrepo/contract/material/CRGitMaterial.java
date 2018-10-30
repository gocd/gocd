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

package com.thoughtworks.go.plugin.access.configrepo.contract.material;

import com.thoughtworks.go.plugin.access.configrepo.ErrorCollection;

import java.util.List;

public class CRGitMaterial extends CRScmMaterial {
    public static final String TYPE_NAME = "git";

    private String url;
    private String branch;
    private Boolean shallow_clone;

    public CRGitMaterial()
    {
        type = TYPE_NAME;
    }

    public CRGitMaterial(String materialName, String folder, boolean autoUpdate,Boolean shallow_clone,String url,String branch,boolean whitelist, String... filters) {
        super(TYPE_NAME, materialName, folder, autoUpdate,whitelist, filters);
        this.url = url;
        this.branch = branch;
        this.shallow_clone = shallow_clone;
    }

    public CRGitMaterial(String materialName, String folder, boolean autoUpdate,Boolean shallow_clone,String url,String branch,boolean whitelist, List<String> filters) {
        super(TYPE_NAME, materialName, folder, autoUpdate,whitelist, filters);
        this.url = url;
        this.branch = branch;
        this.shallow_clone = shallow_clone;
    }

    public CRGitMaterial(String name, String folder, boolean autoUpdate,Boolean shallow_clone, List<String> filter,String url,String branch,boolean whitelist) {
        super(name, folder, autoUpdate,whitelist, filter);
        this.url = url;
        this.branch = branch;
        this.shallow_clone = shallow_clone;
    }

    public boolean shallowClone() {
        if(shallow_clone == null)
            return false;//when nothing was specified then no shallow clone
        return shallow_clone;
    }

    @Override
    public String typeName() {
        return TYPE_NAME;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getBranch() {
        return branch;
    }

    public void setBranch(String branch) {
        this.branch = branch;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        CRGitMaterial that = (CRGitMaterial)o;
        if(that == null)
            return  false;

        if(!super.equals(that))
            return false;

        if (url != null ? !url.equals(that.url) : that.url != null) {
            return false;
        }
        if (branch != null ? !branch.equals(that.branch) : that.branch != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (url != null ? url.hashCode() : 0);
        result = 31 * result + (branch != null ? branch.hashCode() : 0);
        return result;
    }

    @Override
    public void getErrors(ErrorCollection errors, String parentLocation) {
        String location = getLocation(parentLocation);
        getCommonErrors(errors,location);
        errors.checkMissing(location,"url",url);
    }

    @Override
    public String getLocation(String parent) {
        String myLocation = getLocation() == null ? parent : getLocation();
        String name = getName() == null ? "" : getName();
        String url = getUrl() != null ? getUrl() : "unknown";
        return String.format("%s; Git material %s URL: %s",myLocation,name,url);
    }

}
