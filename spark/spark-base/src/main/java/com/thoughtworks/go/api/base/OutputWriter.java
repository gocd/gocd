/*
 * Copyright 2019 ThoughtWorks, Inc.
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

import com.fasterxml.jackson.databind.JsonNode;
import com.thoughtworks.go.config.CaseInsensitiveString;

import java.io.Closeable;
import java.util.Collection;
import java.util.Date;
import java.util.function.Consumer;

public interface OutputWriter extends Closeable {
    OutputWriter add(String key, String value);

    OutputWriter add(String key, Double value);

    OutputWriter add(String key, CaseInsensitiveString value);

    OutputWriter addIfNotNull(String key, String value);

    OutputWriter addIfNotNull(String key, CaseInsensitiveString value);

    OutputWriter addIfNotNull(String key, Long value);

    OutputWriter addWithDefaultIfBlank(String key, String value, String defaultValue);

    OutputWriter add(String key, int value);

    OutputWriter add(String key, boolean value);

    OutputWriter add(String key, long value);

    OutputWriter add(String key, Date value);

    OutputWriter addIfNotNull(String key, Date value);

    OutputWriter addChild(String key, Consumer<OutputWriter> consumer);

    OutputWriter addChildList(String key, Consumer<OutputListWriter> consumer);

    OutputWriter addChildList(String key, Collection<String> values);

    OutputWriter addLinks(Consumer<OutputLinkWriter> consumer);

    OutputWriter addEmbedded(Consumer<OutputWriter> consumer);

    OutputWriter add(String key, JsonNode jsonNode);

    void renderNull(String key);

    OutputWriter addIfNotNull(String key, Double value);
}
