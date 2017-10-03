/*
 * Copyright 2017 ThoughtWorks, Inc.
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

import com.thoughtworks.go.agent.common.ssl.GoAgentServerHttpClientBuilder;
import com.thoughtworks.go.agent.common.util.Downloader;
import com.thoughtworks.go.agent.testhelper.FakeGoServer;
import com.thoughtworks.go.mothers.ServerUrlGeneratorMother;
import com.thoughtworks.go.util.SslVerificationMode;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.NullOutputStream;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import javax.xml.bind.DatatypeConverter;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.net.UnknownHostException;
import java.security.DigestInputStream;
import java.security.MessageDigest;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ServerBinaryDownloaderTest {

    @Rule
    public FakeGoServer server = new FakeGoServer();

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @After
    public void tearDown() throws Exception {
        FileUtils.deleteQuietly(new File(Downloader.AGENT_BINARY));
        FileUtils.deleteQuietly(DownloadableFile.AGENT.getLocalFile());
    }

    @Test
    public void shouldSetMd5AndSSLPortHeaders() throws Exception {
        ServerBinaryDownloader downloader = new ServerBinaryDownloader(ServerUrlGeneratorMother.generatorFor("localhost", server.getPort()), null, SslVerificationMode.NONE);
        downloader.downloadIfNecessary(DownloadableFile.AGENT);

        MessageDigest digester = MessageDigest.getInstance("MD5");
        try (BufferedInputStream stream = new BufferedInputStream(new FileInputStream(DownloadableFile.AGENT.getLocalFile()))) {
            try (DigestInputStream digest = new DigestInputStream(stream, digester)) {
                IOUtils.copy(digest, new NullOutputStream());
            }
            assertThat(downloader.getMd5(), is(DatatypeConverter.printHexBinary(digester.digest()).toLowerCase()));
        }
        assertThat(downloader.getSslPort(), is(String.valueOf(server.getSecurePort())));
    }

    @Test
    public void shouldDownloadAgentJarFile() throws Exception {
        ServerBinaryDownloader downloader = new ServerBinaryDownloader(ServerUrlGeneratorMother.generatorFor("localhost", server.getPort()), null, SslVerificationMode.NONE);
        assertThat(DownloadableFile.AGENT.doesNotExist(), is(true));
        downloader.downloadIfNecessary(DownloadableFile.AGENT);
        assertThat(DownloadableFile.AGENT.getLocalFile().exists(), is(true));
    }

    @Test
    public void shouldReturnTrueIfTheFileIsDownloaded() throws Exception {
        ServerBinaryDownloader downloader = new ServerBinaryDownloader(ServerUrlGeneratorMother.generatorFor("localhost", server.getPort()), null, SslVerificationMode.NONE);
        assertThat(downloader.downloadIfNecessary(DownloadableFile.AGENT), is(true));
    }

    @Test(expected = Exception.class)
    public void shouldThrowExceptionIfTheServerIsDown() throws Exception {
        ServerBinaryDownloader downloader = new ServerBinaryDownloader(ServerUrlGeneratorMother.generatorFor("locahost", server.getPort()), null, SslVerificationMode.NONE);
        downloader.download(DownloadableFile.AGENT);
    }

    @Test
    public void shouldConnectToAnSSLServerWithSelfSignedCertWhenInsecureModeIsNoVerifyHost() throws Exception {
        ServerBinaryDownloader downloader = new ServerBinaryDownloader(ServerUrlGeneratorMother.generatorFor("localhost", server.getPort()), new File("testdata/test_cert.pem"),
                SslVerificationMode.NO_VERIFY_HOST);
        downloader.download(DownloadableFile.AGENT);
        assertThat(DownloadableFile.AGENT.getLocalFile().exists(), is(true));
    }

    @Test
    public void shouldRaiseExceptionWhenSelfSignedCertDoesNotMatchTheHostName() throws Exception {
        exception.expect(Exception.class);
        exception.expectMessage("Host name 'localhost' does not match the certificate subject provided by the peer");

        ServerBinaryDownloader downloader = new ServerBinaryDownloader(ServerUrlGeneratorMother.generatorFor("https://localhost:" + server.getSecurePort() + "/go/hello"), new File("testdata/test_cert.pem"),
                SslVerificationMode.FULL);
        downloader.download(DownloadableFile.AGENT);
    }

    @Test
    public void shouldFailIfMD5HeadersAreMissing() throws Exception {
        exception.expect(Exception.class);
        exception.expectMessage("Missing required headers 'Content-MD5' and 'Cruise-Server-Ssl-Port' in response.");

        ServerBinaryDownloader downloader = new ServerBinaryDownloader(ServerUrlGeneratorMother.generatorWithoutSubPathFor("https://localhost:" + server.getSecurePort() + "/go/hello"), null,
                SslVerificationMode.NONE);
        downloader.fetchUpdateCheckHeaders(DownloadableFile.AGENT);
    }

    @Test
    public void shouldFailIfServerIsNotAvailable() throws Exception {
        exception.expect(UnknownHostException.class);
        exception.expectMessage("invalidserver");

        ServerBinaryDownloader downloader = new ServerBinaryDownloader(ServerUrlGeneratorMother.generatorWithoutSubPathFor("https://invalidserver:" + server.getSecurePort() + "/go/hello"), null,
                SslVerificationMode.NONE);
        downloader.fetchUpdateCheckHeaders(DownloadableFile.AGENT);
    }

    @Test
    public void shouldThrowExceptionInCaseOf404() throws Exception {
        exception.expect(Exception.class);
        exception.expectMessage("This agent might be incompatible with your Go Server."
                + " Please fix the version mismatch between Go Server and Go Agent.");

        ServerBinaryDownloader downloader = new ServerBinaryDownloader(ServerUrlGeneratorMother.generatorWithoutSubPathFor("https://localhost:" + server.getSecurePort() + "/go/not-found"), null,
                SslVerificationMode.NONE);
        downloader.download(DownloadableFile.AGENT);
    }

    @Test
    public void shouldReturnFalseIfTheServerDoesNotRespondWithEntity() throws Exception {
        GoAgentServerHttpClientBuilder builder = mock(GoAgentServerHttpClientBuilder.class);
        CloseableHttpClient closeableHttpClient = mock(CloseableHttpClient.class);
        when(builder.build()).thenReturn(closeableHttpClient);
        CloseableHttpResponse httpResponse = mock(CloseableHttpResponse.class);
        when(closeableHttpClient.execute(any(HttpRequestBase.class))).thenReturn(httpResponse);
        ServerBinaryDownloader downloader = new ServerBinaryDownloader(builder, ServerUrlGeneratorMother.generatorFor("localhost", server.getPort()));
        assertThat(downloader.download(DownloadableFile.AGENT), is(false));
    }
}
