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
package com.thoughtworks.go.apiv1.backupconfig.representers;

import com.thoughtworks.go.api.base.OutputWriter;
import com.thoughtworks.go.api.representers.ErrorGetter;
import com.thoughtworks.go.api.representers.JsonReader;
import com.thoughtworks.go.config.BackupConfig;
import com.thoughtworks.go.spark.Routes;

import java.util.Collections;

public class BackupConfigRepresenter {
    public static void toJSON(OutputWriter outputWriter, BackupConfig backupConfig) {
        outputWriter.addLinks(outputLinkWriter -> {
            outputLinkWriter.addAbsoluteLink("doc", Routes.BackupConfig.DOC)
                    .addLink("self", Routes.BackupConfig.BASE);
        })
                .add("email_on_failure", backupConfig.isEmailOnFailure())
                .add("email_on_success", backupConfig.isEmailOnSuccess())
                .add("post_backup_script", backupConfig.getPostBackupScript())
                .add("schedule", backupConfig.getSchedule());

        if (!backupConfig.errors().isEmpty()) {
            outputWriter.addChild("errors", errorWriter -> new ErrorGetter(Collections.emptyMap()).toJSON(errorWriter, backupConfig));
        }
    }

    public static BackupConfig fromJSON(JsonReader jsonReader) {
        BackupConfig backupConfig = new BackupConfig();

        jsonReader
                .readBooleanIfPresent("email_on_failure", backupConfig::setEmailOnFailure)
                .readBooleanIfPresent("email_on_success", backupConfig::setEmailOnSuccess)
                .readStringIfPresent("post_backup_script", backupConfig::setPostBackupScript)
                .readStringIfPresent("schedule", backupConfig::setSchedule);

        return backupConfig;
    }
}
