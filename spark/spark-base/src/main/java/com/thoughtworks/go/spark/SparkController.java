/*
 * Copyright 2019 ThoughtWorks, Inc.
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
package com.thoughtworks.go.spark;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.policy.SupportedAction;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.newsecurity.utils.SessionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import spark.Request;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public interface SparkController {
    default String controllerPath(Object... paths) {
        if (paths == null || paths.length == 0) {
            return controllerBasePath();
        } else {
            return (controllerBasePath() + "/" + StringUtils.join(paths, '/')).replaceAll("//", "/");
        }
    }

    default String controllerPath(Map<String, String> params, Object... paths) {
        String path = controllerPath(paths);

        if (params == null || params.isEmpty()) {
            return path;
        } else {
            List<BasicNameValuePair> queryParams = params.entrySet().stream().map(entry -> new BasicNameValuePair(entry.getKey(), entry.getValue())).collect(Collectors.toList());
            return path + '?' + URLEncodedUtils.format(queryParams, "utf-8");
        }
    }

    String controllerBasePath();

    void setupRoutes();

    default Username currentUsername() {
        return SessionUtils.currentUsername();
    }

    default SupportedAction getAction(Request request) {
        switch (request.requestMethod()) {
            case "GET":
                return SupportedAction.VIEW;
            case "HEAD":
                return SupportedAction.VIEW;
            case "POST":
                return SupportedAction.ADMINISTER;
            case "DELETE":
                return SupportedAction.ADMINISTER;
            case "PUT":
                return SupportedAction.EDIT;
            case "PATCH":
                return SupportedAction.EDIT;
            default:
                return SupportedAction.UNKNOWN;
        }
    }

    default String currentUsernameString() {
        return currentUserLoginName().toString();
    }

    default Long currentUserId(Request request) {
        return SessionUtils.getUserId(request.raw());
    }

    default CaseInsensitiveString currentUserLoginName() {
        return currentUsername().getUsername();
    }
}
