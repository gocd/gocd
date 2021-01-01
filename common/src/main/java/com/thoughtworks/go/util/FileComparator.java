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
import java.util.Comparator;

//TODO: ChrisS : Make static and rename to FILENAME_ORDER
public class FileComparator implements Comparator<File> {

    @Override
    public int compare(File file1, File file2) {
        if (file1.isDirectory() && file2.isDirectory() || file1.isFile() && file2.isFile()) {
            return file1.getName().compareTo(file2.getName());
        } else {
            return file1.isDirectory() ? -1 : 1;
        }
    }
}
