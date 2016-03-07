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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.thoughtworks.go.agent.ServerUrlGenerator;
import com.thoughtworks.go.agent.common.util.Downloader;
import com.thoughtworks.go.util.FileDigester;
import com.thoughtworks.go.util.PerfTimer;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.HeadMethod;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.NullOutputStream;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class ServerBinaryDownloader implements Downloader {
    private static final Log LOG = LogFactory.getLog(ServerBinaryDownloader.class);
    private static final String MD5_HEADER = "Content-MD5";
    private static final String SSL_PORT_HEADER = "Cruise-Server-Ssl-Port";
    private final ServerUrlGenerator urlGenerator;
    private String md5 = null;
    private DownloadableFile downloadableFile;

    public static final class DownloadResult {
        public final boolean performedDownload;
        private final Map<String, String> headers;
        private final ServerUrlGenerator urlGenerator;

        public DownloadResult(boolean performedDownload, Map<String, String> headers, ServerUrlGenerator urlGenerator) {
            this.performedDownload = performedDownload;
            this.headers = headers;
            this.urlGenerator = urlGenerator;
        }

        public String serverBaseUrl() {
            return urlGenerator.serverSslBaseUrl(Integer.parseInt(headers.get(SSL_PORT_HEADER)));
        }
    }

    public ServerBinaryDownloader(ServerUrlGenerator urlGenerator, final DownloadableFile downloadableFile) {
        this.urlGenerator = urlGenerator;
        this.downloadableFile = downloadableFile;
    }

    public String md5() {
        return md5;
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
        return new DownloadResult(downloaded, null, urlGenerator);
    }

    public DownloadResult downloadIfNecessary() {
        synchronized (downloadableFile.mutex()) {
            Map<String, String> headers = new HashMap<String, String>();
            boolean updated = false;
            boolean downloaded = false;
            while (!updated) {
                try {
                    headers = headers();
                    File localFile = new File(downloadableFile.getLocalFileName());
                    md5 = headers.get(MD5_HEADER);
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
            return new DownloadResult(downloaded, headers, urlGenerator);
        }
    }

    Map<String, String> headers() throws Exception {
        Map<String, String> headers = ServerCall.invoke(new HeadMethod(checkUrl())).headers;
        checkHeaders(headers, downloadableFile.url(urlGenerator));
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

    private static void checkHeaders(Map<String, String> headers, String url) {
        if (!headers.containsKey(MD5_HEADER) || !headers.containsKey(SSL_PORT_HEADER)) {
            LOG.error("Contacted server at URL " + url + " but it didn't give me the information I wanted. Please check the hostname and port.");
        }
    }

    private static boolean checksOut(File file, String expectedSignature) {
        FileInputStream input = null;
        try {
            try {
                input = new FileInputStream(file);
                FileDigester fileDigester = new FileDigester(input, new NullOutputStream());
                fileDigester.copy();
                return expectedSignature.equals(fileDigester.md5());
            } finally {
                if (input != null) {
                    input.close();
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private boolean download() throws Exception {
        HttpClient httpClient = new HttpClient();
        HttpMethod method = new GetMethod(checkUrl());
        httpClient.getHostConfiguration().setProxyHost(
                ProxyConfigurator.create(method, System.getProperties()));
        InputStream body = null;
        OutputStream outputFile = null;
        httpClient.setConnectionTimeout(ServerCall.HTTP_TIMEOUT_IN_MILLISECONDS);
        try {
            LOG.info("download started at " + new Date());
            final int status = httpClient.executeMethod(method);
            if (status != 200) {
                throw new Exception("Got status " + status + " " + method.getStatusText() + " from server");
            }
            body = new BufferedInputStream(method.getResponseBodyAsStream());
            LOG.info("got server response at " + new Date());
            outputFile = new BufferedOutputStream(new FileOutputStream(new File(downloadableFile.getLocalFileName())));
            IOUtils.copy(body, outputFile);
            LOG.info("pipe the stream to " + downloadableFile + " at " + new Date());
            return true;
        } catch (Exception e) {
            String message = "Couldn't access Go Server with base url: " + downloadableFile.url(urlGenerator) + ": " + e.toString();
            LOG.error(message);
            throw new Exception(message, e);
        } finally {
            IOUtils.closeQuietly(body);
            IOUtils.closeQuietly(outputFile);
            method.releaseConnection();
        }
    }
}
