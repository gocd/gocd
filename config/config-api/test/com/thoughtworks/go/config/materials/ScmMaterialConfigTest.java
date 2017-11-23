/*
 * Copyright 2015 ThoughtWorks, Inc.
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

package com.thoughtworks.go.config.materials;

import com.thoughtworks.go.config.ConfigSaveValidationContext;
import org.apache.commons.collections.map.SingletonMap;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;

import static com.thoughtworks.go.config.materials.ScmMaterialConfig.AUTO_UPDATE;
import static com.thoughtworks.go.config.materials.ScmMaterialConfig.FOLDER;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.*;

public class ScmMaterialConfigTest {
    private DummyMaterialConfig material;
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Before
    public void setUp() throws Exception {
        material = new DummyMaterialConfig();
    }

    @Test
    public void shouldSetFilterToNullWhenBlank() {
        material.setFilter(new Filter(new IgnoredFiles("*.*")));
        material.setConfigAttributes(new SingletonMap(ScmMaterialConfig.FILTER, ""));
        assertThat(material.filter(), is(new Filter()));
        assertThat(material.getFilterAsString(), is(""));
    }

    @Test
    public void shouldReturnFilterForDisplay() {
        material.setFilter(new Filter(new IgnoredFiles("/foo/**.*"), new IgnoredFiles("/another/**.*"), new IgnoredFiles("bar")));
        assertThat(material.getFilterAsString(), is("/foo/**.*,/another/**.*,bar"));
    }

    @Test
    public void shouldNotValidateEmptyDestinationFolder() {
        material.setConfigAttributes(Collections.singletonMap(FOLDER, ""));
        material.validate(new ConfigSaveValidationContext(null));
        assertThat(material.errors.isEmpty(), is(true));
    }

    @Test
    public void shouldSetFolderToNullWhenBlank() {
        material.setConfigAttributes(Collections.singletonMap(FOLDER, "foo"));
        assertThat(material.getFolder(), is(not(nullValue())));

        material.setConfigAttributes(new SingletonMap(FOLDER, ""));
        assertThat(material.getFolder(), is(nullValue()));
    }

    @Test
    public void shouldUpdateAutoUpdateFieldFromConfigAttributes() {
        material.setConfigAttributes(new SingletonMap(AUTO_UPDATE, "false"));
        assertThat(material.isAutoUpdate(), is(false));
        material.setConfigAttributes(new SingletonMap(AUTO_UPDATE, null));
        assertThat(material.isAutoUpdate(), is(false));
        material.setConfigAttributes(new SingletonMap(AUTO_UPDATE, "true"));
        assertThat(material.isAutoUpdate(), is(true));
        material.setConfigAttributes(new HashMap());
        assertThat(material.isAutoUpdate(), is(false));
        material.setConfigAttributes(new SingletonMap(AUTO_UPDATE, null));
        assertThat(material.isAutoUpdate(), is(false));
        material.setConfigAttributes(new SingletonMap(AUTO_UPDATE, "random-stuff"));
        assertThat(material.isAutoUpdate(), is(false));
    }

    @Test
    public void shouldFailValidationIfDestinationDirectoryIsNested() {
        material.setFolder("f1");
        material.validateNotSubdirectoryOf("f1/f2");
        assertFalse(material.errors().isEmpty());
        assertThat(material.errors().on(FOLDER), is("Invalid Destination Directory. Every material needs a different destination directory and the directories should not be nested."));
    }

    @Test
    public void shouldNotFailValidationIfDestinationDirectoryIsMultilevelButNotNested() {
        material.setFolder("f1/f2/f3");
        material.validateNotSubdirectoryOf("f1/f2/f");

        assertNull(material.errors().getAllOn(FOLDER));
    }

    @Test
    public void shouldFailValidationIfDestinationDirectoryIsOutsideCurrentWorkingDirectoryAfterNormalization() {
        material.setFolder("f1/../../f3");

        material.validateConcreteMaterial(null);
        assertThat(material.errors().on(FOLDER), is("Dest folder 'f1/../../f3' is not valid. It must be a sub-directory of the working folder."));
    }

    @Test
    public void shouldFailValidationIfDestinationDirectoryIsNestedAfterNormalization() {
        material.setFolder("f1/f2/../../f3");
        material.validateNotSubdirectoryOf("f3/f4");
        assertThat(material.errors().on(FOLDER), is("Invalid Destination Directory. Every material needs a different destination directory and the directories should not be nested."));
    }

    @Test
    public void shouldNotValidateNestingOfMaterialDirectoriesBasedOnServerSideFileSystem() throws IOException {
        final File workingDir = temporaryFolder.newFolder("go-working-dir");
        final File material1 = new File(workingDir, "material1");
        material1.mkdirs();

        final Path material2 = Files.createSymbolicLink(Paths.get(new File(workingDir, "material2").getPath()), Paths.get(material1.getPath()));

        material.setFolder(material1.getAbsolutePath());
        material.validateNotSubdirectoryOf(material2.toAbsolutePath().toString());

        assertNull(material.errors().getAllOn(FOLDER));
    }
}
