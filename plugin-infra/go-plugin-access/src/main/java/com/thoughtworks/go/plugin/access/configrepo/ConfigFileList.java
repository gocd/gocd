/*
 * Copyright 2021 ThoughtWorks, Inc.
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
package com.thoughtworks.go.plugin.access.configrepo;

import com.thoughtworks.go.plugin.configrepo.contract.ErrorCollection;

import java.util.ArrayList;
import java.util.List;

public class ConfigFileList {
    private List<String> files;
    private ErrorCollection errors;

    public ConfigFileList(List<String> files, ErrorCollection errors) {
        this.files = files;
        this.errors = errors;
    }

    public List<String> getFiles() {
        return files;
    }

    public void setFiles(List<String> files) {
        this.files = files;
    }

    public static ConfigFileList withError(String location, String err) {
        ErrorCollection errs = new ErrorCollection();
        errs.addError(location, err);
        return new ConfigFileList(new ArrayList<>(), errs);
    }

    public static ConfigFileList from(List<String> files) {
        ErrorCollection errs = new ErrorCollection();
       if (files == null) {
           errs.addError("Plugin response message",
                   "The plugin returned a response that indicates that it doesn't correctly implement this endpoint");

       }
      return new ConfigFileList(files, errs);
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    public ErrorCollection getErrors() {
        return errors;
    }
}
