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

package com.thoughtworks.go.api;

import com.google.gson.reflect.TypeToken;
import com.thoughtworks.go.api.util.GsonTransformer;
import com.thoughtworks.go.api.util.MessageJson;
import com.thoughtworks.go.spark.SparkController;
import spark.Request;
import spark.Response;

import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;
import java.io.IOException;
import java.util.*;

import static com.thoughtworks.go.api.util.HaltApiResponses.haltBecauseConfirmHeaderMissing;
import static com.thoughtworks.go.api.util.HaltApiResponses.haltBecauseJsonContentTypeExpected;
import static org.apache.commons.lang.StringUtils.isBlank;

public abstract class ApiController implements ControllerMethods, SparkController {
    private static final Set<String> UPDATE_HTTP_METHODS = new HashSet<>(Arrays.asList("PUT", "POST", "PATCH"));

    protected final ApiVersion apiVersion;
    protected final String mimeType;

    protected ApiController(ApiVersion apiVersion) {
        this.apiVersion = apiVersion;
        this.mimeType = apiVersion.mimeType();
    }

    protected void setContentType(Request req, Response res) {
        res.raw().setCharacterEncoding("utf-8");
        res.header("Content-Type", mimeType);
    }

    protected String messageJson(Exception ex) {
        return MessageJson.create(ex.getMessage());
    }

    protected void verifyContentType(Request request, Response response) throws IOException {
        if (!UPDATE_HTTP_METHODS.contains(request.requestMethod().toUpperCase())) {
            return;
        }

        boolean requestHasBody = request.contentLength() >= 1 || request.raw().getInputStream().available() >= 1 || "chunked".equalsIgnoreCase(request.headers("Transfer-Encoding"));

        if (requestHasBody) {
            if (!isJsonContentType(request)) {
                throw haltBecauseJsonContentTypeExpected();
            }
        } else if (request.headers().stream().noneMatch(headerName -> headerName.toLowerCase().equals("x-gocd-confirm"))) {
            throw haltBecauseConfirmHeaderMissing();
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

    protected Map<String, Object> readRequestBodyAsJSON(Request req) {
        Map<String, Object> map = GsonTransformer.getInstance().fromJson(req.body(), new TypeToken<Map<String, Object>>() {
        }.getType());
        if (map == null) {
            return Collections.emptyMap();
        }
        return map;
    }

}
