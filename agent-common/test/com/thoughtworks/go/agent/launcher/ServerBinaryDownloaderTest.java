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

package com.thoughtworks.go.agent.launcher;

import com.thoughtworks.go.agent.common.util.Downloader;
import com.thoughtworks.go.agent.testhelper.FakeBootstrapperServer;
import com.thoughtworks.go.mothers.ServerUrlGeneratorMother;
import com.thoughtworks.go.util.SslVerificationMode;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.util.Map;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

@RunWith(FakeBootstrapperServer.class)
public class ServerBinaryDownloaderTest {

    @After
    public void tearDown() throws Exception {
        FileUtils.deleteQuietly(new File(Downloader.AGENT_BINARY));
    }

    @Test
    public void shouldGetAllHeaders() throws Exception {
        ServerBinaryDownloader downloader = new ServerBinaryDownloader(ServerUrlGeneratorMother.generatorFor("localhost", 9090), DownloadableFile.AGENT, null, SslVerificationMode.NONE);
        Map<String, String> headers = downloader.headers();
        assertNotNull(headers.get("Content-MD5"));
        assertThat(headers.get("Cruise-Server-Ssl-Port"), is("9091"));
    }

    @Test
    public void shouldDownloadTheBinaryAlwaysIrrespectiveOfLocalFileChange() {
        ServerBinaryDownloader downloader = new ServerBinaryDownloader(ServerUrlGeneratorMother.generatorFor("localhost", 9090), DownloadableFile.AGENT, null, SslVerificationMode.NONE);
        ServerBinaryDownloader.DownloadResult result = downloader.downloadAlways();
        assertThat(result.performedDownload, is(true));
        result = downloader.downloadAlways();
        assertThat(result.performedDownload, is(true));
    }

    @Test
    public void shouldDownloadTheBinaryOverSSL() throws Exception {
        ServerBinaryDownloader downloader = new ServerBinaryDownloader(ServerUrlGeneratorMother.generatorFor("https://localhost:9091/go"), DownloadableFile.AGENT, new File("testdata/test_cert.pem"), SslVerificationMode.NO_VERIFY_HOST);
        ServerBinaryDownloader.DownloadResult result = downloader.downloadAlways();
        assertThat(result.performedDownload, is(true));
        result = downloader.downloadAlways();
        assertThat(result.performedDownload, is(true));
    }
}
