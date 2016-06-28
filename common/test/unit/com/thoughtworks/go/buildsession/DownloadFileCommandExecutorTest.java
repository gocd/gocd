/*
 * Copyright 2016 ThoughtWorks, Inc.
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
package com.thoughtworks.go.buildsession;

import com.thoughtworks.go.util.FileUtil;
import com.thoughtworks.go.util.StringUtil;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static com.thoughtworks.go.domain.BuildCommand.downloadFile;
import static com.thoughtworks.go.domain.JobResult.Failed;
import static com.thoughtworks.go.domain.JobResult.Passed;
import static com.thoughtworks.go.util.MapBuilder.map;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class DownloadFileCommandExecutorTest extends BuildSessionBasedTestCase {
    @Test
    public void downloadFilePrintErrorWhenFailed() {
        runBuild(downloadFile(map(
                "url", "http://far.far.away/foo.jar",
                "dest", new File(sandbox, "bar.jar").getPath())), Failed);
        assertThat(console.output(), containsString("Could not fetch artifact"));
    }

    @Test
    public void downloadFileWithoutMD5Check() throws IOException {
        File dest = new File(sandbox, "bar.jar");
        httpService.setupDownload("http://far.far.away/foo.jar", "some content");
        runBuild(downloadFile(map(
                "url", "http://far.far.away/foo.jar",
                "dest", "bar.jar")), Passed);
        assertThat(console.output(), containsString("without verifying the integrity"));
        assertThat(FileUtil.readContentFromFile(dest), is("some content"));
    }

    @Test
    public void downloadFileWithMD5Check() throws IOException {
        httpService.setupDownload("http://far.far.away/foo.jar", "some content");
        httpService.setupDownload("http://far.far.away/foo.jar.md5", "foo.jar=9893532233caff98cd083a116b013c0b");
        runBuild(downloadFile(map(
                "url", "http://far.far.away/foo.jar",
                "dest", "dest.jar",
                "src", "foo.jar",
                "checksumUrl", "http://far.far.away/foo.jar.md5")), Passed);
        assertThat(console.output(), containsString(String.format("Saved artifact to [%s] after verifying the integrity of its contents", new File(sandbox, "dest.jar").getPath())));
        assertThat(FileUtil.readContentFromFile(new File(sandbox, "dest.jar")), is("some content"));
    }

    @Test
    public void downloadFileShouldAppendSha1IntoDownloadUrlIfDestFileAlreadyExists() throws IOException {
        File dest = new File(sandbox, "bar.jar");
        Files.write(Paths.get(dest.getPath()), "foobar".getBytes());
        String sha1 = java.net.URLEncoder.encode(StringUtil.sha1Digest(dest), "UTF-8");

        httpService.setupDownload("http://far.far.away/foo.jar", "content without sha1");
        httpService.setupDownload("http://far.far.away/foo.jar?sha1=" + sha1, "content with sha1");
        runBuild(downloadFile(map(
                "url", "http://far.far.away/foo.jar",
                "dest", "bar.jar")), Passed);
        assertThat(console.output(), containsString("Saved artifact"));
        assertThat(FileUtil.readContentFromFile(dest), is("content with sha1"));
    }
}