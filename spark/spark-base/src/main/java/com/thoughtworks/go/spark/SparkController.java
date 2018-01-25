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

package com.thoughtworks.go.spark;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.util.UserHelper;
import org.apache.commons.lang.StringUtils;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import spark.Request;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public interface SparkController {
    default String controllerPath(String... paths) {
        if (paths == null || paths.length == 0) {
            return controllerBasePath();
        } else {
            return (controllerBasePath() + "/" + StringUtils.join(paths, '/')).replaceAll("//", "/");
        }
    }

    default String controllerPath(Map<String, String> params) {
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

    String controllerBasePath();

    void setupRoutes();

    default Username currentUsername() {
        return UserHelper.getUserName();
    }

    default Long currentUserId(Request request) {
        return UserHelper.getUserId(request.raw());
    }

    default CaseInsensitiveString currentUserLoginName() {
        return currentUsername().getUsername();
    }
}
