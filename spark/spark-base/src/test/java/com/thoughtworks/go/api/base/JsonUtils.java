/*
 * Copyright 2021 ThoughtWorks, Inc.
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
package com.thoughtworks.go.api.base;

import com.thoughtworks.go.spark.mocks.TestRequestContext;

import java.io.IOException;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public abstract class JsonUtils {

    public static String toArrayString(Consumer<OutputListWriter> consumer) {
        return new JsonOutputWriter(new StringWriter(1024), new TestRequestContext()).forTopLevelArray(consumer).writer.toString();
    }

    public static String toObjectString(Consumer<OutputWriter> consumer) {
        return new JsonOutputWriter(new StringWriter(1024), new TestRequestContext()).forTopLevelObject(consumer).writer.toString();
    }

    public static String toObjectStringWithoutLinks(Consumer<OutputWriter> consumer) {
        return new JsonOutputWriter(new StringWriter(1024), null).forTopLevelObject(consumer).writer.toString();
    }

    public static Map<String, ?> toObjectWithoutLinks(Consumer<OutputWriter> consumer) throws IOException {
        return JsonOutputWriter.OBJECT_MAPPER.readValue(toObjectStringWithoutLinks(consumer), Map.class);
    }

    public static Map<String, ?> toObject(Consumer<OutputWriter> consumer) throws IOException {
        return JsonOutputWriter.OBJECT_MAPPER.readValue(toObjectString(consumer), Map.class);
    }

    public static List<?> toArray(Consumer<OutputListWriter> consumer) throws IOException {
        return JsonOutputWriter.OBJECT_MAPPER.readValue(toArrayString(consumer), List.class);
    }
}
