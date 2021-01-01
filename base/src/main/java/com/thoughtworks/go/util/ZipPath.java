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
package com.thoughtworks.go.util;

import java.io.File;
import java.util.zip.ZipEntry;

public class ZipPath {
    private final String path;

    ZipPath() {
        this("");
    }

    ZipPath(String path) {
        this.path = path;
    }

    ZipPath(ZipPath old, File file) {
        String prefix = old.path.equals("") ? "" : old.path + "/";
        this.path = prefix + file.getName();
    }

    public ZipPath with(File file) {
        return new ZipPath(this, file);
    }

    public ZipEntry asZipEntry() {
        return new ZipEntry(path);
    }

    public ZipEntry asZipEntryDirectory() {
        return new ZipEntry(path + "/");
    }
}
