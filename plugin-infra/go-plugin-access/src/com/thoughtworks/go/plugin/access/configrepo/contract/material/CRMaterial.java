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

import com.thoughtworks.go.plugin.access.configrepo.contract.CRBase;

import java.util.HashSet;

public abstract class CRMaterial extends CRBase {
    private String name;
    protected String type;

    public CRMaterial() {
    }

    public CRMaterial(String name) {
        this.name = name;
    }
    public CRMaterial(String type,String name) {
        this.type = type;
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        CRMaterial that = (CRMaterial) o;

        if (name != null ? !name.equals(that.name) : that.name != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        return result;
    }


    public abstract String typeName();

    public String validateNameUniqueness(HashSet<String> keys) {
        if(this.getName() == null)
            return "Material has no name when there many pipeline materials";
        else if(keys.contains(this.getName()))
            return String.format("Material named %s is defined more than once",this.getName());
        else
            keys.add(this.getName());
        return null;
    }
}