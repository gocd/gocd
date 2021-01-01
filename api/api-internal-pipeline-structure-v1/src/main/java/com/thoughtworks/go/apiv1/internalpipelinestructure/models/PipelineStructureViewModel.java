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

package com.thoughtworks.go.apiv1.internalpipelinestructure.models;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.EnvironmentsConfig;
import com.thoughtworks.go.config.TemplatesConfig;
import com.thoughtworks.go.domain.PipelineGroups;
import com.thoughtworks.go.util.Node;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Hashtable;

@Data
@Accessors(chain = true)
public class PipelineStructureViewModel {
    private PipelineGroups pipelineGroups;
    private TemplatesConfig templatesConfig;
    private EnvironmentsConfig environmentsConfig;
    private Hashtable<CaseInsensitiveString, Node> pipelineDependencyTable;
}
