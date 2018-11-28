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

package com.thoughtworks.go.plugin.configrepo.contract.material;

import com.thoughtworks.go.plugin.configrepo.contract.ErrorCollection;

import java.util.List;

public class CRHgMaterial extends CRScmMaterial {
    public static final String TYPE_NAME = "hg";

    private String url;

    public CRHgMaterial()
    {
        type = TYPE_NAME;
    }

    public CRHgMaterial(String materialName, String folder, boolean autoUpdate,String url, boolean whitelist,String... filters) {
        super(TYPE_NAME, materialName, folder, autoUpdate,whitelist, filters);
        this.url = url;
    }

    public CRHgMaterial(String name, String folder, boolean autoUpdate,boolean whitelist, List<String> filter, String url) {
        super(TYPE_NAME, name, folder, autoUpdate, whitelist, filter);
        this.url = url;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        CRHgMaterial that = (CRHgMaterial)o;
        if(that == null)
            return  false;

        if(!super.equals(that))
            return false;

        if (url != null ? !url.equals(that.url) : that.url != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (url != null ? url.hashCode() : 0);
        return result;
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
        return String.format("%s; Hg material %s URL: %s",myLocation,name,url);
    }
}
