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

package com.thoughtworks.go.server.web;

import java.io.PrintWriter;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.thoughtworks.go.util.json.Json;
import com.thoughtworks.go.util.json.JsonFakeMap;
import com.thoughtworks.go.util.json.JsonMap;
import org.springframework.web.servlet.view.AbstractView;

public class JsonView extends AbstractView {

    private static final String RENDER_DIRECT = " ";
    private GoRequestContext requestContext;

    public JsonView() {
    }

    public JsonView(GoRequestContext requestContext) {
        this.requestContext = requestContext;
    }

    //TODO: ChrisS - this should just be a normal HashMap
    public static JsonFakeMap asMap(Json json) {
        return new JsonFakeMap(json);
    }

    protected void renderMergedOutputModel(Map map, HttpServletRequest httpServletRequest,
                                           HttpServletResponse httpServletResponse) throws Exception {
        if (requestContext == null) {
            //TODO requestContext may already exist in request; need to check it
            try {
                requestContext = new GoRequestContext(httpServletRequest);
            } catch (Exception e) {
                //ignore
                throw e;
            }
        }
        Json json = (Json) map.get("json");

        PrintWriter writer = httpServletResponse.getWriter();
        JsonRenderer renderer = new JsonStreamRenderer(requestContext, writer);
        json.renderTo(renderer);
        writer.close();
    }

    public String renderJson(Json json) {
        return JsonStringRenderer.render(json, requestContext);
    }

    public static JsonMap getSimpleAjaxResult(String messageKey, String message){
        JsonMap result = new JsonMap();
        result.put(messageKey, message);
        return result;
    }
}
