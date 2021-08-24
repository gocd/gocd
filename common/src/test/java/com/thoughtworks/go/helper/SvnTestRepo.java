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
package com.thoughtworks.go.helper;

import com.thoughtworks.go.config.materials.svn.SvnMaterial;
import com.thoughtworks.go.config.materials.svn.SvnMaterialConfig;
import com.thoughtworks.go.domain.materials.*;
import com.thoughtworks.go.util.FileUtil;
import com.thoughtworks.go.util.command.InMemoryStreamConsumer;
import com.thoughtworks.go.util.command.ProcessOutputStreamConsumer;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static com.thoughtworks.go.helper.MaterialConfigsMother.svn;
import static com.thoughtworks.go.util.command.ProcessOutputStreamConsumer.inMemoryConsumer;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.io.FileUtils.copyDirectory;
import static org.junit.jupiter.api.Assertions.fail;

public class SvnTestRepo extends TestRepo {
    private static final String REPO_TEST_DATA_FOLDER = "../common/src/test/resources/data/svnrepo";

    protected File tempRepo;

    public SvnTestRepo(Path tempDir) throws IOException {
        this(tempDir, null);
    }

    public SvnTestRepo(Path tempDir, String folderName) throws IOException {
        super(tempDir);
        tempRepo = createTempDirectory(folderName).toFile();
        createTempRepository(tempRepo);
    }

    private void createTempRepository(File location) {
        try {
            copyDirectory(new File(REPO_TEST_DATA_FOLDER), location);
        } catch (IOException e) {
            fail("Could not copy test repo [" + REPO_TEST_DATA_FOLDER + "] into [" + location + "] beacuse of " + e.getMessage());
        }
    }


    public String urlFor(String project) {
        return repositoryUrl(project);
    }

    @Override
    public String projectRepositoryUrl() {
        return repositoryUrl("project1/trunk");
    }

    public File projectRepositoryRoot() {
        return repositoryUrlAsFile("project1");
    }

    public File projectRepositoryUrlAsFile() {
        return repositoryUrlAsFile("project1/trunk");
    }

    public String end2endRepositoryUrl() {
        return repositoryUrl("end2end");
    }

    public String repositoryUrl(String project) {
        return FileUtil.toFileURI(new File(tempRepo, project));
    }

    public File repositoryUrlAsFile(String project) {
        return new File(tempRepo, project);
    }

    @Override
    public SvnMaterial material() {
        return new SvnMaterial(projectRepositoryUrl(), null, null, false);
    }

    public SvnMaterialConfig materialConfig() {
        return svn(projectRepositoryUrl(), null, null, false);
    }

    public SvnMaterial createMaterial(String repo, String folder) {
        SvnMaterial material = MaterialsMother.svnMaterial(urlFor(repo), folder);
        material.setAutoUpdate(true);
        return material;
    }

    public String latestRevision() throws IOException {
        return getLatestRevision(material()).getRevision();
    }

    public String end2ndRepositoryLatestRevision() throws IOException {
        return getLatestRevision(new SvnMaterial(end2endRepositoryUrl(), null, null, false)).getRevision();
    }

    private Revision getLatestRevision(SvnMaterial svnMaterial) throws IOException {
        final File workingCopy = createRandomTempDirectory().toFile();
        return latestRevision(svnMaterial, workingCopy, new TestSubprocessExecutionContext());
    }

    public Modification checkInOneFile(String path) throws Exception {
        return checkInOneFile(path, "adding file [" + path + "]").get(0);
    }

    @Override
    public List<Modification> checkInOneFile(String filename, String message) throws Exception {
        SvnMaterial svnMaterial = material();

        return checkInOneFile(svnMaterial, filename, message);

    }

    protected List<Modification> checkInOneFile(SvnMaterial svnMaterial, String filename, String message) throws IOException {
        final File workingCopy = createRandomTempDirectory().toFile();

        InMemoryStreamConsumer consumer = inMemoryConsumer();


        Revision latestRevision = getLatestRevision(svnMaterial);
        svnMaterial.updateTo(consumer, workingCopy, new RevisionContext(latestRevision), new TestSubprocessExecutionContext());


        File newFileToAdd = new File(workingCopy, filename);
        File directoryToAddTo = newFileToAdd.getParentFile();
        boolean addedToExistingDir = directoryToAddTo.exists();
        directoryToAddTo.mkdirs();

        FileUtils.writeStringToFile(newFileToAdd, "", UTF_8);

        svnMaterial.add(consumer, addedToExistingDir ? newFileToAdd : directoryToAddTo);
        svnMaterial.commit(consumer, workingCopy, message);
        return svnMaterial.latestModification(workingCopy, new TestSubprocessExecutionContext());
    }

    @Override
    public List<Modification> latestModification() throws IOException {
        final File workingCopy = createRandomTempDirectory().toFile();
        return material().latestModification(workingCopy, new TestSubprocessExecutionContext());
    }

    public void checkInOneFile(String fileName, SvnMaterial svnMaterial) throws Exception {
        final File baseDir = createRandomTempDirectory().toFile();

        ProcessOutputStreamConsumer consumer = inMemoryConsumer();

        Revision revision = latestRevision(svnMaterial, baseDir, new TestSubprocessExecutionContext());
        svnMaterial.updateTo(consumer, baseDir, new RevisionContext(revision), new TestSubprocessExecutionContext());

        File workingDir = new File(baseDir, svnMaterial.getFolder());
        File newFileToAdd = new File(workingDir, fileName);
        newFileToAdd.getParentFile().mkdirs();

        FileUtils.writeStringToFile(newFileToAdd, "", UTF_8);
        svnMaterial.add(consumer, newFileToAdd);
        svnMaterial.commit(consumer, workingDir, "adding file [" + svnMaterial.getFolder() + "/" + fileName + "]");
    }

    private Revision latestRevision(SvnMaterial material, File workingDir, TestSubprocessExecutionContext execCtx) {
        List<Modification> modifications = material.latestModification(workingDir, execCtx);
        return new Modifications(modifications).latestRevision(material);
    }

}
