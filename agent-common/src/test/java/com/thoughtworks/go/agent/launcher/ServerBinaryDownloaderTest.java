/*
 * Copyright Thoughtworks, Inc.
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
import com.thoughtworks.go.agent.testhelper.FakeGoServerExtension;
import com.thoughtworks.go.agent.testhelper.GoTestResource;
import com.thoughtworks.go.mothers.ServerUrlGeneratorMother;
import com.thoughtworks.go.util.SslVerificationMode;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.CloseableHttpClient;
import org.assertj.core.util.Hexadecimals;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(FakeGoServerExtension.class)
public class ServerBinaryDownloaderTest {

    @GoTestResource
    public FakeGoServer server;

    @AfterEach
    public void tearDown() throws IOException {
        Files.deleteIfExists(new File(Downloader.AGENT_BINARY).toPath());
        Files.deleteIfExists(DownloadableFile.AGENT.getLocalFile().toPath());
    }

    @Test
    public void shouldSetMd5AndSSLPortHeaders() throws Exception {
        ServerBinaryDownloader downloader = new ServerBinaryDownloader(new GoAgentServerHttpClientBuilder(null, SslVerificationMode.NONE, null, null, null), ServerUrlGeneratorMother.generatorFor("localhost", server.getPort()));
        downloader.downloadIfNecessary(DownloadableFile.AGENT);

        MessageDigest digester = MessageDigest.getInstance("MD5");
        try (DigestInputStream digest = new DigestInputStream(new FileInputStream(DownloadableFile.AGENT.getLocalFile()), digester)) {
            digest.transferTo(OutputStream.nullOutputStream());
        }
        assertThat(downloader.getMd5()).isEqualTo(Hexadecimals.toHexString(digester.digest()).toLowerCase());
    }

    @Test
    public void shouldGetExtraPropertiesFromHeader() {
        assertExtraProperties("", new HashMap<>());

        assertExtraProperties("Key1=Value1 key2=value2",
            Map.of("Key1", "Value1", "key2", "value2"));

        assertExtraProperties("Key1=Value1 key2=value2 key2=value3",
            Map.of("Key1", "Value1", "key2", "value2"));

        assertExtraProperties("Key1%20WithSpace=Value1%20WithSpace key2=value2",
            Map.of("Key1 WithSpace", "Value1 WithSpace", "key2", "value2"));
    }

    @Test
    public void shouldNotFailIfExtraPropertiesAreNotFormattedProperly() {
        assertExtraProperties("abc", new HashMap<>());
    }

    @Test
    public void shouldDownloadAgentJarFile() {
        ServerBinaryDownloader downloader = new ServerBinaryDownloader(new GoAgentServerHttpClientBuilder(null, SslVerificationMode.NONE, null, null, null), ServerUrlGeneratorMother.generatorFor("localhost", server.getPort()));
        assertThat(DownloadableFile.AGENT.doesNotExist()).isEqualTo(true);
        downloader.downloadIfNecessary(DownloadableFile.AGENT);
        assertThat(DownloadableFile.AGENT.getLocalFile().exists()).isEqualTo(true);
    }

    @Test
    public void shouldReturnTrueIfTheFileIsDownloaded() {
        ServerBinaryDownloader downloader = new ServerBinaryDownloader(new GoAgentServerHttpClientBuilder(null, SslVerificationMode.NONE, null, null, null), ServerUrlGeneratorMother.generatorFor("localhost", server.getPort()));
        assertThat(downloader.downloadIfNecessary(DownloadableFile.AGENT)).isEqualTo(true);
    }

    @Test
    public void shouldThrowExceptionIfTheServerIsDown() {
        ServerBinaryDownloader downloader = new ServerBinaryDownloader(new GoAgentServerHttpClientBuilder(null, SslVerificationMode.NONE, null, null, null), ServerUrlGeneratorMother.generatorFor("locahost", server.getPort()));
        assertThatThrownBy(() -> downloader.download(DownloadableFile.AGENT))
                .isExactlyInstanceOf(UnknownHostException.class);
    }

    @Test
    public void shouldFailIfMD5HeadersAreMissing() {
        ServerBinaryDownloader downloader = new ServerBinaryDownloader(new GoAgentServerHttpClientBuilder(null, SslVerificationMode.NONE, null, null, null), ServerUrlGeneratorMother.generatorWithoutSubPathFor("https://localhost:" + server.getSecurePort() + "/go/hello"));
        assertThatThrownBy(() -> downloader.fetchUpdateCheckHeaders(DownloadableFile.AGENT))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("Missing required headers 'Content-MD5' in response.");
    }

    @Test
    public void shouldFailIfServerIsNotAvailable() {
        ServerBinaryDownloader downloader = new ServerBinaryDownloader(new GoAgentServerHttpClientBuilder(null, SslVerificationMode.NONE, null, null, null), ServerUrlGeneratorMother.generatorWithoutSubPathFor("https://invalidserver:" + server.getSecurePort() + "/go"));
        assertThatThrownBy(() -> downloader.fetchUpdateCheckHeaders(DownloadableFile.AGENT))
                .isExactlyInstanceOf(UnknownHostException.class)
                .hasMessageContaining("invalidserver");
    }

    @Test
    public void shouldThrowExceptionInCaseOf404() {
        ServerBinaryDownloader downloader = new ServerBinaryDownloader(new GoAgentServerHttpClientBuilder(null, SslVerificationMode.NONE, null, null, null), ServerUrlGeneratorMother.generatorWithoutSubPathFor("https://localhost:" + server.getSecurePort() + "/go/not-found"));
        assertThatThrownBy(() -> downloader.download(DownloadableFile.AGENT))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("This agent might be incompatible with your GoCD Server. Please fix the version mismatch between GoCD Server and GoCD Agent.");
    }

    @Test
    public void shouldReturnFalseIfTheServerDoesNotRespondWithEntity() throws Exception {
        GoAgentServerHttpClientBuilder builder = mock(GoAgentServerHttpClientBuilder.class);
        CloseableHttpClient closeableHttpClient = mock(CloseableHttpClient.class);
        when(builder.build()).thenReturn(closeableHttpClient);
        CloseableHttpResponse httpResponse = mock(CloseableHttpResponse.class);
        when(closeableHttpClient.execute(any(HttpRequestBase.class))).thenReturn(httpResponse);
        ServerBinaryDownloader downloader = new ServerBinaryDownloader(builder, ServerUrlGeneratorMother.generatorFor("localhost", server.getPort()));
        assertThat(downloader.download(DownloadableFile.AGENT)).isEqualTo(false);
    }

    private void assertExtraProperties(String valueToSet, Map<String, String> expectedValue) {
        ServerBinaryDownloader downloader = new ServerBinaryDownloader(new GoAgentServerHttpClientBuilder(null, SslVerificationMode.NONE, null, null, null), ServerUrlGeneratorMother.generatorFor("localhost", server.getPort()));
        try {
            server.setExtraPropertiesHeaderValue(valueToSet);
            downloader.downloadIfNecessary(DownloadableFile.AGENT);

            assertThat(downloader.getExtraProperties()).isEqualTo(expectedValue);
        } finally {
            server.setExtraPropertiesHeaderValue(null);
        }
    }
}
