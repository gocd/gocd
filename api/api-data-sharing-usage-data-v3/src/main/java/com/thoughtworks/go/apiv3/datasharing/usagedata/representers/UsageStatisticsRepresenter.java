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

package com.thoughtworks.go.apiv3.datasharing.usagedata.representers;

import com.thoughtworks.go.api.base.OutputWriter;
import com.thoughtworks.go.apiv3.datasharing.usagedata.UsagedataType;
import com.thoughtworks.go.apiv3.datasharing.usagedata.representers.typebased.AdditionalUsageStatisticsRepresenter;
import com.thoughtworks.go.apiv3.datasharing.usagedata.representers.typebased.BasicUsageStatisticsRepresenter;
import com.thoughtworks.go.server.domain.UsageStatistics;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UsageStatisticsRepresenter {
    private static final int MESSAGE_SCHEMA_VERSION = 2;
    private static final Map<UsagedataType, UsageDataTypeRepresenter> representerMapper;

    static {
        representerMapper = new HashMap<>();
        representerMapper.put(UsagedataType.BASIC, new BasicUsageStatisticsRepresenter());
        representerMapper.put(UsagedataType.ADDITIONAL, new AdditionalUsageStatisticsRepresenter());
    }

    public static void toJSON(OutputWriter outputWriter, UsageStatistics usageStatistics, List<UsagedataType> usageDataTypes) {
        outputWriter
                .add("server_id", usageStatistics.serverId())
                .add("message_version", MESSAGE_SCHEMA_VERSION)
                .addChild("data", childWriter -> {
                    usageDataTypes.forEach(t -> representerMapper.get(t).toJSON(childWriter, usageStatistics));
                });
    }
}
