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

package com.thoughtworks.go.util;

import com.thoughtworks.go.util.json.Json;
import com.thoughtworks.go.util.json.JsonList;
import com.thoughtworks.go.util.json.JsonMap;
import com.thoughtworks.go.util.json.JsonString;
import static com.thoughtworks.go.util.ExceptionUtils.bomb;

import org.hamcrest.core.Is;
import static org.junit.Assert.assertThat;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.UniqueTag;

/**
 * Used to test Json objects
 */
public class JsonTester {
    private final Json json;

    public JsonTester(Json json) {
        this.json = json;
    }

    public JsonTester(String json) {
        this.json = javascriptParse(json);
    }

    public void is(String other) {
        Json otherJson = javascriptParse(other);
        assertThat(json, Is.is(otherJson));
    }

    public void shouldContain(String other) {
        JsonContains jsonContains = jsonContains(other);
        assertThat(json, jsonContains);
    }

    public JsonContains jsonContains(String other) {
        Json otherJson = javascriptParse(other);
        return JsonContains.contains(otherJson);
    }

    public void shouldNotContain(String other) {
        Json otherJson = javascriptParse(other);
        assertThat(json.contains(otherJson), Is.is(false));
    }

    public static Json javascriptParse(String other) {
        try {
            Context ctx = Context.enter();
            ScriptableObject scope = ctx.initStandardObjects();
            return parse(ctx.evaluateString(scope, "json = " + other, "JsonTester", 1, null));
        } catch (Exception evaluator) {
            System.err.println("Invalid json:\n" + other);
            throw bomb("Invalid javascript", evaluator);
        }
    }

    private static Json parse(Object o) {
        if (o instanceof String) {
            return new JsonString((String) o);
        } else if (o instanceof ScriptableObject) {
            return parseMapOrList((ScriptableObject) o);
        } else if (o instanceof UniqueTag) {
            return new JsonString(o.toString());
        }
        throw bomb("Unknown type: " + o.getClass());
    }

    private static Json parseMapOrList(ScriptableObject o) {
        if ("Array".equals(o.getPrototype().getClassName())) {
            return parseList(o);
        } else if ("Object".equals(o.getPrototype().getClassName())) {
            return parseMap(o);
        }
        throw bomb("Unknown type: " + o.getPrototype().getClassName());
    }

    private static Json parseMap(ScriptableObject o) {
        JsonMap jsonMap = new JsonMap();
        for (Object basicId : o.getIds()) {
            String id = String.valueOf(basicId);
            jsonMap.put(id, parse(o.get(id, o)));
        }
        return jsonMap;
    }

    private static  Json parseList(ScriptableObject o) {
        JsonList jsonList = new JsonList();
        for (Object basicId : o.getIds()) {
            Integer id = (Integer) basicId;
            jsonList.add(parse(o.get(id, o)));
        }
        return jsonList;
    }

}
