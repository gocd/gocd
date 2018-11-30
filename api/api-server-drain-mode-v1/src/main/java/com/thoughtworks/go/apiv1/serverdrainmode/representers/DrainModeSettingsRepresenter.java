/*
 * Copyright 2018 ThoughtWorks, Inc.
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

package com.thoughtworks.go.apiv1.serverdrainmode.representers;

import com.thoughtworks.go.api.base.OutputWriter;
import com.thoughtworks.go.api.representers.JsonReader;
import com.thoughtworks.go.server.domain.ServerDrainMode;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.spark.Routes;
import com.thoughtworks.go.util.TimeProvider;

import java.sql.Timestamp;

public class DrainModeSettingsRepresenter {
    public static void toJSON(OutputWriter jsonWriter, ServerDrainMode serverDrainMode) {
        jsonWriter
                .addLinks(linksWriter -> linksWriter.addLink("self", Routes.DrainMode.BASE + Routes.DrainMode.SETTINGS)
                        .addAbsoluteLink("doc", Routes.DrainMode.SETTINGS_DOC))
                .addChild("_embedded", childWriter -> {
                    childWriter.add("drain", serverDrainMode.isDrainMode())
                            .add("updated_by", serverDrainMode.updatedBy())
                            .add("updated_on", serverDrainMode.updatedOn());
                });
    }

    public static ServerDrainMode fromJSON(JsonReader jsonReader, Username currentUsername, TimeProvider timeProvider, ServerDrainMode fromServer) {
        ServerDrainMode serverDrainMode = new ServerDrainMode();
        Boolean mode = jsonReader.optBoolean("drain").orElse(fromServer.isDrainMode());
        serverDrainMode.setDrainMode(mode);
        serverDrainMode.updatedBy(currentUsername.getUsername().toString());
        serverDrainMode.updatedOn(new Timestamp(timeProvider.currentTimeMillis()));
        return serverDrainMode;
    }
}
