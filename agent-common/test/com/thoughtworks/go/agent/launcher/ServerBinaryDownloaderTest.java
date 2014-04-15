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

package com.thoughtworks.go.agent.launcher;

import java.util.Map;

import com.thoughtworks.go.agent.testhelper.FakeBootstrapperServer;
import com.thoughtworks.go.mothers.ServerUrlGeneratorMother;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

@RunWith(FakeBootstrapperServer.class)
public class ServerBinaryDownloaderTest {

    @Test
    public void shouldGetAllHeaders() throws Exception {
        ServerBinaryDownloader downloader = new ServerBinaryDownloader(ServerUrlGeneratorMother.generatorFor("localhost", 9090), DownloadableFile.AGENT);
        Map<String, String> headers = downloader.headers();
        assertNotNull(headers.get("Content-MD5"));
        assertThat(headers.get("Cruise-Server-Ssl-Port"), is("9443"));
    }

    @Test
    public void shouldDownloadTheBinaryAlwaysIrrespectiveOfLocalFileChange() {
        ServerBinaryDownloader downloader = new ServerBinaryDownloader(ServerUrlGeneratorMother.generatorFor("localhost", 9090), DownloadableFile.AGENT);
        ServerBinaryDownloader.DownloadResult result = downloader.downloadAlways();
        assertThat(result.performedDownload,is(true));
        result = downloader.downloadAlways();
        assertThat(result.performedDownload,is(true));
    }

}
