/*
 * Copyright 2021 ThoughtWorks, Inc.
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

import com.thoughtworks.go.api.base.OutputWriter;
import com.thoughtworks.go.api.representers.ErrorGetter;
import com.thoughtworks.go.api.representers.JsonReader;
import com.thoughtworks.go.config.Approval;
import com.thoughtworks.go.config.AuthConfig;

import java.util.HashMap;

public class ApprovalRepresenter {

    public static void toJSON(OutputWriter jsonWriter, Approval approval) {
        if (!approval.errors().isEmpty()) {
            jsonWriter.addChild("errors", errorWriter -> {
                new ErrorGetter(new HashMap<>()).toJSON(errorWriter, approval);
            });
        }

        jsonWriter.add("type", approval.getType());
        jsonWriter.add(Approval.ALLOW_ONLY_ON_SUCCESS, approval.isAllowOnlyOnSuccess());
        jsonWriter.addChild("authorization", authConfigWriter -> StageAuthorizationRepresenter.toJSON(authConfigWriter, approval.getAuthConfig()));
    }

    public static Approval fromJSON(JsonReader jsonReader) {
        Approval approval = new Approval();
        jsonReader.readStringIfPresent("type", approval::setType);
        jsonReader.readBooleanIfPresent(Approval.ALLOW_ONLY_ON_SUCCESS, approval::setAllowOnlyOnSuccess);
        AuthConfig authConfig = StageAuthorizationRepresenter.fromJSON(jsonReader.readJsonObject("authorization"));
        approval.setAuthConfig(authConfig);
        return approval;
    }
}
