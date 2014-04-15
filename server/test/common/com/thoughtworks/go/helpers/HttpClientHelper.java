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

package com.thoughtworks.go.helpers;

import java.io.IOException;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 *
 */
public class HttpClientHelper {

    public static final String DEFAULT_URL = "http://localhost:7493/go/";
    private String baseUrl = DEFAULT_URL;

    public HttpClientHelper() {
    }

    public HttpClientHelper(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String httpRequest(final String path, RequestMethod methodRequired) throws Exception {
        return httpRequest(path, methodRequired, null);
    }

    public String httpRequest(final String path, RequestMethod methodRequired, String params) throws Exception {
        HttpMethod method = doRequest(path, methodRequired, params);
        return method.getResponseBodyAsString();
    }


    public int httpRequestForHeaders(final String path, RequestMethod methodRequired)
            throws Exception {
        HttpMethod method = doRequest(path, methodRequired, "");
        return method.getStatusCode();

    }

    private HttpMethod doRequest(String path, RequestMethod methodRequired, String params) throws IOException {
        HttpMethod method = null;
        String url = baseUrl + path;
        switch (methodRequired) {
            case PUT:
                method = new PutMethod(url);
                break;
            case POST:
                method = new PostMethod(url);
                break;
            case GET:
                method = new GetMethod(url);
                break;
        }
        method.setQueryString(params);
        HttpClient client = new HttpClient();
        client.executeMethod(method);
        return method;
    }

    public static boolean isRedirect(int statusCode) {
        return statusCode >= 300 && statusCode < 400;
    }
}
