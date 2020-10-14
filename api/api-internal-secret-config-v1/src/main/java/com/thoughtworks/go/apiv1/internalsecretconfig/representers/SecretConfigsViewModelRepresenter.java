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

package com.thoughtworks.go.apiv1.internalsecretconfig.representers;

import com.thoughtworks.go.api.base.OutputWriter;
import com.thoughtworks.go.apiv1.internalsecretconfig.models.SecretConfigsViewModel;

public class SecretConfigsViewModelRepresenter {
    public static void toJSON(OutputWriter outputWriter, SecretConfigsViewModel configsViewModel) {
        outputWriter
                .addChild("_embedded",
                        embeddedWriter -> {
                            embeddedWriter.addChildList("secret_configs",
                                    configsWriter -> configsViewModel.getSecretConfigs().forEach(
                                            config -> configsWriter.addChild(
                                                    configWriter -> SecretConfigRepresenter.toJSON(configWriter, config))));
                        })
                .addChildList("auto_completion", (suggestionWriter) -> configsViewModel.getAutoSuggestions()
                        .forEach((key, value) -> suggestionWriter.addChild(childWriter -> childWriter
                                .add("key", key)
                                .addChildList("value", value))));
        ;
    }
}
