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

public class CRConfigMaterial extends CRMaterial {
    public static final String TYPE_NAME = "configrepo";

    private CRFilter filter;
    private String destination;

    public CRConfigMaterial() {
        type = TYPE_NAME;
    }
    public CRConfigMaterial(String name, String destination,CRFilter filter) {
        super(TYPE_NAME,name);
        this.destination = destination;
        this.filter = filter;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        CRConfigMaterial that = (CRConfigMaterial)o;
        if(that == null)
            return  false;

        if(!super.equals(that))
            return false;

        if (destination != null ? !destination.equals(that.destination) : that.destination != null) {
            return false;
        }
        if (filter != null ? !filter.equals(that.filter) : that.filter != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (destination != null ? destination.hashCode() : 0);
        result = 31 * result + (filter != null ? filter.hashCode() : 0);
        return result;
    }

    @Override
    public String typeName() {
        return TYPE_NAME;
    }

    @Override
    public void getErrors(ErrorCollection errors, String parentLocation) {
        String location = getLocation(parentLocation);
        if(this.filter != null)
            this.filter.getErrors(errors,location);
    }

    @Override
    public String getLocation(String parent) {
        String myLocation = getLocation() == null ? parent : getLocation();
        String name = getName() == null ? "" : getName();
        return String.format("%s; Config material %s",myLocation,name);
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public void setFilter(CRFilter filter) {
        this.filter = filter;
    }

    public CRFilter getFilter() {
        return filter;
    }
}