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
import org.springframework.context.MessageSourceResolvable;

public class JsonI18NResolvable implements Json {
    private MessageSourceResolvable resolvable;

    public JsonI18NResolvable(MessageSourceResolvable resolvable) {
        this.resolvable = resolvable;
    }

    public void renderTo(JsonRenderer renderer) {
        renderer.renderResolved(resolvable);
    }

    public boolean contains(Json json) {
        if (json instanceof JsonI18NResolvable) {
            JsonI18NResolvable jsonI18n = (JsonI18NResolvable) json;
            return resolvable.getDefaultMessage().equals(jsonI18n.resolvable.getDefaultMessage());
        } else if (json instanceof JsonString) {
            JsonString jsonString = (JsonString) json;
            return new JsonString(resolvable.getDefaultMessage()).equals(jsonString);
        }
        return false;
    }

    public String toString() {
        return "[JSONi18n:" + resolvable + "]";
    }

    public boolean equals(Object other) {
        if (this == other) { return true; }
        if (other == null) { return false; }
        if (this.getClass() != other.getClass()) { return false; }
        return equals((JsonI18NResolvable) other);
    }

    private boolean equals(JsonI18NResolvable other) {
        return this.resolvable.equals(other.resolvable);
    }

    public int hashCode() {
        return resolvable.hashCode();
    }

    public Json toJson() {
        return this;
    }
}
