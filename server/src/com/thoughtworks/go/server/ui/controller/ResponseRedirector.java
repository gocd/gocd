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

package com.thoughtworks.go.server.ui.controller;

import java.util.Map;
import java.net.URLEncoder;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.servlet.View;

public class ResponseRedirector implements View {
    private final String target;

    public ResponseRedirector(String target) {
        this.target = target;
    }

    public String getContentType() {
        return "text/html";
    }

    public void render(Map model, HttpServletRequest request, HttpServletResponse response) throws Exception {
        String contextpath = request.getContextPath();
        StringBuilder params = new StringBuilder();
        Map queryString = (Map) model.get("params");
        if (queryString != null) {
            for (Object key : queryString.keySet()) {
                String value = (String) queryString.get(key);
                if (value != null) {
                    if (params.length() > 0) {
                        params.append("&");
                    }
                    params.append(key).append("=").append(URLEncoder.encode(value, "UTF-8"));
                }
            }
        }
        String paramString = (params.length() == 0) ? "" : "?" + params;
        response.sendRedirect(contextpath + "/tab" + target + paramString);
    }

    public String getTarget() {
        return target;
    }

    public String toString() {
        return "ResponseRedirector["
                + target + "]";
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ResponseRedirector that = (ResponseRedirector) o;

        if (target != null ? !target.equals(that.target) : that.target != null) {
            return false;
        }

        return true;
    }

    public int hashCode() {
        return (target != null ? target.hashCode() : 0);
    }
}
