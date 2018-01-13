/*
 * Copyright 2018 ThoughtWorks, Inc.
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

package com.thoughtworks.go.server.api;

import org.apache.commons.lang.StringUtils;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.springframework.http.HttpStatus;
import spark.Request;
import spark.Response;

import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.apache.commons.lang.StringUtils.isBlank;
import static spark.Spark.halt;

public abstract class BaseController implements ControllerMethods {
    private static final Set<String> UPDATE_HTTP_METHODS = new HashSet<>(Arrays.asList("PUT", "POST", "PATCH"));

    protected final ApiVersion apiVersion;
    protected final String mimeType;

    protected BaseController(ApiVersion apiVersion) {
        this.apiVersion = apiVersion;
        this.mimeType = apiVersion.mimeType();
    }

    public String controllerPath(String... paths) {
        if (paths == null || paths.length == 0) {
            return controllerBasePath();
        } else {
            return (controllerBasePath() + "/" + StringUtils.join(paths, '/')).replaceAll("//", "/");
        }
    }

    public String controllerPath(Map<String, String> params) {
        if (params == null || params.isEmpty()) {
            return controllerBasePath();
        } else {
            List<BasicNameValuePair> queryParams = params.entrySet().stream().map(new Function<Map.Entry<String, String>, BasicNameValuePair>() {
                @Override
                public BasicNameValuePair apply(Map.Entry<String, String> entry) {
                    return new BasicNameValuePair(entry.getKey(), entry.getValue());
                }
            }).collect(Collectors.toList());
            return controllerBasePath() + '?' + URLEncodedUtils.format(queryParams, "utf-8");
        }
    }

    protected abstract String controllerBasePath();

    public abstract void setupRoutes();

    protected void setContentType(Request req, Response res) {
        res.raw().setCharacterEncoding("utf-8");
        res.header("Content-Type", mimeType);
    }

    protected String messageJson(Exception ex) {
        return MessageJson.create(ex.getMessage());
    }

    protected void verifyContentType(Request request, Response response) {
        if (!UPDATE_HTTP_METHODS.contains(request.requestMethod().toUpperCase())) {
            return;
        }

        if (request.contentLength() >= 1 && !isJsonContentType(request)) {
            throw halt(HttpStatus.UNSUPPORTED_MEDIA_TYPE.value(), MessageJson.create("You must specify a 'Content-Type' of 'application/json'"));
        }

        if ("chunked".equalsIgnoreCase(request.headers("Transfer-Encoding")) && !isJsonContentType(request)) {
            throw halt(HttpStatus.UNSUPPORTED_MEDIA_TYPE.value(), MessageJson.create("You must specify a 'Content-Type' of 'application/json'"));
        }
    }

    protected boolean isJsonContentType(Request request) {
        String mime = request.headers("Content-Type");
        if (isBlank(mime)) {
            return false;
        }
        try {
            return new MimeType(mime).getBaseType().equals("application/json");
        } catch (MimeTypeParseException e) {
            return false;
        }
    }

    public String getMimeType() {
        return mimeType;
    }
}
