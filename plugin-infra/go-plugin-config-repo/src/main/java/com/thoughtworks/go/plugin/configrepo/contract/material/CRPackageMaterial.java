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

public class CRPackageMaterial extends CRMaterial {
    public static final String TYPE_NAME = "package";

    private String package_id;

    public CRPackageMaterial() {
        type = TYPE_NAME;
    }
    public CRPackageMaterial(String packageId)
    {
        type = TYPE_NAME;
        this.package_id = packageId;
    }
    public CRPackageMaterial(String material,String packageId)
    {
        super(TYPE_NAME,material);
        this.package_id = packageId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CRPackageMaterial that = (CRPackageMaterial) o;
        if(!super.equals(that))
            return false;

        if (package_id != null ? !package_id.equals(that.package_id) : that.package_id != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (package_id != null ? package_id.hashCode() : 0);
        return result;
    }

    @Override
    public String typeName() {
        return TYPE_NAME;
    }


    public String getPackageId() {
        return package_id;
    }

    public void setPackageId(String packageId) {
        this.package_id = packageId;
    }

    @Override
    public void getErrors(ErrorCollection errors, String parentLocation) {
        String location = getLocation(parentLocation);
        errors.checkMissing(location,"package_id",package_id);
    }

    @Override
    public String getLocation(String parent) {
        String myLocation = getLocation() == null ? parent : getLocation();
        String name = getName() == null ? "" : getName();
        String url = getPackageId() != null ? getPackageId() : "unknown";
        return String.format("%s; Package material %s ID: %s",myLocation,name,url);
    }
}