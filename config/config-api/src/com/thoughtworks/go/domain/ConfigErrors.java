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

package com.thoughtworks.go.domain;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConfigErrors implements Serializable {
    private Map<String, List<String>> fieldErrors = new HashMap<String, List<String>>();

    public void add(String fieldName, String msg) {
        List<String> msgList = fieldErrors.get(fieldName);
        if (msgList == null) {
            msgList = new ArrayList<String>();
            fieldErrors.put(fieldName, msgList);
        }
        if (!msgList.contains(msg)) {
            msgList.add(msg);
        }
    }

    public List<String> getAll() {
        ArrayList<String> allErrors = new ArrayList<String>();
        for (List<String> errorOnAnAttribute : fieldErrors.values()) {
            allErrors.addAll(errorOnAnAttribute);
        }
        return allErrors;
    }

    public List<String> getAllOn(String fieldName) {
        return fieldErrors.get(fieldName);
    }

    /**
     * This is for Rails form helpers
     */
    public String on(String fieldName) {
        return getFirstOn(fieldName);
    }

    private String getFirstOn(String fieldName) {
        List<String> errors = fieldErrors.get(fieldName);
        if (errors != null && !errors.isEmpty()) {
            return errors.get(0);
        }
        return null;
    }

    public boolean isEmpty() {
        return fieldErrors.isEmpty();
    }

    public String firstError() {
        for (Map.Entry<String, List<String>> fieldToErrors : fieldErrors.entrySet()) {
            return getFirstOn(fieldToErrors.getKey());
        }
        return null;
    }

    @Override
    public String toString() {
        return "ConfigErrors{" +
                "fieldErrors=" + fieldErrors +
                '}';
    }

    public void addAll(ConfigErrors configErrors) {
        for (String fieldName : configErrors.fieldErrors.keySet()) {
            for (String value : configErrors.fieldErrors.get(fieldName)) {
                this.add(fieldName, value);
            }
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ConfigErrors that = (ConfigErrors) o;

        if (fieldErrors != null ? !fieldErrors.equals(that.fieldErrors) : that.fieldErrors != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return fieldErrors != null ? fieldErrors.hashCode() : 0;
    }

}
