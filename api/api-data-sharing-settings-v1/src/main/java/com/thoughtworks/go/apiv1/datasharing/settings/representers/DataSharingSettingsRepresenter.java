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
package com.thoughtworks.go.apiv1.datasharing.settings.representers;

import com.thoughtworks.go.api.base.OutputWriter;
import com.thoughtworks.go.api.representers.JsonReader;
import com.thoughtworks.go.domain.DataSharingSettings;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.spark.Routes.DataSharing;
import com.thoughtworks.go.util.TimeProvider;

import java.sql.Timestamp;

public class DataSharingSettingsRepresenter {
    public static void toJSON(OutputWriter outputWriter, DataSharingSettings dataSharingSettings) {
        outputWriter
                .addLinks(linksWriter -> linksWriter.addLink("self", DataSharing.SETTINGS_PATH)
                        .addAbsoluteLink("doc", DataSharing.SETTINGS_DOC))
                .addChild("_embedded", childWriter -> {
                    childWriter
                            .add("allow", dataSharingSettings.isAllowSharing())
                            .add("updated_by", dataSharingSettings.getUpdatedBy())
                            .add("updated_on", dataSharingSettings.getUpdatedOn());
                });
    }

    public static DataSharingSettings fromJSON(JsonReader jsonReader, Username username, TimeProvider timeProvider, DataSharingSettings fromServer) {
        DataSharingSettings dataSharingSettings = new DataSharingSettings();
        Boolean consent = jsonReader.optBoolean("allow").orElse(fromServer.isAllowSharing());
        dataSharingSettings.setAllowSharing(consent);
        dataSharingSettings.setUpdatedBy(username.getUsername().toString());
        dataSharingSettings.setUpdatedOn(new Timestamp(timeProvider.currentTimeMillis()));
        return dataSharingSettings;
    }
}
