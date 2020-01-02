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
package com.thoughtworks.go.helper;

import com.thoughtworks.go.config.materials.svn.SvnMaterial;
import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.domain.materials.svn.SubversionRevision;
import com.thoughtworks.go.domain.materials.svn.SvnCommand;
import com.thoughtworks.go.util.command.ProcessOutputStreamConsumer;
import org.apache.commons.io.FileUtils;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;

import static com.thoughtworks.go.util.command.ProcessOutputStreamConsumer.inMemoryConsumer;

public class SvnTestRepoWithExternal extends SvnTestRepo {
    private Date beforeCheckin;
    private Date afterCheckin;
    private ProcessOutputStreamConsumer outputStreamConsumer = inMemoryConsumer();
    private File workingFolder;
    public static final String EXTERNAL_REPO_NAME = "end2end";
    private String externalRepoUrl;

    public SvnTestRepoWithExternal(String externalRepoUrl, TemporaryFolder temporaryFolder) throws IOException {
        super(temporaryFolder);
        this.externalRepoUrl = externalRepoUrl;
        setupWorkingFolder();
        setupExternals(EXTERNAL_REPO_NAME, workingFolder);
    }

    public SvnTestRepoWithExternal(TemporaryFolder temporaryFolder) throws IOException {
        this(null, temporaryFolder);
    }

    private void setupWorkingFolder() throws IOException {
        workingFolder = temporaryFolder.newFolder();
        tmpFolders.add(this.workingFolder);
    }

    public Date beforeCheckin() {
        return beforeCheckin;
    }

    public Date afterCheckin() {
        return afterCheckin;
    }

    public void setupExternals(String externalRepoName, File externalsHost) throws IOException {
        String url = projectRepositoryUrl();
        SvnCommand svnRepo = getSvnExternalCommand(url, false);
        svnRepo.checkoutTo(outputStreamConsumer, workingFolder, SubversionRevision.HEAD);
        svnRepo.propset(externalsHost, "svn:externals", externalRepoName + " " + externalRepositoryUrl());
        beforeCheckin = new Date();
        svnRepo.commit(inMemoryConsumer(), workingFolder, "changed svn externals");
        commitToExternalRepo(externalRepositoryUrl());

        afterCheckin = new Date();
    }

    private String escape(String externalUrl) {
        return externalUrl.replace(" ", "%20");
    }

    private void commitToExternalRepo(String externalUrl) throws IOException {
        File folder = temporaryFolder.newFolder();
        try {
            SvnCommand repository = getSvnExternalCommand(externalUrl, false);
            repository.checkoutTo(outputStreamConsumer, folder, SubversionRevision.HEAD);
            repository.propset(folder, "newPropetyForExternal", "any value");
            repository.commit(inMemoryConsumer(), folder, "make change");
        } finally {
            FileUtils.deleteQuietly(folder);
        }
    }

    private SvnCommand getSvnExternalCommand(String externalUrl, boolean checkExternals) {
        return new SvnCommand(null, externalUrl, null, null, checkExternals);
    }

    public String externalRepositoryUrl() {
        return escape(externalRepoUrl == null ? repositoryUrl(EXTERNAL_REPO_NAME)  : externalRepoUrl);
    }

    public File workingFolder() {
        return workingFolder;
    }

    public List<Modification> checkInExternalFile(String fileName, String comment) throws IOException {
        SvnMaterial svnMaterial = new SvnMaterial(externalRepositoryUrl(), null, null, false);
        return checkInOneFile(svnMaterial, fileName, comment);
    }

    public SvnMaterial externalMaterial() {
        SvnMaterial svnMaterial = new SvnMaterial(getSvnExternalCommand(externalRepositoryUrl(), true));
        svnMaterial.setFolder(EXTERNAL_REPO_NAME);
        return svnMaterial;
    }
}
