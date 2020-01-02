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
package com.thoughtworks.go.apiv1.dependencymaterialautocomplete.representers;

import com.thoughtworks.go.api.base.OutputListWriter;
import com.thoughtworks.go.config.PipelineConfig;

import java.util.List;
import java.util.stream.Collectors;

public class SuggestionsRepresenter {

    private static final String NAME = "name";
    private static final String STAGES = "stages";

    public static void toJSON(OutputListWriter writer, List<PipelineConfig> suggestions) {
        suggestions.forEach(pipeline -> writer.addChild(entry -> {
                    entry.add(NAME, pipeline.name());
                    entry.addChildList(STAGES, pipeline.stream().map(stage -> stage.name().toString()).collect(Collectors.toList()));
                })
        );
    }
}
