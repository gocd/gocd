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
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public abstract class CRMaterial extends CRBase {
    @SerializedName("name")
    @Expose
    private String name;
    @SerializedName("type")
    @Expose
    protected String type;

    public CRMaterial() {
    }

    public CRMaterial(String type, String name) {
        this.type = type;
        this.name = name;
    }

    public abstract String typeName();

    public String validateNameUniqueness(HashSet<String> keys) {
        if (this.getName() == null)
            return "Material has no name when there many pipeline materials";
        else if (keys.contains(this.getName()))
            return String.format("Material named %s is defined more than once", this.getName());
        else
            keys.add(this.getName());
        return null;
    }
}