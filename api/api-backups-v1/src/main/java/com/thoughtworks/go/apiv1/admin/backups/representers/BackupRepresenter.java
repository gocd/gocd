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

package com.thoughtworks.go.apiv1.admin.backups.representers;

import com.thoughtworks.go.api.representers.JsonWriter;
import com.thoughtworks.go.apiv1.user.representers.UserSummaryRepresenter;
import com.thoughtworks.go.server.domain.ServerBackup;
import com.thoughtworks.go.spark.RequestContext;
import com.thoughtworks.go.spark.Routes;

import java.util.Map;

public class BackupRepresenter {

    public static Map<String, Object> toJSON(ServerBackup backup, RequestContext requestContext) {
        return new JsonWriter(requestContext)

                .addDocLink(Routes.Backups.DOC)

                .add("time", backup.getTime())
                .add("path", backup.getPath())
                .add("user", UserSummaryRepresenter.toJSON(backup.getUsername(), requestContext)).getAsMap();
    }

}
