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
import com.thoughtworks.go.agent.common.util.Downloader;
import com.thoughtworks.go.util.FileDigester;
import com.thoughtworks.go.util.PerfTimer;
import com.thoughtworks.go.util.SslVerificationMode;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.NullOutputStream;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static java.lang.String.format;

public class ServerBinaryDownloader implements Downloader {

    private final ServerCall serverCall = new ServerCall();

    private static final Log LOG = LogFactory.getLog(ServerBinaryDownloader.class);
    private static final String MD5_HEADER = "Content-MD5";
    @Deprecated // for backward compatibility
    private static final String SSL_PORT_HEADER = "Cruise-Server-Ssl-Port";
    private final ServerUrlGenerator urlGenerator;
    private File rootCertFile;
    private String md5 = null;
    private DownloadableFile downloadableFile;
    private SslVerificationMode sslVerificationMode;
    private String sslPort;

    public static final class DownloadResult {
        public final boolean performedDownload;

        public DownloadResult(boolean performedDownload) {
            this.performedDownload = performedDownload;
        }

    }

    public ServerBinaryDownloader(ServerUrlGenerator urlGenerator, final DownloadableFile downloadableFile, File rootCertFile, SslVerificationMode sslVerificationMode) {
        this.rootCertFile = rootCertFile;
        this.urlGenerator = urlGenerator;
        this.downloadableFile = downloadableFile;
        this.sslVerificationMode = sslVerificationMode;
    }

    public String md5() {
        return md5;
    }

    public String sslPort() {
        return sslPort;
    }

    public DownloadResult downloadAlways() {
        boolean downloaded = false;
        synchronized (downloadableFile.mutex()) {
            while (!downloaded) {
                try {
                    PerfTimer timer = PerfTimer.start("Downloading new " + downloadableFile + " with md5 signature: " + md5);
                    downloaded = download();
                    timer.stop();
                } catch (Exception e) {
                    LOG.error("Couldn't update " + downloadableFile + ". Sleeping for 1m. Error: " + e.toString());
                    try {
                        int period = Integer.parseInt(System.getProperty("sleep.for.download", "60000"));
                        Thread.sleep(period);
                    } catch (InterruptedException ie) { /* we don't care. Stupid checked exception.*/ }
                }
            }
        }
        return new DownloadResult(downloaded);
    }

    public boolean downloadIfNecessary() {
        synchronized (downloadableFile.mutex()) {
            Map<String, String> headers = new HashMap<>();
            boolean updated = false;
            boolean downloaded = false;
            while (!updated) {
                try {
                    headers = headers();
                    File localFile = new File(downloadableFile.getLocalFileName());
                    md5 = headers.get(MD5_HEADER);
                    sslPort = headers.get(SSL_PORT_HEADER);
                    if (!localFile.exists() || !checksOut(localFile, md5)) {
                        PerfTimer timer = PerfTimer.start("Downloading new " + downloadableFile + " with md5 signature: " + md5);
                        downloaded = download();
                        timer.stop();
                    }
                    updated = true;
                } catch (Exception e) {
                    e.printStackTrace();
                    LOG.error("Couldn't update " + downloadableFile + ". Sleeping for 1m. Error: " + e.toString());
                    try {
                        int period = Integer.parseInt(System.getProperty("sleep.for.download", "60000"));
                        Thread.sleep(period);
                    } catch (InterruptedException ie) { /* we don't care. Stupid checked exception.*/ }
                }
            }
            return downloaded;
        }
    }

    Map<String, String> headers() throws Exception {
        Map<String, String> headers = serverCall.invoke(new HttpHead(checkUrl()), rootCertFile, sslVerificationMode).headers;
        if (!headers.containsKey(MD5_HEADER)) {
            LOG.error(format("Contacted server at URL %s but the server did not send back a response containing the header %s", downloadableFile.url(urlGenerator), MD5_HEADER));
        }
        return headers;
    }

    private String checkUrl() {
        String url = downloadableFile.url(urlGenerator);
        try {
            new URL(url);
        } catch (MalformedURLException mue) {
            throw new RuntimeException(
                    "URL you provided to access Go Server: " + downloadableFile.url(urlGenerator) + " is not valid");
        }
        return url;
    }

    private static boolean checksOut(File file, String expectedSignature) {
        try (FileInputStream input = new FileInputStream(file)) {
            FileDigester fileDigester = new FileDigester(input, new NullOutputStream());
            fileDigester.copy();
            return expectedSignature.equals(fileDigester.md5());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private boolean download() throws Exception {
        InputStream body = null;
        File toDownload = new File(downloadableFile.getLocalFileName());
        try (OutputStream outputFile = new BufferedOutputStream(new FileOutputStream(toDownload))) {
            LOG.info("download of " + toDownload + " started at " + new Date());
            ServerCall.ServerResponseWrapper invoke = serverCall.invoke(new HttpGet(checkUrl()), rootCertFile, sslVerificationMode);
            body = invoke.body;
            LOG.info("got server response at " + new Date());
            IOUtils.copy(body, outputFile);
            LOG.info("pipe the stream to " + downloadableFile + " at " + new Date());
            return true;
        } catch (Exception e) {
            String message = "Couldn't access Go Server with base url: " + downloadableFile.url(urlGenerator) + ": " + e.toString();
            LOG.error(message);
            throw new Exception(message, e);
        } finally {
            IOUtils.closeQuietly(body);
        }
    }
}
