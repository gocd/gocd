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

package com.thoughtworks.go.domain.builder;

import com.thoughtworks.go.buildsession.BuildSessionBasedTestCase;
import com.thoughtworks.go.domain.*;
import com.thoughtworks.go.util.*;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.zip.Deflater;

import static java.lang.String.format;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertThat;

public class FetchArtifactBuilderBuildCommandTest extends BuildSessionBasedTestCase {
    private File zip;

    @Before
    public void setUp() throws Exception {
        File folder = TestFileUtil.createTempFolder("log");
        File consolelog = new File(folder, "console.log");
        folder.mkdirs();
        consolelog.createNewFile();
        zip = new ZipUtil().zip(folder, TestFileUtil.createUniqueTempFile(folder.getName()), Deflater.NO_COMPRESSION);
    }


    @Test
    public void shouldUnzipWhenFetchingFolder() throws Exception {
        httpService.setupDownload(format("%s/remoting/files/cruise/1/dev/1/windows/log.zip", new URLService().baseRemoteURL()), zip);

        FetchArtifactBuilder builder = getBuilder(new JobIdentifier("cruise", -10, "1", "dev", "1", "windows", 1L), "log", "dest", new DirHandler("log", new File("pipelines/cruise/dest")));
        runBuilder(builder, JobResult.Passed);
        assertDownloaded(new File(sandbox, "pipelines/cruise/dest"));
    }

    @Test
    public void shouldGiveWarningWhenMd5FileNotExists() throws Exception {
        httpService.setupDownload(format("%s/remoting/files/cruise/1/dev/1/windows/a.jar", new URLService().baseRemoteURL()), "some content");

        FetchArtifactBuilder builder = getBuilder(new JobIdentifier("cruise", -1, "1", "dev", "1", "windows", 1L), "a.jar", "foo", new FileHandler(new File("pipelines/cruise/foo/a.jar"), "a.jar"));

        runBuilder(builder, JobResult.Passed);
        assertThat(new File(sandbox, "pipelines/cruise/foo/a.jar").isFile(), is(true));
        assertThat(console.output(), containsString("[WARN] The md5checksum property file was not found"));
    }

    @Test
    public void shouldFailBuildWhenChecksumNotValidForArtifact() throws Exception {
        httpService.setupDownload(format("%s/remoting/files/cruise/1/dev/1/windows/cruise-output/md5.checksum", new URLService().baseRemoteURL()), "a.jar=invalid-checksum");
        httpService.setupDownload(format("%s/remoting/files/cruise/1/dev/1/windows/a.jar", new URLService().baseRemoteURL()), "some content");

        FetchArtifactBuilder builder = getBuilder(new JobIdentifier("cruise", -1, "1", "dev", "1", "windows", 1L), "a.jar", "foo", new FileHandler(new File("pipelines/cruise/foo/a.jar"), "a.jar"));
        runBuilder(builder, JobResult.Failed);
        assertThat(console.output(), containsString("[ERROR] Verification of the integrity of the artifact [a.jar] failed"));
        assertThat(new File(sandbox, "pipelines/cruise/foo/a.jar").isFile(), is(true));
    }

    @Test
    public void shouldBuildWhenChecksumValidForArtifact() throws Exception {
        httpService.setupDownload(format("%s/remoting/files/cruise/1/dev/1/windows/cruise-output/md5.checksum", new URLService().baseRemoteURL()), "a.jar=9893532233caff98cd083a116b013c0b");
        httpService.setupDownload(format("%s/remoting/files/cruise/1/dev/1/windows/a.jar", new URLService().baseRemoteURL()), "some content");

        FetchArtifactBuilder builder = getBuilder(new JobIdentifier("cruise", -1, "1", "dev", "1", "windows", 1L), "a.jar", "foo", new FileHandler(new File("pipelines/cruise/foo/a.jar"), "a.jar"));
        runBuilder(builder, JobResult.Passed);
        assertThat(console.output(), containsString(format("Saved artifact to [%s] after verifying the integrity of its contents", new File(sandbox, "pipelines/cruise/foo/a.jar").getPath())));
    }

    @Test
    public void shouldFailBuildAndPrintErrorMessageToConsoleWhenArtifactNotExisit() throws Exception {
        FetchArtifactBuilder builder = getBuilder(new JobIdentifier("cruise", -1, "1", "dev", "1", "windows", 1L), "a.jar", "foo", new FileHandler(new File("pipelines/cruise/foo/a.jar"), "a.jar"));
        runBuilder(builder, JobResult.Failed);
        assertThat(console.output(), not(containsString("Saved artifact")));
        assertThat(console.output(), containsString("Could not fetch artifact"));
    }

    @Test
    public void shouldDownloadWithURLContainsSHA1WhenFileExists() throws Exception {
        File artifactOnAgent = new File(sandbox, "pipelines/cruise/foo/a.jar");
        new File(sandbox, "pipelines/cruise/foo").mkdirs();
        FileUtil.writeContentToFile("foobar", artifactOnAgent);
        String sha1 = java.net.URLEncoder.encode(StringUtil.sha1Digest(artifactOnAgent), "UTF-8");

        httpService.setupDownload(format("%s/remoting/files/cruise/1/dev/1/windows/a.jar", new URLService().baseRemoteURL()), "content for url without sha1");

        httpService.setupDownload(format("%s/remoting/files/cruise/1/dev/1/windows/a.jar?sha1=%s", new URLService().baseRemoteURL(), sha1), "content for url with sha1");


        FetchArtifactBuilder builder = getBuilder(new JobIdentifier("cruise", -1, "1", "dev", "1", "windows", 1L), "a.jar", "foo", new FileHandler(new File("pipelines/cruise/foo/a.jar"), "a.jar"));

        runBuilder(builder, JobResult.Passed);
        assertThat(artifactOnAgent.isFile(), is(true));
        assertThat(FileUtil.readContentFromFile(artifactOnAgent), is("content for url with sha1"));
    }


    private void assertDownloaded(File destOnAgent) {
        File logFolder = new File(destOnAgent, "log");
        assertThat(logFolder.exists(), is(true));
        assertThat(logFolder.isDirectory(), is(true));
        assertThat(new File(logFolder, "console.log").exists(), is(true));
        assertThat(destOnAgent.listFiles(), is(new File[]{logFolder}));
    }


    private void runBuilder(FetchArtifactBuilder builder, JobResult expectedResult) {
        BuildCommand buildCommand = builder.buildCommand();
        JobResult result = newBuildSession().build(buildCommand);
        assertThat(buildInfo(), result, is(expectedResult));
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
