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

package com.thoughtworks.go.apiv1.artifactconfig.represernter;

import com.thoughtworks.go.api.base.OutputWriter;
import com.thoughtworks.go.api.representers.ErrorGetter;
import com.thoughtworks.go.config.PurgeSettings;

import java.util.HashMap;
import java.util.Map;

public class PurgeSettingsRepresenter {
    public static void toJSON(OutputWriter purgeSettingWriter, PurgeSettings purgeSettings) {

        purgeSettingWriter
                .addIfNotNull("purge_start_disk_space", purgeSettings.getPurgeStart().getPurgeStartDiskSpace())
                .addIfNotNull("purge_upto_disk_space", purgeSettings.getPurgeUpto().getPurgeUptoDiskSpace());

        if (!purgeSettings.errors().isEmpty()) {
            Map<String, String> fieldMapping = new HashMap<>();
            fieldMapping.put("purgeStart", "purge_start_disk_space");
            fieldMapping.put("purgeUpto", "purge_upto_disk_space");
            purgeSettingWriter.addChild("errors", errorWriter -> new ErrorGetter(fieldMapping).toJSON(errorWriter, purgeSettings));
        }
    }
}
