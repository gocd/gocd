/*
 * Copyright 2017 ThoughtWorks, Inc.
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
package com.thoughtworks.go.config.materials.mercurial;

import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.domain.materials.TestSubprocessExecutionContext;
import com.thoughtworks.go.domain.materials.mercurial.StringRevision;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class HgMaterialDockerTest {
    private File workingFolder;
    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();


    @Before
    public void setup() throws IOException {
        workingFolder = temporaryFolder.newFolder("workingFolder");
    }

    @Test
    public void shouldRefreshWorkingDirectoryIfUsernameInUrlChanges() throws Exception {
        HgMaterial material = new HgMaterial("http://user1:password@localhost:9999", null);
        final List<Modification> modifications = material.latestModification(workingFolder, new TestSubprocessExecutionContext());
        final File unversionedFile = new File(workingFolder, "unversioned.txt");
        FileUtils.writeStringToFile(unversionedFile, "something", UTF_8);
        assertTrue(unversionedFile.exists());

        material = new HgMaterial("http://user2:password@localhost:9999", null);
        material.modificationsSince(workingFolder, new StringRevision(modifications.get(0).getRevision()), new TestSubprocessExecutionContext());
        assertFalse(unversionedFile.exists());
    }

    @Test
    public void shouldNotRefreshWorkingDirectoryIfPasswordIsNotSetInHgrcFileButIsAvailableInMaterialUrl() throws Exception {
        HgMaterial material = new HgMaterial("http://user1:password@localhost:9999", null);
        final List<Modification> modifications = material.latestModification(workingFolder, new TestSubprocessExecutionContext());
        final File unversionedFile = new File(workingFolder, "unversioned.txt");
        FileUtils.writeStringToFile(unversionedFile, "something", UTF_8);
        assertTrue(unversionedFile.exists());

        material.modificationsSince(workingFolder, new StringRevision(modifications.get(0).getRevision()), new TestSubprocessExecutionContext());
        assertTrue(unversionedFile.exists());
    }
}
