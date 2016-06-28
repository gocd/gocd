/*
 * Copyright 2015 ThoughtWorks, Inc.
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

import java.io.File;
import java.util.*;

import com.thoughtworks.go.domain.materials.RevisionContext;
import com.thoughtworks.go.domain.materials.TestSubprocessExecutionContext;
import com.thoughtworks.go.domain.materials.ValidationBean;
import com.thoughtworks.go.domain.materials.mercurial.StringRevision;
import com.thoughtworks.go.domain.materials.perforce.PerforceFixture;
import com.thoughtworks.go.helper.P4TestRepo;

import com.thoughtworks.go.util.JsonValue;
import com.thoughtworks.go.util.StringUtil;
import org.junit.Test;

import static com.thoughtworks.go.util.JsonUtils.from;
import static java.lang.String.format;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.assertThat;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.junit.Assert.fail;

public abstract class P4MaterialTestBase extends PerforceFixture {
    protected static final String VIEW = "//depot/... //something/...";

    @Test public void shouldValidateCorrectConnection() throws Exception {
        P4Material p4Material = p4Fixture.material(VIEW);
        p4Material.setPassword("secret");
        p4Material.setUseTickets(false);
        ValidationBean validation = p4Material.checkConnection(new TestSubprocessExecutionContext());
        assertThat(validation.isValid(), is(true));
        assertThat(StringUtil.isBlank(validation.getError()), is(true));
    }

    @Test public void shouldBeAbleToGetUrlArgument() throws Exception {
        P4Material p4Material = new P4Material("localhost:9876", "p4view");
        assertThat(p4Material.getUrlArgument().forDisplay(), is("localhost:9876"));
    }

    @Test public void shouldCheckConnection() throws Exception {
        P4Material p4Material = new P4Material("localhost:9876", "p4view");
        p4Material.setPassword("secret");
        p4Material.setUseTickets(false);
        ValidationBean validation = p4Material.checkConnection(new TestSubprocessExecutionContext());
        assertThat(validation.isValid(), is(false));
        assertThat(validation.getError(), containsString("Unable to connect to server localhost:9876"));
        assertThat(validation.getError(), not(containsString("secret")));
    }

    @Test public void shouldReplacePasswordUsingStar() throws Exception {
        P4Material p4Material = new P4Material("localhost:9876", "p4view");
        p4Material.setPassword("secret");
        p4Material.setUseTickets(true);

        try{
            p4Material.latestModification(clientFolder, new TestSubprocessExecutionContext());
            fail("should throw exception because p4 server not exists.");
        }catch(Exception e){
            assertThat(e.getMessage(), not(containsString("secret")));
        }
    }

    @Test public void shouldUpdateToSpecificRevision() throws Exception {
        P4Material p4Material = p4Fixture.material(VIEW);
        updateMaterial(p4Material, new StringRevision("2"));
        assertThat(clientFolder.listFiles().length, is(8));

        updateMaterial(p4Material, new StringRevision("3"));
        assertThat(clientFolder.listFiles().length, is(7));
    }

    @Test public void shouldCleanOutRepoWhenMaterialChanges() throws Exception {
        P4TestRepo secondTestRepo = P4TestRepo.createP4TestRepo();
        try {
            secondTestRepo.onSetup();

            P4Material p4Material = p4Fixture.material(VIEW);
            updateMaterial(p4Material, new StringRevision("2"));
            File.createTempFile("temp", "txt", clientFolder);
            assertThat(clientFolder.listFiles().length, is(9));

            P4Material otherMaterial = secondTestRepo.material("//depot/lib/... //something/...");
            otherMaterial.setUsername("cceuser1");
            updateMaterial(otherMaterial, new StringRevision("2"));
            File.createTempFile("temp", "txt", clientFolder);
            assertThat("Should clean and re-checkout after p4repo changed", clientFolder.listFiles().length, is(3));
            otherMaterial.setUsername("cceuser");
            otherMaterial.setPassword("password");
            updateMaterial(otherMaterial, new StringRevision("2"));
            assertThat("Should clean and re-checkout after user changed", clientFolder.listFiles().length, is(2));
            assertThat(outputconsumer.getStdOut(),
                    containsString("Working directory has changed. Deleting and re-creating it."));
        } finally {
            secondTestRepo.stop();
            secondTestRepo.onTearDown();
        }
    }

    @Test
    public void shouldMapDirectoryInRepoToClientRoot() throws Exception {
        P4Material p4Material = p4Fixture.material("//depot/lib/... //cws/...");
        updateMaterial(p4Material, new StringRevision("2"));

        assertThat(getContainedFileNames(clientFolder), hasItem("junit.jar"));
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
    public void shouldMapDirectoryInRepoToDirectoryUnderClientRoot() throws Exception {
        P4Material p4Material = p4Fixture.material("//depot/lib/... //cws/release1/...");
        updateMaterial(p4Material, new StringRevision("2"));
        assertThat(getContainedFileNames(new File(clientFolder, "release1")), hasItem("junit.jar"));
    }

    @Test
    public void shouldExcludeSpecifiedDirectory() throws Exception {
        P4Material p4Material = p4Fixture.material("//depot/... //cws/...   \n  -//depot/lib/... //cws/release1/...");
        updateMaterial(p4Material, new StringRevision("2"));

        assertThat(new File(clientFolder, "release1").exists(), is(false));
        assertThat(new File(clientFolder, "release1/junit.jar").exists(), is(false));
    }

    @Test
    public void shouldSupportAsterisk() throws Exception {
        P4Material p4Material = p4Fixture.material("//depot/lib/*.jar //cws/*.war");
        updateMaterial(p4Material, new StringRevision("2"));

        File file = new File(clientFolder, "junit.war");
        assertThat(file.exists(), is(true));
    }

    @Test
    public void shouldSupportPercetage() throws Exception {
        P4Material p4Material = p4Fixture.material("//depot/lib/%%1.%%2 //cws/%%2.%%1");
        updateMaterial(p4Material, new StringRevision("2"));
        File file = new File(clientFolder, "jar.junit");
        assertThat(file.exists(), is(true));
    }

    @Test
    public void laterDefinitionShouldOveridePreviousOne() throws Exception {
        P4Material p4Material = p4Fixture.material(
                "//depot/src/... //cws/build/... \n //depot/lib/... //cws/build/...");

        File file = new File(clientFolder, "build/junit.jar");
        File folderNet = new File(clientFolder, "build/net");
        updateMaterial(p4Material, new StringRevision("2"));

        assertThat(folderNet.exists(), is(false));
        assertThat(file.exists(), is(true));
    }

    @Test
    public void laterDefinitionShouldMergeWithPreviousOneWhenPlusPresent() throws Exception {
        P4Material p4Material = p4Fixture.material(
                "//depot/src/... //cws/build/... \n +//depot/lib/... //cws/build/...");

        File file = new File(clientFolder, "build/junit.jar");
        File folderNet = new File(clientFolder, "build/net");
        updateMaterial(p4Material, new StringRevision("2"));
        assertThat(folderNet.exists(), is(true));
        assertThat(file.exists(), is(true));
    }

    @Test public void shouldCleanOutRepoWhenViewChanges() throws Exception {
        P4Material p4Material = p4Fixture.material(VIEW);
        updateMaterial(p4Material, new StringRevision("2"));
        assertThat(clientFolder.listFiles().length, is(8));

        P4Material otherMaterial = p4Fixture.material("//depot/lib/... //something/...");
        updateMaterial(otherMaterial, new StringRevision("2"));
        assertThat(clientFolder.listFiles().length, is(2));
    }

    private void updateMaterial(P4Material p4Material, StringRevision revision) {
        p4Material.updateTo(outputconsumer, clientFolder, new RevisionContext(revision), new TestSubprocessExecutionContext());
    }

    @Test
    public void shouldCreateUniqueClientForDifferentFoldersOnTheSameMachine() {
        P4Material p4Material = p4Fixture.material(VIEW);

        String name1 = p4Material.clientName(new File("/one-directory/foo/with_underlines"));
        String name2 = p4Material.clientName(new File("/another-directory/foo/with_underlines"));
        assertThat(name1, is(not(name2)));
        assertThat("Client name should be a legal filename: " + name1,
                name1.matches("[0-9a-zA-Z_\\.\\-]*"), is(true));
        assertThat("Client name should be a legal filename: " + name2,
                name2.matches("[0-9a-zA-Z_\\.\\-]*"), is(true));
    }

    @Test
    public void hashCodeShouldBeSameWhenP4MaterialEquals() throws Exception {
        P4Material p4 = p4Fixture.material("material1");
        P4Material anotherP4 = p4Fixture.material("material1");

        assertThat(p4, is(anotherP4));
        assertThat(p4.hashCode(), is(anotherP4.hashCode()));
    }

    @Test
    public void shouldBeAbleToConvertToJson() {
        P4Material p4Material = p4Fixture.material(VIEW);

        Map<String, Object> json = new LinkedHashMap<>();
        p4Material.toJson(json, new StringRevision("123"));

        JsonValue jsonValue = from(json);
        assertThat(jsonValue.getString("scmType"), is("Perforce"));
        assertThat(jsonValue.getString("location"), is(p4Material.getServerAndPort()));
        assertThat(jsonValue.getString("action"), is("Modified"));
    }

    @Test
    public void shouldLogRepoInfoToConsoleOutWithoutFolder() throws Exception {
        P4Material p4Material = p4Fixture.material(VIEW);
        updateMaterial(p4Material, new StringRevision("2"));
        String message = format("Start updating %s at revision %s from %s", "files", "2", p4Material.getUrl());
        assertThat(outputconsumer.getStdOut(), containsString(message));
    }

    @Test public void shouldGenerateSqlCriteriaMapInSpecificOrder() throws Exception {
        Map<String, Object> map = p4Fixture.material("view").getSqlCriteria();
        assertThat(map.size(), is(4));
        Iterator<Map.Entry<String,Object>> iter = map.entrySet().iterator();
        assertThat(iter.next().getKey(), is("type"));
        assertThat(iter.next().getKey(), is("url"));
        assertThat(iter.next().getKey(), is("username"));
        assertThat(iter.next().getKey(), is("view"));
    }
}
