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
package com.thoughtworks.go.config;

import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class DeleteTemplateCommandTest {

    @Test
    public void shouldRemoveATemplateWithTheGivenName() throws Exception {
        DeleteTemplateCommand command = new DeleteTemplateCommand("template", "md5");
        CruiseConfig config = new BasicCruiseConfig();
        config.addTemplate(new PipelineTemplateConfig(new CaseInsensitiveString("template")));
        command.update(config);
        assertThat(config.getTemplates().isEmpty(), is(true));
    }
}
