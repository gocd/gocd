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

package com.thoughtworks.go.junitext;

import java.util.ArrayList;
import java.util.Iterator;

import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.resources.FileResource;

public class FilterFileSet extends FileSet {
    private Iterator<FileResource> iterator;

    public FilterFileSet() {
    }

    FilterFileSet(Iterator<FileResource> iterator) {
        this.iterator = iterator;
    }

    public Iterator<FileResource> iterator() {
        boolean started = false;
        ArrayList<FileResource> list = new ArrayList<FileResource>();

        Iterator<FileResource> iterator = getIterator();
        String skipTo = getProject().getProperty("skipTo");

        while (iterator.hasNext()) {
            FileResource file = iterator.next();
            if (file.getFile().getName().equals(skipTo + ".java")) {
                started = true;
            }
            if (started) {
                list.add(file);
            }
        }
        return list.iterator();
    }

    private Iterator<FileResource> getIterator() {
        if (iterator == null) {
            return super.iterator();
        }
        return iterator;
    }
}
