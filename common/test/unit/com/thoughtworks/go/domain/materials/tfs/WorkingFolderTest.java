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

package com.thoughtworks.go.domain.materials.tfs;

import com.thoughtworks.go.util.TempFiles;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class WorkingFolderTest {
    private TempFiles tempFiles;

    @Before
    public void setUp() throws Exception {
        tempFiles = new TempFiles();
    }

    @After
    public void tearDown() throws Exception {
        tempFiles.cleanUp();
    }

    @Test
    public void shouldAlwaysMatchLocalDirToTheCanonicalPathOfTheGivenFile() throws Exception {
        File parent = tempFiles.createUniqueFolder("parent");
        File child = new File(parent.getAbsolutePath() + "/child");
        child.mkdir();
        File grandchild = new File(child.getAbsolutePath() + "/grandchild");
        grandchild.mkdir();

        WorkingFolder workingFolder = new WorkingFolder("$/helloworld", grandchild.getCanonicalPath());
        File grandChild = new File(child.getAbsolutePath() + "/../child/grandchild");
        assertThat("Working folder (" + workingFolder + ") does not match: " + grandChild.getCanonicalPath(), workingFolder.matchesLocalDir(grandChild), is(true));
    }
}
