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
package com.thoughtworks.go.apiv2.datasharing.reporting.representers;

import com.thoughtworks.go.api.base.OutputWriter;
import com.thoughtworks.go.domain.UsageStatisticsReporting;
import com.thoughtworks.go.spark.Routes.DataSharing;

public class UsageStatisticsReportingRepresenter {
    public static void toJSON(OutputWriter outputWriter, UsageStatisticsReporting usageStatisticsReporting) {
        outputWriter
                .addLinks(linksWriter -> linksWriter.addLink("self", DataSharing.REPORTING_PATH))
                .addChild("_embedded", childWriter -> {
                    childWriter
                            .add("server_id", usageStatisticsReporting.getServerId())
                            .add("last_reported_at", usageStatisticsReporting.getLastReportedAt().getTime())
                            .add("data_sharing_server_url", usageStatisticsReporting.getDataSharingServerUrl())
                            .add("data_sharing_get_encryption_keys_url", usageStatisticsReporting.getDataSharingGetEncryptionKeysUrl())
                            .add("can_report", usageStatisticsReporting.isCanReport());
                });
    }
}
