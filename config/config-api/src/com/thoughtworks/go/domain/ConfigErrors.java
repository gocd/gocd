/*
 * Copyright 2016 ThoughtWorks, Inc.
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
 */

package com.thoughtworks.go.domain;

import com.thoughtworks.go.util.ListUtil;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConfigErrors extends HashMap<String, List<String>> implements Serializable {
    public void add(String fieldName, String msg) {
        List<String> msgList = get(fieldName);
        if (msgList == null) {
            msgList = new ArrayList<>();
            put(fieldName, msgList);
        }
        if (!msgList.contains(msg)) {
            msgList.add(msg);
        }
    }

    public List<String> getAll() {
        ArrayList<String> allErrors = new ArrayList<>();
        for (List<String> errorOnAnAttribute : values()) {
            allErrors.addAll(errorOnAnAttribute);
        }
        return allErrors;
    }

    public List<String> getAllOn(String fieldName) {
        return get(fieldName);
    }

    /**
     * This is for Rails form helpers
     */
    public String on(String fieldName) {
        return getFirstOn(fieldName);
    }

    private String getFirstOn(String fieldName) {
        List<String> errors = get(fieldName);
        if (errors != null && !errors.isEmpty()) {
            return errors.get(0);
        }
        return null;
    }

    public String firstError() {
        for (Map.Entry<String, List<String>> fieldToErrors : entrySet()) {
            return getFirstOn(fieldToErrors.getKey());
        }
        return null;
    }

    public void addAll(ConfigErrors configErrors) {
        for (String fieldName : configErrors.keySet()) {
            for (String value : configErrors.get(fieldName)) {
                this.add(fieldName, value);
            }
        }
    }

    public String asString(){
        return ListUtil.join(this.getAll());
    }
}
