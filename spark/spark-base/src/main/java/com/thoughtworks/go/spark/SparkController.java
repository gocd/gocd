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
package com.thoughtworks.go.spark;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.policy.SupportedAction;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.newsecurity.utils.SessionUtils;
import com.thoughtworks.go.util.UriEncodingUtil;
import spark.Request;
import spark.route.HttpMethod;

import java.util.Map;

import static org.apache.commons.lang3.StringUtils.join;

public interface SparkController {
    default String getMimeType() {
        throw new UnsupportedOperationException("Mime type must be implemented by this controller to use with this function");
    }

    default String controllerPath(Object... paths) {
        if (paths == null || paths.length == 0) {
            return controllerBasePath();
        } else {
            return (controllerBasePath() + "/" + join(paths, '/')).replace("//", "/");
        }
    }

    default String controllerPath(Map<String, String> params, Object... paths) {
        String path = controllerPath(paths);

        if (params == null || params.isEmpty()) {
            return path;
        } else {
            return path + '?' + UriEncodingUtil.encodeQueryParams(params);
        }
    }

    String controllerBasePath();

    void setupRoutes(GlobalExceptionMapper exceptionMapper);

    default Username currentUsername() {
        return SessionUtils.currentUsername();
    }

    default SupportedAction getAction(Request request) {
        return switch (HttpMethod.get(request.requestMethod().toLowerCase())) {
            case get, head -> SupportedAction.VIEW;
            case post, delete, patch, put -> SupportedAction.ADMINISTER;
            default -> SupportedAction.UNKNOWN;
        };
    }

    default String currentUsernameString() {
        return currentUsernameCis().toString();
    }

    default CaseInsensitiveString currentUsernameCis() {
        return currentUsername().getUsername();
    }

    default Long currentUserId(Request request) {
        return SessionUtils.getUserId(request.raw());
    }
}
