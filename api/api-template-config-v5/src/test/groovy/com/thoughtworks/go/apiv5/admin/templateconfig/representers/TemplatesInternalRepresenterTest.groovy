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

package com.thoughtworks.go.apiv5.admin.templateconfig.representers;

import com.thoughtworks.go.config.CaseInsensitiveString
import com.thoughtworks.go.config.ParamConfig
import com.thoughtworks.go.config.ParamsConfig;
import com.thoughtworks.go.config.PipelineEditabilityInfo
import com.thoughtworks.go.config.PipelineTemplateConfig
import com.thoughtworks.go.config.StageConfig;
import com.thoughtworks.go.config.TemplateToPipelines
import com.thoughtworks.go.config.TemplatesConfig;
import org.junit.jupiter.api.Test;

import static com.thoughtworks.go.api.base.JsonUtils.toObjectString;
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson;
import static org.junit.jupiter.api.Assertions.*;

class TemplatesInternalRepresenterTest {
    @Test
    void 'should render a template name and its associated pipelines in hal representation'() {
        def templates = new TemplatesConfig(new TestPipelineConfigTemplate("template-name"))
        def actualJson = toObjectString({ TemplatesInternalRepresenter.toJSON(it, templates) })

        assertThatJson(actualJson).isEqualTo(indexHash)
    }

    def indexHash =
    [
      templates: [
        [
          name: "template-name",
          parameters: ["myParam"]
        ]
      ]
    ]
}

class TestPipelineConfigTemplate extends PipelineTemplateConfig {
    TestPipelineConfigTemplate(String name) {
        super(new CaseInsensitiveString(name))
    }

    @Override
    ParamsConfig referredParams() {
        return new ParamsConfig(new ParamConfig("myParam", null))
    }
}