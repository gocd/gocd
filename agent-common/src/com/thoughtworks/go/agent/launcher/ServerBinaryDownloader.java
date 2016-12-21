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

import com.thoughtworks.go.agent.ServerUrlGenerator;
import com.thoughtworks.go.agent.common.ssl.GoAgentServerHttpClientBuilder;
import com.thoughtworks.go.agent.common.util.Downloader;
import com.thoughtworks.go.util.PerfTimer;
import com.thoughtworks.go.util.SslVerificationMode;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.api.Response;
import org.eclipse.jetty.client.util.InputStreamResponseListener;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.http.HttpStatus;

import java.io.*;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class ServerBinaryDownloader implements Downloader {
    private static final File TEMP_DIR = new File("data/server-binary-downloads");

    private static final Log LOG = LogFactory.getLog(ServerBinaryDownloader.class);
    public static final int MAX_BUFFER_SIZE = 16 * 1024;
    private final ServerUrlGenerator urlGenerator;
    private String md5 = null;
    private String sslPort;

    private static final String MD5_HEADER = "Content-MD5";
    @Deprecated // for backward compatibility
    private static final String SSL_PORT_HEADER = "Cruise-Server-Ssl-Port";
    private GoAgentServerHttpClientBuilder httpClientBuilder;

    static {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                FileUtils.deleteQuietly(TEMP_DIR);
            }
        });
    }

    public ServerBinaryDownloader(ServerUrlGenerator urlGenerator, File rootCertFile, SslVerificationMode sslVerificationMode) throws Exception {
        this(new GoAgentServerHttpClientBuilder(rootCertFile, sslVerificationMode), urlGenerator);
    }

    protected ServerBinaryDownloader(GoAgentServerHttpClientBuilder httpClientBuilder, ServerUrlGenerator urlGenerator) {
        this.httpClientBuilder = httpClientBuilder;
        this.urlGenerator = urlGenerator;
    }

    public String getMd5() {
        return md5;
    }

    public String getSslPort() {
        return sslPort;
    }

    public boolean downloadIfNecessary(final DownloadableFile downloadableFile) {
        boolean updated = false;
        boolean downloaded = false;
        while (!updated) {
            try {
                fetchUpdateCheckHeaders(downloadableFile);
                if (downloadableFile.doesNotExist() || !downloadableFile.isChecksumEquals(getMd5())) {
                    PerfTimer timer = PerfTimer.start("Downloading new " + downloadableFile + " with md5 signature: " + md5);
                    downloaded = download(downloadableFile);
                    timer.stop();
                }
                updated = true;
            } catch (Exception e) {
                LOG.error("Couldn't update " + downloadableFile + ". Sleeping for 1m. Error: ", e);
                try {
                    int period = Integer.parseInt(System.getProperty("sleep.for.download", "60000"));
                    Thread.sleep(period);
                } catch (InterruptedException ie) { /* we don't care. Stupid checked exception.*/ }
            }
        }
        return downloaded;
    }

    void fetchUpdateCheckHeaders(DownloadableFile downloadableFile) throws Exception {
        String url = downloadableFile.validatedUrl(urlGenerator);

        HttpClient httpClient = httpClientBuilder.build();
        try {
            Request request = httpClient.newRequest(url)
                    .method(HttpMethod.HEAD)
                    .timeout(5, TimeUnit.SECONDS)
                    .onResponseHeaders(this::abortRequestIfBadResponse);
            ContentResponse response = request.send();
            this.md5 = response.getHeaders().get(MD5_HEADER);
            this.sslPort = response.getHeaders().get(SSL_PORT_HEADER);
        } finally {
            httpClient.stop();
        }

    }

    protected synchronized boolean download(final DownloadableFile downloadableFile) throws Exception {
        File toDownload = downloadableFile.getLocalFile();
        String url = downloadableFile.url(urlGenerator);

        return download(url, toDownload);
    }

    protected boolean download(String url, File toDownload) throws Exception {
        LOG.info("Downloading " + toDownload);
        HttpClient httpClient = httpClientBuilder.build();

        try {
            Request request = httpClient.newRequest(url)
                    .method(HttpMethod.GET)
                    .idleTimeout(5, TimeUnit.SECONDS);

            InputStreamResponseListener listener = new InputStreamResponseListener(MAX_BUFFER_SIZE);
            request.send(listener);
            Response response = listener.get(5, TimeUnit.SECONDS);
            if (response.getStatus() == 200) {
                return writeViaTemporaryFile(listener, toDownload);
            } else {
                abortRequestIfBadResponse(response);
            }
        } finally {
            httpClient.stop();
        }
        return true;
    }

    private boolean writeViaTemporaryFile(InputStreamResponseListener listener, File toDownload) throws IOException {
        TEMP_DIR.mkdirs();
        File tempFile = new File(TEMP_DIR, UUID.randomUUID().toString() + ".jar");
        try (
                OutputStream outStream = new BufferedOutputStream(new FileOutputStream(tempFile));
                InputStream inputStream = listener.getInputStream()
        ) {
            IOUtils.copy(inputStream, outStream, MAX_BUFFER_SIZE);
        }

        if (tempFile.length() == 0) {
            tempFile.delete();
            return false;
        } else {
            FileUtils.deleteQuietly(toDownload);
            FileUtils.moveFile(tempFile, toDownload);
            return true;
        }
    }

    private void abortRequestIfBadResponse(Response response) {
        StringWriter sw = new StringWriter();
        try (PrintWriter out = new PrintWriter(sw)) {
            out.print("Problem accessing server at ");
            out.print(response.getRequest().getURI());
            out.println(" the status was " + response.getStatus());
            if (response.getStatus() != HttpStatus.OK_200) {
                LOG.info("Response code: " + response.getStatus());
                out.println("Few Possible Causes: ");
                out.println("1. Your Go Server is down or not accessible.");
                out.println("2. This agent might be incompatible with your Go Server. Please fix the version mismatch between Go Server and Go Agent.");
                RuntimeException cause = new RuntimeException(sw.toString());
                response.abort(cause);
                throw cause;
            } else if (response.getHeaders().get(MD5_HEADER) == null || response.getHeaders().get(SSL_PORT_HEADER) == null) {
                out.print("Missing required headers '");
                out.print(MD5_HEADER);
                out.print("' and '");
                out.print(SSL_PORT_HEADER);
                out.println("' in response.");
                RuntimeException cause = new RuntimeException(sw.toString());
                response.abort(cause);

                throw cause;
            }
        }
    }
}
