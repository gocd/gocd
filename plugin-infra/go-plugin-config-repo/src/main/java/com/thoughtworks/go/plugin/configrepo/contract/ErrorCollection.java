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
package com.thoughtworks.go.plugin.configrepo.contract;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.*;

public class ErrorCollection {
    // key is location (file path or object description); values are errors detected
    @SerializedName("errors")
    @Expose
    private Map<String, List<String>> errors = new HashMap<>();

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        if (this.isEmpty())
            builder.append("No errors");
        else {
            builder.append(this.getErrorCount());
            builder.append(" errors in partial configuration");
        }
        return builder.toString();
    }

    public boolean isEmpty() {
        return errors.isEmpty();
    }

    public int getErrorCount() {
        int count = 0;
        for (List<String> entry : this.errors.values()) {
            count += entry.size();
        }
        return count;
    }

    public String yamlFormat() {
        StringBuilder sb = new StringBuilder();
        Iterator<Map.Entry<String, List<String>>> it = this.errors.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, List<String>> entry = it.next();
            sb.append(entry.getKey()).append(':');
            for (String message : entry.getValue()) {
                sb.append('\n').append("  ").append('-').append(' ').append(message);
            }
            if (it.hasNext()) {
                sb.append("\n\n");
            }
        }
        return sb.toString();
    }

    public String getErrorsAsText() {
        StringBuilder errorsBuilder = new StringBuilder();
        for (Map.Entry<String, List<String>> entry : this.errors.entrySet()) {
            errorsBuilder.append('\n');
            errorsBuilder.append(entry.getKey()).append(';');

            for (int i = 1; i <= entry.getValue().size(); i++) {
                errorsBuilder.append('\n').append(i).append(". ").append(entry.getValue().get(i - 1));
            }
            errorsBuilder.append("\n");
        }
        return errorsBuilder.toString();
    }

    public List<String> getOrCreateErrorList(String location) {
        if (!errors.containsKey(location))
            errors.put(location, new ArrayList<>());

        return errors.get(location);
    }

    public void checkMissing(String location, String fieldName, Object value) {
        if (value == null) {
            List<String> list = getOrCreateErrorList(location);
            list.add(String.format("Missing field '%s'.", fieldName));
        }
    }

    public void addError(String location, String error) {
        List<String> list = getOrCreateErrorList(location);
        list.add(error);
    }

    public void addErrors(List<CRError> pluginErrors) {
        for (CRError error : pluginErrors) {
            this.addError(error.getLocation(), error.getMessage());
        }
    }
}
