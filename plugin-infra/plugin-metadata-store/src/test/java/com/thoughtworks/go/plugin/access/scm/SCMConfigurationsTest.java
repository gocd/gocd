/*
 * Copyright Thoughtworks, Inc.
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
package com.thoughtworks.go.plugin.access.scm;

import com.thoughtworks.go.plugin.api.config.Property;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class SCMConfigurationsTest {
    @Test
    public void shouldGetAllSCMsSortedByDisplayOrder() {
        SCMConfiguration c1 = new SCMConfiguration("k1").with(SCMConfiguration.DISPLAY_ORDER, 2);
        SCMConfiguration c2 = new SCMConfiguration("k2").with(SCMConfiguration.DISPLAY_ORDER, 0);
        SCMConfiguration c3 = new SCMConfiguration("k3").with(SCMConfiguration.DISPLAY_ORDER, 1);
        SCMConfigurations scmConfigurations = new SCMConfigurations();
        scmConfigurations.add(c1);
        scmConfigurations.add(c2);
        scmConfigurations.add(c3);

        List<SCMConfiguration> scmConfigurationList = scmConfigurations.list();

        assertThat(scmConfigurationList.get(0)).isEqualTo(c2);
        assertThat(scmConfigurationList.get(1)).isEqualTo(c3);
        assertThat(scmConfigurationList.get(2)).isEqualTo(c1);
    }

    @Test
    public void shouldConstructSCMConfiguration() {
        SCMPropertyConfiguration scmPropertyConfiguration = new SCMPropertyConfiguration();
        scmPropertyConfiguration.add(new SCMProperty("k1", "v1").with(Property.SECURE, Boolean.TRUE));

        SCMConfigurations scmConfigurations = new SCMConfigurations(scmPropertyConfiguration);

        assertThat(scmConfigurations.list().size()).isEqualTo(1);
        SCMConfiguration scmConfiguration = scmConfigurations.list().get(0);
        assertThat(scmConfiguration.getKey()).isEqualTo("k1");
        assertThat(scmConfiguration.getValue()).isEqualTo("v1");
        assertThat(scmConfiguration.getOption(SCMConfiguration.REQUIRED)).isEqualTo(true);
        assertThat(scmConfiguration.getOption(SCMConfiguration.PART_OF_IDENTITY)).isEqualTo(true);
        assertThat(scmConfiguration.getOption(SCMConfiguration.SECURE)).isEqualTo(true);
        assertThat(scmConfiguration.getOption(SCMConfiguration.DISPLAY_NAME)).isEmpty();
        assertThat(scmConfiguration.getOption(SCMConfiguration.DISPLAY_ORDER)).isEqualTo(0);
    }
}
