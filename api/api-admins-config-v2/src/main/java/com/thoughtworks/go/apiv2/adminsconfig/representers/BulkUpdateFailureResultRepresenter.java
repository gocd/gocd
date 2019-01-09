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

package com.thoughtworks.go.apiv2.adminsconfig.representers;

import com.thoughtworks.go.api.base.OutputWriter;
import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.server.service.result.BulkUpdateAdminsResult;

import java.util.List;
import java.util.stream.Collectors;

public class BulkUpdateFailureResultRepresenter {

    public static void toJSON(OutputWriter outputWriter, BulkUpdateAdminsResult result) {
        outputWriter.add("message", result.message());
        outputWriter.addChildList("non_existent_users", toStringList(result.getNonExistentUsers()));
        outputWriter.addChildList("non_existent_roles", toStringList(result.getNonExistentRoles()));
    }

    private static List<String> toStringList(List<CaseInsensitiveString> list) {
        return list.stream().map(CaseInsensitiveString::toString).collect(Collectors.toList());
    }
}
