/*
 * Copyright 2015 ThoughtWorks, Inc.
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

package com.thoughtworks.go.util;

import com.google.gson.*;
import org.hamcrest.core.Is;
import org.json.JSONException;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.UniqueTag;
import org.skyscreamer.jsonassert.JSONAssert;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.thoughtworks.go.util.ExceptionUtils.bomb;
import static java.lang.String.valueOf;
import static org.junit.Assert.assertThat;

/**
 * Used to test Json objects
 */
public class JsonTester {
    private final Object json;
    private static final Gson GSON = new GsonBuilder().registerTypeAdapter(TimeConverter.ConvertedTime.class, new JsonSerializer<TimeConverter.ConvertedTime>() {
        @Override
        public JsonElement serialize(TimeConverter.ConvertedTime src, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(src.toString());
        }
    }).setPrettyPrinting().create();

    public JsonTester(Map json) {
        this.json = json;
    }

    public JsonTester(List json) {
        this.json = json;
    }

    public JsonTester(String json) {
        this.json = javascriptParse(json);
    }

    public void is(String other) {
        Object otherJson = javascriptParse(other);
        assertThat(json, Is.is(otherJson));
    }

    public void shouldContain(String other) {
        Object otherJson = javascriptParse(other);

        try {
            String actual = GSON.toJson(this.json);
            String expected = GSON.toJson(otherJson);
            JSONAssert.assertEquals(expected, actual, false);
        } catch (JSONException e) {
            throw new AssertionError(e.getMessage());
        }
    }

    public static Object javascriptParse(String other) {
        try {
            Context ctx = Context.enter();
            ScriptableObject scope = ctx.initStandardObjects();
            return parse(ctx.evaluateString(scope, "json = " + other, "JsonTester", 1, null));
        } catch (Exception evaluator) {
            evaluator.printStackTrace();
            System.err.println("Invalid json:\n" + other);
            throw bomb("Invalid javascript", evaluator);
        } finally {
            Context.exit();
        }
    }

    private static Object parse(Object o) {
        if (o instanceof String) {
            return o;
        } else if (o instanceof ScriptableObject) {
            return parseMapOrList((ScriptableObject) o);
        } else if (o instanceof UniqueTag) {
            return o.toString();
        } else if (o instanceof Double) {
            return ((Double) o).intValue();
        } else if (o instanceof Number) {
            return o;
        }
        throw bomb("Unknown type: " + o.getClass());
    }

    private static Object parseMapOrList(ScriptableObject o) {
        if ("Array".equals(o.getPrototype().getClassName())) {
            return parseList(o);
        } else if ("Object".equals(o.getPrototype().getClassName())) {
            return parseMap(o);
        }
        throw bomb("Unknown type: " + o.getPrototype().getClassName());
    }

    private static Map<String, Object> parseMap(ScriptableObject o) {
        Map<String, Object> jsonMap = new LinkedHashMap<>();
        for (Object basicId : o.getIds()) {
            String id = valueOf(basicId);
            jsonMap.put(id, parse(o.get(id, o)));
        }
        return jsonMap;
    }

    private static List parseList(ScriptableObject o) {
        List jsonList = new ArrayList();
        for (Object basicId : o.getIds()) {
            Integer id = (Integer) basicId;
            jsonList.add(parse(o.get(id, o)));
        }
        return jsonList;
    }

}
