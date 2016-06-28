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

package com.thoughtworks.go.legacywrapper;

import java.io.File;
import java.io.FilenameFilter;

class And implements FilenameFilter {

    private final FilenameFilter[] filters;

    And(FilenameFilter[] filters) {
        this.filters = filters;
    }

    public boolean accept(File dir, String name) {
        for (FilenameFilter filter : filters) {
            if (!filter.accept(dir, name)) {
                return false;
            }
        }
        return true;
    }
}

class Not implements FilenameFilter {

    private final FilenameFilter filter;

    Not(FilenameFilter filter) {
        this.filter = filter;
    }

    public boolean accept(File dir, String name) {
        return !filter.accept(dir, name);
    }
}