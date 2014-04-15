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

import com.thoughtworks.go.util.json.Json;

public class JsonStringRenderer extends AbstractJsonRenderer {
    private StringBuilder builder = new StringBuilder();

    protected JsonStringRenderer(GoRequestContext requestContext) {
        super(requestContext);
    }

    public String toString() {
        return builder.toString();
    }

    public void append(String s) {
        builder.append(s);
    }

    public static String render(Json json) {
        return render(json, null);
    }

    public static String render(Json json, GoRequestContext requestContext) {
        JsonStringRenderer renderer = new JsonStringRenderer(requestContext);
        json.renderTo(renderer);
        return renderer.toString();
    }

}
