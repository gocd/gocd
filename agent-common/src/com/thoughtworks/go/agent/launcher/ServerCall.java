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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @understands calling any server url
 */
public class ServerCall {
    private static final Log LOG = LogFactory.getLog(ServerCall.class);
    public static final int HTTP_TIMEOUT_IN_MILLISECONDS = 5000;

    public static ServerResponseWrapper invoke(HttpMethod method) throws Exception {
        HashMap<String, String> headers = new HashMap<String, String>();
        HttpClient httpClient = new HttpClient();
        httpClient.setConnectionTimeout(HTTP_TIMEOUT_IN_MILLISECONDS);
        final String httpProxyHost = System.getProperty("http.proxyHost");
        final String httpProxyPort = System.getProperty("http.proxyPort");
        final String httpsProxyHost = System.getProperty("https.proxyHost");
        final String httpsProxyPort = System.getProperty("https.proxyPort");
        final String nonProxyHosts = System.getProperty("http.nonProxyHosts");
        httpClient.getHostConfiguration().setProxyHost(
                ProxyConfigurator.create(method, httpProxyHost, httpProxyPort, httpsProxyHost, httpsProxyPort, nonProxyHosts));
        try {
            final int status = httpClient.executeMethod(method);
            if (status == HttpStatus.SC_NOT_FOUND) {
                StringWriter sw = new StringWriter();
                PrintWriter out = new PrintWriter(sw);
                out.println("Return Code: " + status);
                out.println("Few Possible Causes: ");
                out.println("1. Your Go Server is down or not accessible.");
                out.println("2. This agent might be incompatible with your Go Server.Please fix the version mismatch between Go Server and Go Agent.");
                out.close();
                throw new Exception(sw.toString());
            }
            if (status != HttpStatus.SC_OK) {
                throw new Exception("Got status " + status + " " + method.getStatusText() + " from server");
            }
            for (Header header : method.getResponseHeaders()) {
                headers.put(header.getName(), header.getValue());
            }
            return new ServerResponseWrapper(headers, method.getResponseBodyAsStream());
        } catch (Exception e) {
            String message = "Couldn't access Go Server with base url: " + method.getURI() + ": " + e.toString();
            LOG.error(message);
            throw new Exception(message, e);
        } finally {
            method.releaseConnection();
        }
    }

    public static class ServerResponseWrapper {
        public final Map<String, String> headers;
        public final InputStream body;

        public ServerResponseWrapper(Map<String, String> headers, InputStream body) throws IOException {
            this.headers = Collections.unmodifiableMap(headers);
            this.body = toByteArrayInputStream(body);
        }

        private InputStream toByteArrayInputStream(InputStream body) throws IOException {
            if (body == null) {
                return null;
            }
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            IOUtils.copy(body, baos);
            baos.close();
            return new ByteArrayInputStream(baos.toByteArray());
        }
    }
}
