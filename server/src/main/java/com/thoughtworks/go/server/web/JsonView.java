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

package com.thoughtworks.go.server.web;

import com.thoughtworks.go.util.json.JsonFakeMap;
import org.springframework.web.servlet.view.AbstractView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class JsonView extends AbstractView {

    private static final String RENDER_DIRECT = " ";
    private GoRequestContext requestContext;

    public JsonView() {
    }

    public JsonView(GoRequestContext requestContext) {
        this.requestContext = requestContext;
    }

    //TODO: ChrisS - this should just be a normal HashMap
    public static JsonFakeMap asMap(Object json) {
        return new JsonFakeMap(json);
    }

    protected void renderMergedOutputModel(Map map, HttpServletRequest httpServletRequest,
                                           HttpServletResponse httpServletResponse) throws Exception {
        if (requestContext == null) {
            //TODO requestContext may already exist in request; need to check it
            requestContext = new GoRequestContext(httpServletRequest);
        }
        Object json = map.get("json");

        PrintWriter writer = httpServletResponse.getWriter();
        JsonRenderer.render(json, requestContext, writer);
        writer.close();
    }

    public static Map getSimpleAjaxResult(String messageKey, String message) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put(messageKey, message);
        return result;
    }

    public String renderJson(Map<String, Object> json) {
        return JsonRenderer.render(json, requestContext);
    }

    public String renderJson(List json) {
        return JsonRenderer.render(json, requestContext);
    }
}
