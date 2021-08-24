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
package com.thoughtworks.go.config.materials.perforce;

import com.thoughtworks.go.domain.materials.RevisionContext;
import com.thoughtworks.go.domain.materials.TestSubprocessExecutionContext;
import com.thoughtworks.go.domain.materials.ValidationBean;
import com.thoughtworks.go.domain.materials.mercurial.StringRevision;
import com.thoughtworks.go.domain.materials.perforce.PerforceFixture;
import com.thoughtworks.go.helper.P4TestRepo;
import com.thoughtworks.go.util.JsonValue;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.*;

import static com.thoughtworks.go.util.JsonUtils.from;
import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

public abstract class P4MaterialTestBase extends PerforceFixture {
    protected static final String VIEW = "//depot/... //something/...";

    @Test
    void shouldValidateCorrectConnection() {
        P4Material p4Material = p4Fixture.material(VIEW);
        p4Material.setPassword("secret");
        p4Material.setUseTickets(false);
        ValidationBean validation = p4Material.checkConnection(new TestSubprocessExecutionContext());
        assertThat(validation.isValid()).isTrue();
        assertThat(StringUtils.isBlank(validation.getError())).isTrue();
    }

    @Test
    void shouldBeAbleToGetUrlArgument() {
        P4Material p4Material = new P4Material("localhost:9876", "p4view");
        assertThat(p4Material.getUrlArgument().forDisplay()).isEqualTo("localhost:9876");
    }

    @Test
    void shouldCheckConnection() {
        P4Material p4Material = new P4Material("localhost:9876", "p4view");
        p4Material.setPassword("secret");
        p4Material.setUseTickets(false);
        ValidationBean validation = p4Material.checkConnection(new TestSubprocessExecutionContext());
        assertThat(validation.isValid()).isFalse();
        assertThat(validation.getError()).contains("Unable to connect to server localhost:9876");
        assertThat(validation.getError()).doesNotContain("secret");
    }

    @Test
    void shouldReplacePasswordUsingStar() {
        P4Material p4Material = new P4Material("localhost:9876", "p4view");
        p4Material.setPassword("secret");
        p4Material.setUseTickets(true);

        try {
            p4Material.latestModification(clientFolder, new TestSubprocessExecutionContext());
            fail("should throw exception because p4 server not exists.");
        } catch (Exception e) {
            assertThat(e.getMessage()).doesNotContain("secret");
        }
    }

    @Test
    void shouldUpdateToSpecificRevision() {
        P4Material p4Material = p4Fixture.material(VIEW);
        updateMaterial(p4Material, new StringRevision("2"));
        assertThat(clientFolder.listFiles()).hasSize(8);

        updateMaterial(p4Material, new StringRevision("3"));
        assertThat(clientFolder.listFiles()).hasSize(7);
    }

    @Test
    void shouldCleanOutRepoWhenMaterialChanges() throws Exception {
        P4TestRepo secondTestRepo = P4TestRepo.createP4TestRepo(tempDir, clientFolder);
        try {
            secondTestRepo.onSetup();

            P4Material p4Material = p4Fixture.material(VIEW);
            updateMaterial(p4Material, new StringRevision("2"));
            File.createTempFile("temp", "txt", clientFolder);
            assertThat(clientFolder.listFiles()).hasSize(9);

            P4Material otherMaterial = secondTestRepo.material("//depot/lib/... //something/...");
            otherMaterial.setUsername("cceuser1");
            updateMaterial(otherMaterial, new StringRevision("2"));
            File.createTempFile("temp", "txt", clientFolder);
            assertThat(clientFolder.listFiles())
                    .as("Should clean and re-checkout after p4repo changed")
                    .hasSize(3);
            otherMaterial.setUsername("cceuser");
            otherMaterial.setPassword("password");
            updateMaterial(otherMaterial, new StringRevision("2"));
            assertThat(clientFolder.listFiles())
                    .as("Should clean and re-checkout after user changed")
                    .hasSize(2);
            assertThat(outputconsumer.getStdOut()).contains("Working directory has changed. Deleting and re-creating it.");
        } finally {
            secondTestRepo.stop();
            secondTestRepo.tearDown();
        }
    }

    @Test
    void shouldMapDirectoryInRepoToClientRoot() {
        P4Material p4Material = p4Fixture.material("//depot/lib/... //cws/...");
        updateMaterial(p4Material, new StringRevision("2"));

        assertThat(getContainedFileNames(clientFolder)).contains("junit.jar");
    }

    private List<String> getContainedFileNames(File folder) {
        List<String> fileNames = new ArrayList();
        File[] files = folder.listFiles();
        for (File file : files) {
            fileNames.add(file.getName());
        }
        return fileNames;
    }

    @Test
    void shouldMapDirectoryInRepoToDirectoryUnderClientRoot() {
        P4Material p4Material = p4Fixture.material("//depot/lib/... //cws/release1/...");
        updateMaterial(p4Material, new StringRevision("2"));
        assertThat(getContainedFileNames(new File(clientFolder, "release1"))).contains("junit.jar");
    }

    @Test
    void shouldExcludeSpecifiedDirectory() {
        P4Material p4Material = p4Fixture.material("//depot/... //cws/...   \n  -//depot/lib/... //cws/release1/...");
        updateMaterial(p4Material, new StringRevision("2"));

        assertThat(new File(clientFolder, "release1").exists()).isFalse();
        assertThat(new File(clientFolder, "release1/junit.jar").exists()).isFalse();
    }

    @Test
    void shouldSupportAsterisk() {
        P4Material p4Material = p4Fixture.material("//depot/lib/*.jar //cws/*.war");
        updateMaterial(p4Material, new StringRevision("2"));

        File file = new File(clientFolder, "junit.war");
        assertThat(file.exists()).isTrue();
    }

    @Test
    void shouldSupportPercetage() {
        P4Material p4Material = p4Fixture.material("//depot/lib/%%1.%%2 //cws/%%2.%%1");
        updateMaterial(p4Material, new StringRevision("2"));
        File file = new File(clientFolder, "jar.junit");
        assertThat(file.exists()).isTrue();
    }

    @Test
    void laterDefinitionShouldOveridePreviousOne() {
        P4Material p4Material = p4Fixture.material(
                "//depot/src/... //cws/build/... \n //depot/lib/... //cws/build/...");

        File file = new File(clientFolder, "build/junit.jar");
        File folderNet = new File(clientFolder, "build/net");
        updateMaterial(p4Material, new StringRevision("2"));

        assertThat(folderNet.exists()).isFalse();
        assertThat(file.exists()).isTrue();
    }

    @Test
    void laterDefinitionShouldMergeWithPreviousOneWhenPlusPresent() {
        P4Material p4Material = p4Fixture.material(
                "//depot/src/... //cws/build/... \n +//depot/lib/... //cws/build/...");

        File file = new File(clientFolder, "build/junit.jar");
        File folderNet = new File(clientFolder, "build/net");
        updateMaterial(p4Material, new StringRevision("2"));
        assertThat(folderNet.exists()).isTrue();
        assertThat(file.exists()).isTrue();
    }

    @Test
    void shouldCleanOutRepoWhenViewChanges() {
        P4Material p4Material = p4Fixture.material(VIEW);
        updateMaterial(p4Material, new StringRevision("2"));
        assertThat(clientFolder.listFiles()).hasSize(8);

        P4Material otherMaterial = p4Fixture.material("//depot/lib/... //something/...");
        updateMaterial(otherMaterial, new StringRevision("2"));
        assertThat(clientFolder.listFiles()).hasSize(2);
    }

    private void updateMaterial(P4Material p4Material, StringRevision revision) {
        p4Material.updateTo(outputconsumer, clientFolder, new RevisionContext(revision), new TestSubprocessExecutionContext());
    }

    @Test
    void shouldCreateUniqueClientForDifferentFoldersOnTheSameMachine() {
        P4Material p4Material = p4Fixture.material(VIEW);

        String name1 = p4Material.clientName(new File("/one-directory/foo/with_underlines"));
        String name2 = p4Material.clientName(new File("/another-directory/foo/with_underlines"));
        assertThat(name1).isNotEqualTo(name2);
        assertThat(name1.matches("[0-9a-zA-Z_\\.\\-]*")).as("Client name should be a legal filename: " + name1).isTrue();
        assertThat(name2.matches("[0-9a-zA-Z_\\.\\-]*")).as("Client name should be a legal filename: " + name2).isTrue();
    }

    @Test
    void hashCodeShouldBeSameWhenP4MaterialEquals() {
        P4Material p4 = p4Fixture.material("material1");
        P4Material anotherP4 = p4Fixture.material("material1");

        assertThat(p4).isEqualTo(anotherP4);
        assertThat(p4.hashCode()).isEqualTo(anotherP4.hashCode());
    }

    @Test
    void shouldBeAbleToConvertToJson() {
        P4Material p4Material = p4Fixture.material(VIEW);

        Map<String, Object> json = new LinkedHashMap<>();
        p4Material.toJson(json, new StringRevision("123"));

        JsonValue jsonValue = from(json);
        assertThat(jsonValue.getString("scmType")).isEqualTo("Perforce");
        assertThat(jsonValue.getString("location")).isEqualTo(p4Material.getServerAndPort());
        assertThat(jsonValue.getString("action")).isEqualTo("Modified");
    }

    @Test
    void shouldLogRepoInfoToConsoleOutWithoutFolder() {
        P4Material p4Material = p4Fixture.material(VIEW);
        updateMaterial(p4Material, new StringRevision("2"));
        String message = format("Start updating %s at revision %s from %s", "files", "2", p4Material.getUrl());
        assertThat(outputconsumer.getStdOut()).contains(message);
    }

    @Test
    void shouldGenerateSqlCriteriaMapInSpecificOrder() {
        Map<String, Object> map = p4Fixture.material("view").getSqlCriteria();
        assertThat(map.size()).isEqualTo(4);
        Iterator<Map.Entry<String, Object>> iter = map.entrySet().iterator();
        assertThat(iter.next().getKey()).isEqualTo("type");
        assertThat(iter.next().getKey()).isEqualTo("url");
        assertThat(iter.next().getKey()).isEqualTo("username");
        assertThat(iter.next().getKey()).isEqualTo("view");
    }
}
