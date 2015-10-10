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

package com.thoughtworks.go.domain.materials;

import java.io.File;
import java.io.IOException;

import com.thoughtworks.go.util.FileUtil;
import com.thoughtworks.go.util.TestFileUtil;
import com.thoughtworks.go.util.command.InMemoryStreamConsumer;
import com.thoughtworks.go.util.command.ProcessOutputStreamConsumer;
import static org.hamcrest.Matchers.is;
import org.junit.After;
import org.junit.Assert;
import static org.junit.Assert.assertThat;
import org.junit.Before;
import org.junit.Test;
import static org.hamcrest.CoreMatchers.containsString;

public class DirectoryCleanerTest {
    private File baseFolder;
    private DirectoryCleaner cleaner;
    private InMemoryStreamConsumer consumer;

    @Before
    public void createBaseDirectory() {
        consumer = ProcessOutputStreamConsumer.inMemoryConsumer();
        baseFolder = TestFileUtil.createTempFolder("directoryCleaner");
        cleaner = new DirectoryCleaner(baseFolder, consumer);
    }

    @After
    public void removeBaseDirectory() {
        FileUtil.deleteFolder(baseFolder);
    }

    @Test
    public void shouldDoNothingIfDirectoryIsEmpty() {
        cleaner.allowed("non-existent");
        cleaner.clean();

        assertThat(baseFolder.exists(), is(true));
    }

    @Test
    public void shouldNotCleanSvnDestIfExternalIsEnabled() {
        File svnDest = new File(baseFolder, "test1");
        File shouldExist = new File(svnDest, "shouldExist");
        shouldExist.mkdirs();

        File svnExternal = new File(baseFolder, "test1/external");
        svnExternal.mkdirs();

        cleaner.allowed("test1", "test1/subdir");
        cleaner.clean();

        assertThat(svnDest.exists(), is(true));
        assertThat(shouldExist.exists(), is(true));
    }

    @Test
    public void shouldKeepMaterialFolderIfItContainsOtherMaterials() {
        File material1 = mkdirDir(baseFolder, "material1");
        File dirOfMaterial1 = mkdirDir(material1, "dirOfMaterial1");
        File material2 = mkdirDir(material1, "material2");
        File oldMaterial3 = mkdirDir(baseFolder, "oldMaterial3");

        cleaner.allowed("material1", "material1/material2");
        cleaner.clean();

        assertThat(material1.exists(), is(true));
        assertThat(dirOfMaterial1.exists(), is(true));
        assertThat(material2.exists(), is(true));
        assertThat(oldMaterial3.exists(), is(false));
    }

    private File mkdirDir(File root, String dir) {
        File directory = new File(root, dir);
        directory.mkdir();
        return directory;
    }

    @Test
    public void shouldRemoveExtraDirectoriesInRootFolder() {
        File notAllowed = new File(baseFolder, "notAllowed");
        notAllowed.mkdirs();

        cleaner.allowed("allowed");
        cleaner.clean();

        assertThat(baseFolder.exists(), is(true));
        assertThat(notAllowed.exists(), is(false));
    }

    @Test
    public void shouldNotRemoveAllowedDirectoriesInRootFolder() {
        File allowedFolder = new File(baseFolder, "allowed");
        allowedFolder.mkdir();

        cleaner.allowed("allowed");
        cleaner.clean();

        assertThat(baseFolder.exists(), is(true));
        assertThat(allowedFolder.exists(), is(true));
    }

    @Test
    public void shouldNotRemoveAllowedDirectoriesInSubfolder() {
        File allowedFolder = new File(baseFolder, "subfolder/allowed");
        allowedFolder.mkdirs();

        cleaner.allowed("subfolder/allowed");
        cleaner.clean();

        assertThat(baseFolder.exists(), is(true));
        assertThat(allowedFolder.getParentFile().exists(), is(true));
        assertThat(allowedFolder.exists(), is(true));
    }

    @Test
    public void shouldRemoveNotAllowedDirectoriesInSubfolder() {
        File allowedFolder = new File(baseFolder, "subfolder/allowed");
        allowedFolder.mkdirs();
        File notAllowedFolder = new File(baseFolder, "subfolder/notAllowed");
        notAllowedFolder.mkdirs();

        cleaner.allowed("subfolder/allowed");
        cleaner.clean();

        assertThat(baseFolder.exists(), is(true));
        assertThat(allowedFolder.getParentFile().exists(), is(true));
        assertThat(notAllowedFolder.exists(), is(false));
    }

    @Test
    public void shouldDoNothingIfSubdirectoryDoesNotExist() {
        File allowedFolder = new File(baseFolder, "subfolder/allowed");

        cleaner.allowed("subfolder/allowed");
        cleaner.clean();

        assertThat(baseFolder.exists(), is(true));
        assertThat(allowedFolder.exists(), is(false));
    }

    @Test
    public void shouldNotRemoveAnythingIfNoAllowedWasSet() {
        File allowedFolder = new File(baseFolder, "subfolder/allowed");
        allowedFolder.mkdirs();

        cleaner.clean();

        assertThat(baseFolder.exists(), is(true));
        assertThat(allowedFolder.exists(), is(true));
    }

    @Test
    public void shouldNotProcessFilesOutsideTheBaseFolder() {
        try {
            cleaner.allowed("/../..");
            Assert.fail("Should not allow file outside the baseDirectory");
        } catch (Exception e) {
            assertThat(
                    e.getMessage(),
                    containsString("Folder " + new File(baseFolder, "/../..").getAbsolutePath() + " is outside the base folder"));
        }
    }

    @Test
    public void shouldReportDeletingFiles() throws IOException {
        File allowedFolder = new File(baseFolder, "subfolder/allowed");
        allowedFolder.mkdirs();

        File notAllowedFolder = new File(baseFolder, "subfolder/notallowed");
        notAllowedFolder.mkdirs();

        cleaner.allowed("subfolder/allowed");
        cleaner.clean();

        assertThat(consumer.getStdOut(), containsString("Deleting folder " + notAllowedFolder.getPath()));
        assertThat(consumer.getStdOut(), containsString("Keeping folder " + allowedFolder.getPath()));
    }
}
