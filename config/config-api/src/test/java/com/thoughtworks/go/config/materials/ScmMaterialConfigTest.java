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
package com.thoughtworks.go.config.materials;

import com.thoughtworks.go.config.ConfigSaveValidationContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;

import static com.thoughtworks.go.config.materials.ScmMaterialConfig.*;
import static org.assertj.core.api.Assertions.assertThat;

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
        assertThat(material.filter()).isEqualTo(new Filter());
        assertThat(material.getFilterAsString()).isEqualTo("");
    }

    @Test
    void shouldReturnFilterForDisplay() {
        material.setFilter(new Filter(new IgnoredFiles("/foo/**.*"), new IgnoredFiles("/another/**.*"), new IgnoredFiles("bar")));
        assertThat(material.getFilterAsString()).isEqualTo("/foo/**.*,/another/**.*,bar");
    }

    @Test
    void shouldSetFolderToNullWhenBlank() {
        material.setConfigAttributes(Collections.singletonMap(FOLDER, "foo"));
        assertThat(material.getFolder()).isNotNull();

        material.setConfigAttributes(Collections.singletonMap(FOLDER, ""));
        assertThat(material.getFolder()).isNull();
    }

    @Test
    void shouldUpdateAutoUpdateFieldFromConfigAttributes() {
        material.setConfigAttributes(Collections.singletonMap(AUTO_UPDATE, "false"));
        assertThat(material.isAutoUpdate()).isFalse();
        material.setConfigAttributes(Collections.singletonMap(AUTO_UPDATE, null));
        assertThat(material.isAutoUpdate()).isFalse();
        material.setConfigAttributes(Collections.singletonMap(AUTO_UPDATE, "true"));
        assertThat(material.isAutoUpdate()).isTrue();
        material.setConfigAttributes(new HashMap());
        assertThat(material.isAutoUpdate()).isFalse();
        material.setConfigAttributes(Collections.singletonMap(AUTO_UPDATE, null));
        assertThat(material.isAutoUpdate()).isFalse();
        material.setConfigAttributes(Collections.singletonMap(AUTO_UPDATE, "random-stuff"));
        assertThat(material.isAutoUpdate()).isFalse();
    }

    @Nested
    class validate {
        @Test
        void shouldNotValidateEmptyDestinationFolder() {
            material.setConfigAttributes(Collections.singletonMap(FOLDER, ""));
            material.validate(new ConfigSaveValidationContext(null));
            assertThat(material.errors.isEmpty()).isTrue();
        }

        @Test
        void shouldFailValidationIfDestinationDirectoryIsNested() {
            material.setFolder("f1");
            material.validateNotSubdirectoryOf("f1/f2");
            assertThat(material.errors().isEmpty()).isFalse();
            assertThat(material.errors().on(FOLDER)).isEqualTo("Invalid Destination Directory. Every material needs a different destination directory and the directories should not be nested.");
        }

        @Test
        void shouldNotFailValidationIfDestinationDirectoryIsMultilevelButNotNested() {
            material.setFolder("f1/f2/f3");
            material.validateNotSubdirectoryOf("f1/f2/f");

            assertThat(material.errors().getAllOn(FOLDER)).isEmpty();
        }

        @Test
        void shouldFailValidationIfDestinationDirectoryIsOutsideCurrentWorkingDirectoryAfterNormalization() {
            material.setFolder("f1/../../f3");

            material.validateConcreteMaterial(null);
            assertThat(material.errors().on(FOLDER)).isEqualTo("Dest folder 'f1/../../f3' is not valid. It must be a sub-directory of the working folder.");
        }

        @Test
        void shouldFailValidationIfDestinationDirectoryIsNestedAfterNormalization() {
            material.setFolder("f1/f2/../../f3");
            material.validateNotSubdirectoryOf("f3/f4");
            assertThat(material.errors().on(FOLDER)).isEqualTo("Invalid Destination Directory. Every material needs a different destination directory and the directories should not be nested.");
        }

        @Test
        void shouldNotValidateNestingOfMaterialDirectoriesBasedOnServerSideFileSystem(@TempDir File workingDir) throws IOException {
            final File material1 = new File(workingDir, "material1");
            material1.mkdirs();

            final Path material2 = Files.createSymbolicLink(Paths.get(new File(workingDir, "material2").getPath()), Paths.get(material1.getPath()));

            material.setFolder(material1.getAbsolutePath());
            material.validateNotSubdirectoryOf(material2.toAbsolutePath().toString());

            assertThat(material.errors().getAllOn(FOLDER)).isEmpty();
        }
    }
}
