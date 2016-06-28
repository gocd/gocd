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
import com.thoughtworks.go.util.TestFileUtil;
import com.thoughtworks.go.util.ZipUtil;
import org.junit.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.zip.Deflater;

import static com.thoughtworks.go.domain.BuildCommand.downloadDir;
import static com.thoughtworks.go.domain.JobResult.Passed;
import static com.thoughtworks.go.util.MapBuilder.map;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;


public class DownloadDirCommandExecutorTest extends BuildSessionBasedTestCase {

    @Test
    public void downloadDirWithChecksum() throws Exception {
        File folder = TestFileUtil.createTempFolder("log");
        Files.write(Paths.get(folder.getPath(), "a"), "content for a".getBytes());
        Files.write(Paths.get(folder.getPath(), "b"), "content for b".getBytes());


        File zip = new ZipUtil().zip(folder, TestFileUtil.createUniqueTempFile(folder.getName()), Deflater.NO_COMPRESSION);

        httpService.setupDownload("http://far.far.away/log.zip", zip);
        httpService.setupDownload("http://far.far.away/log.zip.md5", "s/log/a=524ebd45bd7de3616317127f6e639bd6\ns/log/b=83c0aa3048df233340203c74e8a93d7d");

        runBuild(downloadDir(map(
                "url", "http://far.far.away/log.zip",
                "dest", "dest",
                "src", "s/log",
                "checksumUrl", "http://far.far.away/log.zip.md5")), Passed);
        File dest = new File(sandbox, "dest");

        assertThat(console.output(), containsString(String.format("Saved artifact to [%s] after verifying the integrity of its contents", dest.getPath())));
        assertThat(FileUtil.readContentFromFile(new File(dest, "log/a")), is("content for a"));
        assertThat(FileUtil.readContentFromFile(new File(dest, "log/b")), is("content for b"));
    }

}