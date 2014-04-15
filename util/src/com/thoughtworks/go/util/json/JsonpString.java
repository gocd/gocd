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

package com.thoughtworks.go.util.json;

import com.thoughtworks.go.server.web.JsonRenderer;

public class JsonpString implements Json {
    private Json json;
    private String callback;

    public boolean contains(Json json) {
        return false;
    }

    public JsonpString(Json json, String callback) {
        this.json = json;
        this.callback = callback;
    }

    public void renderTo(JsonRenderer renderer) {
        renderer.append(callback);
        renderer.append("(");
        json.renderTo(renderer);
        renderer.append(")");
    }

    public Json toJson() {
        return this;
    }
}
