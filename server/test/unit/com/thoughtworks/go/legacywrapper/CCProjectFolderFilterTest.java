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

package com.thoughtworks.go.legacywrapper;

import junit.framework.TestCase;

import java.io.File;

import com.thoughtworks.go.helpers.FileSystemUtils;

public class CCProjectFolderFilterTest extends TestCase {
    public void testShouldRejectEmptyDirectory() {
        File project = FileSystemUtils.createDirectory("project");
        assertFalse(new CCProjectFolderFilter().accept(project));
    }

    public void testShouldAcceptDirectoryContainsAtLeastOneCXmlFile() throws Exception {
        File project = FileSystemUtils.createDirectory("project");
        FileSystemUtils.createFile("log12340505121212.xml", project);
        FileSystemUtils.createFile("readme.txt", project);
        assertTrue(new CCProjectFolderFilter().accept(project));
    }

    public void testShouldRejectDirectoryContainsNonCCLogFile() throws Exception {
        File project = FileSystemUtils.createDirectory("project2");
        FileSystemUtils.createFile("readme.txt", project);
        assertFalse(new CCProjectFolderFilter().accept(project));
    }
}