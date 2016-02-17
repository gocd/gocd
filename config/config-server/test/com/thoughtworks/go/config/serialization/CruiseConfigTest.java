/*
 * Copyright 2016 ThoughtWorks, Inc.
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

package com.thoughtworks.go.config.serialization;

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.registry.ConfigElementImplementationRegistry;
import com.thoughtworks.go.helper.ConfigFileFixture;
import com.thoughtworks.go.util.ConfigElementImplementationRegistryMother;
import org.junit.Test;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * @understands
 */
public class CruiseConfigTest {

    @Test public void shouldFindAllResourcesOnAllJobs() throws Exception {
        String jobXml = "<job name=\"dev1\">\n"
                + "<resources>\n"
                + "<resource>one</resource>\n"
                + "<resource>two</resource>\n"
                + "</resources>\n"
                + "</job>";
        String jobXml2 = "<job name=\"dev2\">\n"
                + "<resources>\n"
                + "<resource>two</resource>\n"
                + "<resource>three</resource>\n"
                + "</resources>\n"
                + "</job>";

        ConfigElementImplementationRegistry registry = ConfigElementImplementationRegistryMother.withNoPlugins();
        CruiseConfig config = new MagicalGoConfigXmlLoader(new ConfigCache(), registry).loadConfigHolder(ConfigFileFixture.withJob(jobXml + jobXml2)).config;
        assertThat(config.getAllResources(), hasItem(new Resource("one")));
        assertThat(config.getAllResources(), hasItem(new Resource("two")));
        assertThat(config.getAllResources(), hasItem(new Resource("three")));
        assertThat(config.getAllResources().size(), is(3));
    }

    @Test
    public void shouldReturnTrueIfUserIsAdmin() throws Exception {
        CruiseConfig config = ConfigMigrator.loadWithMigration(ConfigFileFixture.STAGE_AUTH_WITH_ADMIN_AND_AUTH).config;
        assertThat(config.isAdministrator("admin"), is(true));
    }

    @Test
    public void doesRoleHaveAdminPrivileges_shouldReturnTrueIfRoleIsAdmin() throws Exception {
        CruiseConfig config = ConfigMigrator.loadWithMigration(ConfigFileFixture.CONFIG_WITH_ROLE_AS_ADMIN).config;
        assertThat(config.doesRoleHaveAdminPrivileges("adminRole"), is(true));
    }

    @Test
    public void doesRoleHaveAdminPrivileges_shouldReturnTrueIfNoAdminsAreDefined() throws Exception {
        CruiseConfig config = ConfigMigrator.loadWithMigration(ConfigFileFixture.TWO_PIPELINES).config;
        assertThat(config.doesRoleHaveAdminPrivileges("adminRole"), is(true));
    }

    @Test
    public void shouldReturnfalseIfUserIsNotAdmin() throws Exception {
        CruiseConfig config = ConfigMigrator.loadWithMigration(ConfigFileFixture.STAGE_AUTH_WITH_ADMIN_AND_AUTH).config;
        assertThat(config.isAdministrator("pavan"), is(false));
    }
}
