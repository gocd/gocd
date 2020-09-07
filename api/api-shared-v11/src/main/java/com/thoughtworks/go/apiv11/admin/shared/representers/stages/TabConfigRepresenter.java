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
package com.thoughtworks.go.apiv11.admin.shared.representers.stages;

import com.thoughtworks.go.api.base.OutputListWriter;
import com.thoughtworks.go.api.base.OutputWriter;
import com.thoughtworks.go.api.representers.ErrorGetter;
import com.thoughtworks.go.api.representers.JsonReader;
import com.thoughtworks.go.config.Tab;
import com.thoughtworks.go.config.Tabs;

import java.util.HashMap;

public class TabConfigRepresenter {

    public static void toJSONArray(OutputListWriter tabsWriter, Tabs tabs) {
        tabs.forEach(tab -> {
            tabsWriter.addChild(tabWriter -> toJSON(tabWriter, tab));
        });
    }

    public static void toJSON(OutputWriter jsonWriter, Tab tab) {
        if (!tab.errors().isEmpty()) {
            jsonWriter.addChild("errors", errorWriter -> {
                new ErrorGetter(new HashMap<>()).toJSON(errorWriter, tab);
            });
        }
        jsonWriter.add("name", tab.getName());
        jsonWriter.add("path", tab.getPath());
    }

    public static Tabs fromJSONArray(JsonReader jsonReader) {
        Tabs tabsConfig = new Tabs();
        jsonReader.readArrayIfPresent("tabs", tabs -> {
            tabs.forEach(tab -> {
                tabsConfig.add(fromJSON(new JsonReader(tab.getAsJsonObject())));
            });
        });
        return tabsConfig;
    }

    public static Tab fromJSON(JsonReader jsonReader) {
        Tab tab = new Tab();
        jsonReader.readStringIfPresent("name", tab::setName);
        jsonReader.readStringIfPresent("path", tab::setPath);
        return tab;
    }
}
