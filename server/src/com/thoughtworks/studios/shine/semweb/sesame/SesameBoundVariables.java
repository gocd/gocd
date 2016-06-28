/*************************GO-LICENSE-START*********************************
 * Copyright 2014 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *************************GO-LICENSE-END***********************************/

package com.thoughtworks.studios.shine.semweb.sesame;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.thoughtworks.studios.shine.semweb.BoundVariables;
import com.thoughtworks.studios.shine.semweb.URIReference;
import com.thoughtworks.studios.shine.util.ArgumentUtil;
import org.openrdf.model.Literal;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.query.BindingSet;

public class SesameBoundVariables implements BoundVariables, Serializable {

    private Map<String, Value> bindings = new LinkedHashMap<>();

    SesameBoundVariables(List<String> bindingNames, BindingSet bindingSet) {

        for (String bindingName : bindingNames) {
            bindings.put(bindingName, bindingSet.getValue(bindingName));
        }
    }

    public String getString(String boundName) {
        org.openrdf.model.Value value = getValue(boundName);
        return value == null ? null : value.stringValue();
    }

    public Integer getInt(String boundName) {
        org.openrdf.model.Value value = getValue(boundName);
        return value == null ? null : ((Literal) value).intValue();
    }

    public Boolean getBoolean(String boundName) {
        org.openrdf.model.Value value = getValue(boundName);
        return value == null ? null : ((Literal) value).booleanValue();
    }

    public List<String> getBoundVariableNames() {
        return new LinkedList<>(bindings.keySet());
    }

    public URIReference getURIReference(String boundName) {
        org.openrdf.model.Value value = getValue(boundName);
        return value == null ? null : new SesameURIReference((URI) value);
    }

    private org.openrdf.model.Value getValue(String boundName) {
        ArgumentUtil.guaranteeInList(boundName, bindings.keySet(), "boundName");
        return bindings.get(boundName);
    }

    public String getAsString(String boundName) {
        return getString(boundName);
    }

    public String toString() {
        String value = "\n{";
        for (String boundName : getBoundVariableNames()) {
            value += "\n\t" + boundName;
            value += " => ";
            value += getAsString(boundName);
        }
        value += "\n}";

        return value;
    }
}
