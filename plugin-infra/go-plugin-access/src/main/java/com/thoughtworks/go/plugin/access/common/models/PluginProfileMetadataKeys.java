/*
 * Copyright 2020 ThoughtWorks, Inc.
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
package com.thoughtworks.go.plugin.access.common.models;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.thoughtworks.go.plugin.domain.common.PluginConfiguration;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Spliterator;
import java.util.function.Consumer;

public class PluginProfileMetadataKeys implements Iterable<PluginProfileMetadataKey> {
    private static final Gson GSON = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();

    private final List<PluginProfileMetadataKey> keys;

    public PluginProfileMetadataKeys(List<PluginProfileMetadataKey> keys) {
        this.keys = keys;
    }

    public static PluginProfileMetadataKeys fromJSON(String json) {
        List<PluginProfileMetadataKey> keys = GSON.fromJson(json, new TypeToken<ArrayList<PluginProfileMetadataKey>>() {
        }.getType());

        return new PluginProfileMetadataKeys(keys);
    }

    public List<PluginConfiguration> toPluginConfigurations() {
        List<PluginConfiguration> pluginConfigurations = new ArrayList<>();

        for(PluginProfileMetadataKey key: keys) {
            pluginConfigurations.add(key.toPluginConfiguration());
        }

        return pluginConfigurations;
    }

    @Override
    public Iterator<PluginProfileMetadataKey> iterator() {
        return keys.iterator();
    }

    @Override
    public void forEach(Consumer<? super PluginProfileMetadataKey> action) {
        keys.forEach(action);
    }

    @Override
    public Spliterator<PluginProfileMetadataKey> spliterator() {
        return keys.spliterator();
    }

    public int size() {
        return keys.size();
    }

    public PluginProfileMetadataKey get(String key) {
        for (PluginProfileMetadataKey pluginProfileMetadataKey : keys) {
            if (key.equals(pluginProfileMetadataKey.getKey()))
                return pluginProfileMetadataKey;
        }
        return null;
    }
}
