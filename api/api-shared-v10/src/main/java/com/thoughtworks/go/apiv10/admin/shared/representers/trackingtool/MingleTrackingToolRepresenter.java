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

package com.thoughtworks.go.apiv10.admin.shared.representers.trackingtool;

import com.thoughtworks.go.api.base.OutputWriter;
import com.thoughtworks.go.api.representers.JsonReader;
import com.thoughtworks.go.config.MingleConfig;

public class MingleTrackingToolRepresenter {

    public static void toJSON(OutputWriter jsonWriter, MingleConfig mingleConfig) {
        jsonWriter.add("base_url", mingleConfig.getBaseUrl());
        jsonWriter.add("project_identifier", mingleConfig.getProjectIdentifier());
        jsonWriter.add("mql_grouping_conditions", mingleConfig.getMqlCriteria().getMql());
    }

    public static MingleConfig fromJSON(JsonReader jsonReader) {
        MingleConfig mingleConfig = new MingleConfig();
        jsonReader.readStringIfPresent("base_url", mingleConfig::setBaseUrl);
        jsonReader.readStringIfPresent("project_identifier", mingleConfig::setProjectIdentifier);
        jsonReader.readStringIfPresent("mql_grouping_conditions", mingleConfig::setMqlCriteria);
        return mingleConfig;
    }
}
