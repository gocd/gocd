/*************************GO-LICENSE-START*********************************
 * Copyright 2014 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *************************GO-LICENSE-END***********************************/

package com.thoughtworks.go.config.serialization;

import com.thoughtworks.go.config.BasicCruiseConfig;
import com.thoughtworks.go.helper.ConfigFileFixture;
import com.thoughtworks.go.config.ConfigMigrator;
import com.thoughtworks.go.config.CruiseConfig;
import com.thoughtworks.go.feature.FeatureValidator;
import com.thoughtworks.go.feature.TemplateFeatureValidator;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * @understands
 */
public class TemplateFeatureValidatorTest {
    private FeatureValidator feature;

    @Before
    public void setUp() {
        feature = new TemplateFeatureValidator();
    }

    @Test public void shouldBeInValidIfEnvironmentsAreDefined() throws Exception {
        assertThat(feature.isValidFeature(configWithTemplates(), null), is(false));
        assertThat(feature.isValidFeature(new BasicCruiseConfig(), null), is(true));
    }

    private CruiseConfig configWithTemplates() throws Exception {
        return ConfigMigrator.loadWithMigration(ConfigFileFixture.configWithTemplates(
                "<templates>"
                        + "  <pipeline name='foo-template' >"
                        + "  <stage name='foo-stage'>"
                        + "  <jobs>"
                        + "  <job name='job1'>"
                        + "  <tasks>"
                        + "  <exec command='ls' />"
                        + "  </tasks>"
                        + "  </job>"
                        + "  </jobs>"
                        + "  </stage>"
                        + "  </pipeline>"
                        + "</templates>")).config;
    }
}
