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

package com.thoughtworks.go.apiv3.rolesconfig.representers;

import com.thoughtworks.go.api.base.OutputWriter;
import com.thoughtworks.go.apiv3.rolesconfig.models.RolesViewModel;

public class RolesViewModelRepresenter {
    public static void toJSON(OutputWriter outputWriter, RolesViewModel rolesViewModel) {
        outputWriter
                .addChild("_embedded", embeddedWriter -> embeddedWriter.addChildList("roles", rolesWriter -> rolesViewModel.getRolesConfig().forEach(role -> rolesWriter.addChild(roleWriter -> RoleRepresenter.toJSON(roleWriter, role)))))
                .addChildList("auto_completion", (suggestionWriter) -> rolesViewModel.getAutoSuggestions()
                        .forEach((key, value) -> suggestionWriter.addChild(childWriter -> childWriter
                                .add("key", key)
                                .addChildList("value", value))));
    }

}
