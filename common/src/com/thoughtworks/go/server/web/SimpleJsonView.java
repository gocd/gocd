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

import com.thoughtworks.go.util.GoConstants;
import com.thoughtworks.go.util.json.Json;
import com.thoughtworks.go.util.json.JsonAware;
import com.thoughtworks.go.util.json.JsonMap;

import static com.thoughtworks.go.util.GoConstants.RESPONSE_CHARSET;
import static com.thoughtworks.go.util.GoConstants.RESPONSE_CHARSET_JSON;

import org.springframework.web.servlet.View;

public class SimpleJsonView implements View {
    private int status;
    private JsonAware jsonAware;

    public SimpleJsonView(int status, JsonAware jsonAware) {
        this.status = status;
        this.jsonAware = jsonAware;
    }


    public String getContentType() {
        return RESPONSE_CHARSET_JSON;
    }

    public void render(Map model, HttpServletRequest request, HttpServletResponse response) throws Exception {
        // In IE, there's a problem with caching. We want to cache if we can.
        // This will force the browser to clear the cache only for this page.
        // If any other pages need to clear the cache, we might want to move this
        // logic to an intercepter.
        GoRequestContext goRequestContext = new GoRequestContext(request);
        response.addHeader("Cache-Control", GoConstants.CACHE_CONTROL);
        response.setStatus(status);
        response.setContentType(getContentType());
        Json json = jsonAware.toJson();
        PrintWriter writer = response.getWriter();
        JsonRenderer renderer = new JsonStreamRenderer(goRequestContext, writer);
        json.renderTo(renderer);
        writer.close();
    }

    public static JsonMap getSimpleAjaxResult(String message){
        JsonMap result = new JsonMap();
        result.put("result", message);
        return result;
    }

}
