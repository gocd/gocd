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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.thoughtworks.go.server.web.GoRequestContext;
import com.thoughtworks.go.server.web.JsonRenderer;
import static com.thoughtworks.go.util.StringUtil.quote;
import static org.apache.commons.lang.StringEscapeUtils.escapeJavaScript;

import org.apache.commons.lang.StringUtils;
import static org.apache.commons.lang.StringUtils.replace;

public class JsonString implements Json {
    protected final String value;

    public JsonString(String value) {
        this.value = defaultIfNull(value, "");
    }

    public String asJsonString(GoRequestContext context) {
        return quote(escapeJavaScript(value));
    }

    public void renderTo(JsonRenderer renderer) {
        renderer.quote(escapeJavaScript(value));
    }


    public boolean contains(Json json) {
        if (json instanceof JsonString) {
            JsonString jsonString = (JsonString) json;
            if (StringUtils.startsWith(jsonString.value, "RegEx:")) {
                Pattern pattern = Pattern.compile(replace(jsonString.value, "RegEx:", ""));
                Matcher matcher = pattern.matcher(this.value);
                return matcher.find();
            } else {
                return StringUtils.equals(this.value, jsonString.value);
            }
        } else if (json instanceof JsonI18NResolvable) {
            return json.contains(this);
        }
        return false;
    }


    protected <T> T defaultIfNull(T o, T def) {
        if (o == null) {
            return def;
        }
        return o;
    }

    public int hashCode() {
        return value.hashCode();
    }

    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null) {
            return false;
        }
        if (this.getClass() != other.getClass()) {
            return false;
        }
        return equals((JsonString) other);
    }

    private boolean equals(JsonString other) {
        return this.value.equals(other.value);
    }

    public String toString() {
        return asJsonString(null);
    }

    public String withoutQuote() {
        return value;
    }

    public Json toJson() {
        return this;
    }
}
