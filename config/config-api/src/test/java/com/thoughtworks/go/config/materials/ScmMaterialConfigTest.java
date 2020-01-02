/*
 * Copyright 2020 ThoughtWorks, Inc.
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
package com.thoughtworks.go.config.materials;

import com.thoughtworks.go.config.ConfigSaveValidationContext;
import org.junit.Rule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.migrationsupport.rules.EnableRuleMigrationSupport;
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
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

class ScmMaterialConfigTest {
    private DummyMaterialConfig material;

    @BeforeEach
    void setUp() {
        material = new DummyMaterialConfig();
    }

    @Test
    void shouldSetFilterToNullWhenBlank() {
        material.setFilter(new Filter(new IgnoredFiles("*.*")));
        material.setConfigAttributes(Collections.singletonMap(ScmMaterialConfig.FILTER, ""));
        assertThat(material.filter(), is(new Filter()));
        assertThat(material.getFilterAsString(), is(""));
    }

    @Test
    void shouldReturnFilterForDisplay() {
        material.setFilter(new Filter(new IgnoredFiles("/foo/**.*"), new IgnoredFiles("/another/**.*"), new IgnoredFiles("bar")));
        assertThat(material.getFilterAsString(), is("/foo/**.*,/another/**.*,bar"));
    }

    @Test
    void shouldSetFolderToNullWhenBlank() {
        material.setConfigAttributes(Collections.singletonMap(FOLDER, "foo"));
        assertThat(material.getFolder(), is(not(nullValue())));

        material.setConfigAttributes(Collections.singletonMap(FOLDER, ""));
        assertThat(material.getFolder(), is(nullValue()));
    }

    @Test
    void shouldUpdateAutoUpdateFieldFromConfigAttributes() {
        material.setConfigAttributes(Collections.singletonMap(AUTO_UPDATE, "false"));
        assertThat(material.isAutoUpdate(), is(false));
        material.setConfigAttributes(Collections.singletonMap(AUTO_UPDATE, null));
        assertThat(material.isAutoUpdate(), is(false));
        material.setConfigAttributes(Collections.singletonMap(AUTO_UPDATE, "true"));
        assertThat(material.isAutoUpdate(), is(true));
        material.setConfigAttributes(new HashMap());
        assertThat(material.isAutoUpdate(), is(false));
        material.setConfigAttributes(Collections.singletonMap(AUTO_UPDATE, null));
        assertThat(material.isAutoUpdate(), is(false));
        material.setConfigAttributes(Collections.singletonMap(AUTO_UPDATE, "random-stuff"));
        assertThat(material.isAutoUpdate(), is(false));
    }

    @Nested
    @EnableRuleMigrationSupport
    class validate {
        @Rule
        public final TemporaryFolder temporaryFolder = new TemporaryFolder();

        @Test
        void shouldNotValidateEmptyDestinationFolder() {
            material.setConfigAttributes(Collections.singletonMap(FOLDER, ""));
            material.validate(new ConfigSaveValidationContext(null));
            assertThat(material.errors.isEmpty(), is(true));
        }

        @Test
        void shouldFailValidationIfDestinationDirectoryIsNested() {
            material.setFolder("f1");
            material.validateNotSubdirectoryOf("f1/f2");
            assertFalse(material.errors().isEmpty());
            assertThat(material.errors().on(FOLDER), is("Invalid Destination Directory. Every material needs a different destination directory and the directories should not be nested."));
        }

        @Test
        void shouldNotFailValidationIfDestinationDirectoryIsMultilevelButNotNested() {
            material.setFolder("f1/f2/f3");
            material.validateNotSubdirectoryOf("f1/f2/f");

            assertNull(material.errors().getAllOn(FOLDER));
        }

        @Test
        void shouldFailValidationIfDestinationDirectoryIsOutsideCurrentWorkingDirectoryAfterNormalization() {
            material.setFolder("f1/../../f3");

            material.validateConcreteMaterial(null);
            assertThat(material.errors().on(FOLDER), is("Dest folder 'f1/../../f3' is not valid. It must be a sub-directory of the working folder."));
        }

        @Test
        void shouldFailValidationIfDestinationDirectoryIsNestedAfterNormalization() {
            material.setFolder("f1/f2/../../f3");
            material.validateNotSubdirectoryOf("f3/f4");
            assertThat(material.errors().on(FOLDER), is("Invalid Destination Directory. Every material needs a different destination directory and the directories should not be nested."));
        }

        @Test
        void shouldNotValidateNestingOfMaterialDirectoriesBasedOnServerSideFileSystem() throws IOException {
            final File workingDir = temporaryFolder.newFolder("go-working-dir");
            final File material1 = new File(workingDir, "material1");
            material1.mkdirs();

            final Path material2 = Files.createSymbolicLink(Paths.get(new File(workingDir, "material2").getPath()), Paths.get(material1.getPath()));

            material.setFolder(material1.getAbsolutePath());
            material.validateNotSubdirectoryOf(material2.toAbsolutePath().toString());

            assertNull(material.errors().getAllOn(FOLDER));
        }
    }
}
