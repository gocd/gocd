/*
 * Copyright 2019 ThoughtWorks, Inc.
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

package com.thoughtworks.go.domain.builder;

import com.thoughtworks.go.buildsession.BuildSessionBasedTestCase;
import com.thoughtworks.go.domain.*;
import com.thoughtworks.go.util.FileUtil;
import com.thoughtworks.go.util.URLService;
import com.thoughtworks.go.util.ZipUtil;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.UUID;
import java.util.zip.Deflater;

import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

class FetchArtifactBuilderBuildCommandTest extends BuildSessionBasedTestCase {
    private File zip;

    @BeforeEach
    void setUp() throws Exception {
        File folder = temporaryFolder.newFolder("log");
        File consolelog = new File(folder, "console.log");
        folder.mkdirs();
        consolelog.createNewFile();
        File uniqueTempFile = new File(folder, UUID.randomUUID().toString());
        uniqueTempFile.createNewFile();
        zip = new ZipUtil().zip(folder, uniqueTempFile, Deflater.NO_COMPRESSION);
    }


    @Test
    void shouldUnzipWhenFetchingFolder() throws Exception {
        httpService.setupDownload(format("%s/remoting/files/cruise/1/dev/1/windows/log.zip", new URLService().baseRemoteURL()), zip);

        FetchArtifactBuilder builder = getBuilder(new JobIdentifier("cruise", -10, "1", "dev", "1", "windows", 1L), "log", "dest", new DirHandler("log", new File("pipelines/cruise/dest")));
        runBuilder(builder, JobResult.Passed);
        assertDownloaded(new File(sandbox, "pipelines/cruise/dest"));
    }

    @Test
    void shouldGiveWarningWhenMd5FileNotExists() {
        httpService.setupDownload(format("%s/remoting/files/cruise/1/dev/1/windows/a.jar", new URLService().baseRemoteURL()), "some content");

        FetchArtifactBuilder builder = getBuilder(new JobIdentifier("cruise", -1, "1", "dev", "1", "windows", 1L), "a.jar", "foo", new FileHandler(new File("pipelines/cruise/foo/a.jar"), "a.jar"));

        runBuilder(builder, JobResult.Passed);
        assertThat(new File(sandbox, "pipelines/cruise/foo/a.jar").isFile()).isTrue();
        assertThat(console.output()).contains("[WARN] The md5checksum property file was not found");
    }

    @Test
    void shouldFailBuildWhenChecksumNotValidForArtifact() {
        httpService.setupDownload(format("%s/remoting/files/cruise/1/dev/1/windows/cruise-output/md5.checksum", new URLService().baseRemoteURL()), "a.jar=invalid-checksum");
        httpService.setupDownload(format("%s/remoting/files/cruise/1/dev/1/windows/a.jar", new URLService().baseRemoteURL()), "some content");

        FetchArtifactBuilder builder = getBuilder(new JobIdentifier("cruise", -1, "1", "dev", "1", "windows", 1L), "a.jar", "foo", new FileHandler(new File("pipelines/cruise/foo/a.jar"), "a.jar"));
        runBuilder(builder, JobResult.Failed);
        assertThat(console.output()).contains("[ERROR] Verification of the integrity of the artifact [a.jar] failed");
        assertThat(new File(sandbox, "pipelines/cruise/foo/a.jar").isFile()).isTrue();
    }

    @Test
    void shouldBuildWhenChecksumValidForArtifact() {
        httpService.setupDownload(format("%s/remoting/files/cruise/1/dev/1/windows/cruise-output/md5.checksum", new URLService().baseRemoteURL()), "a.jar=9893532233caff98cd083a116b013c0b");
        httpService.setupDownload(format("%s/remoting/files/cruise/1/dev/1/windows/a.jar", new URLService().baseRemoteURL()), "some content");

        FetchArtifactBuilder builder = getBuilder(new JobIdentifier("cruise", -1, "1", "dev", "1", "windows", 1L), "a.jar", "foo", new FileHandler(new File("pipelines/cruise/foo/a.jar"), "a.jar"));
        runBuilder(builder, JobResult.Passed);
        assertThat(console.output()).contains(format("Saved artifact to [%s] after verifying the integrity of its contents", new File(sandbox, "pipelines/cruise/foo/a.jar").getPath()));
    }

    @Test
    void shouldFailBuildAndPrintErrorMessageToConsoleWhenArtifactNotExisit() {
        FetchArtifactBuilder builder = getBuilder(new JobIdentifier("cruise", -1, "1", "dev", "1", "windows", 1L), "a.jar", "foo", new FileHandler(new File("pipelines/cruise/foo/a.jar"), "a.jar"));
        runBuilder(builder, JobResult.Failed);
        assertThat(console.output()).doesNotContain("Saved artifact");
        assertThat(console.output()).contains("Could not fetch artifact");
    }

    @Test
    void shouldDownloadWithURLContainsSHA1WhenFileExists() throws Exception {
        File artifactOnAgent = new File(sandbox, "pipelines/cruise/foo/a.jar");
        new File(sandbox, "pipelines/cruise/foo").mkdirs();
        FileUtils.writeStringToFile(artifactOnAgent, "foobar", UTF_8);
        String sha1 = java.net.URLEncoder.encode(FileUtil.sha1Digest(artifactOnAgent), "UTF-8");

        httpService.setupDownload(format("%s/remoting/files/cruise/1/dev/1/windows/a.jar", new URLService().baseRemoteURL()), "content for url without sha1");

        httpService.setupDownload(format("%s/remoting/files/cruise/1/dev/1/windows/a.jar?sha1=%s", new URLService().baseRemoteURL(), sha1), "content for url with sha1");


        FetchArtifactBuilder builder = getBuilder(new JobIdentifier("cruise", -1, "1", "dev", "1", "windows", 1L), "a.jar", "foo", new FileHandler(new File("pipelines/cruise/foo/a.jar"), "a.jar"));

        runBuilder(builder, JobResult.Passed);
        assertThat(artifactOnAgent.isFile()).isTrue();
        assertThat(FileUtils.readFileToString(artifactOnAgent, UTF_8)).isEqualTo("content for url with sha1");
    }


    private void assertDownloaded(File destOnAgent) {
        File logFolder = new File(destOnAgent, "log");
        assertThat(logFolder.exists()).isTrue();
        assertThat(logFolder.isDirectory()).isTrue();
        assertThat(new File(logFolder, "console.log").exists()).isTrue();
        assertThat(destOnAgent.listFiles()).isEqualTo(new File[]{logFolder});
    }


    private void runBuilder(FetchArtifactBuilder builder, JobResult expectedResult) {
        BuildCommand buildCommand = builder.buildCommand();
        JobResult result = newBuildSession().build(buildCommand);
        assertThat(result).as(buildInfo()).isEqualTo(expectedResult);
    }

    private String getSrc() {
        return "";
    }

    private FetchArtifactBuilder getBuilder(JobIdentifier jobLocator, String srcdir, String dest, FetchHandler handler) {
        return new FetchArtifactBuilder(new RunIfConfigs(), new NullBuilder(), "", jobLocator, srcdir, dest, handler, new ChecksumFileHandler(checksumFile(jobLocator, srcdir, dest)));
    }

    private File checksumFile(JobIdentifier jobIdentifier, String srcdir, String dest) {
        File destOnAgent = new File("pipelines" + '/' + jobIdentifier.getPipelineName() + '/' + dest);
        return new File(destOnAgent, String.format("%s_%s_%s_md5.checksum", jobIdentifier.getPipelineName(), jobIdentifier.getStageName(), jobIdentifier.getBuildName()));
    }

}
