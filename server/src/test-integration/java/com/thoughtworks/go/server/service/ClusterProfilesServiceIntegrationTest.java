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
package com.thoughtworks.go.server.service;

import com.thoughtworks.go.config.CruiseConfig;
import com.thoughtworks.go.config.UpdateConfigCommand;
import com.thoughtworks.go.config.elastic.ClusterProfile;
import com.thoughtworks.go.config.elastic.ClusterProfiles;
import com.thoughtworks.go.domain.config.ConfigurationKey;
import com.thoughtworks.go.domain.config.ConfigurationProperty;
import com.thoughtworks.go.domain.config.ConfigurationValue;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import com.thoughtworks.go.util.GoConfigFileHelper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {
        "classpath:/applicationContext-global.xml",
        "classpath:/applicationContext-dataLocalAccess.xml",
        "classpath:/testPropertyConfigurer.xml",
        "classpath:/spring-all-servlet.xml",
})
public class ClusterProfilesServiceIntegrationTest {
    @Autowired
    private GoConfigService goConfigService;
    @Autowired
    private ClusterProfilesService clusterProfilesService;

    private GoConfigFileHelper configHelper = new GoConfigFileHelper();

    @BeforeEach
    public void setUp() throws Exception {
        configHelper.onSetUp();
        goConfigService.updateConfig(new UpdateConfigCommand() {
            @Override
            public CruiseConfig update(CruiseConfig cruiseConfig) throws Exception {
                cruiseConfig.getElasticConfig().setClusterProfiles(new ClusterProfiles());
                return cruiseConfig;
            }
        });
    }

    @AfterEach
    public void tearDown() throws Exception {
        configHelper.onTearDown();
    }

    @Test
    public void shouldCreateANewClusterProfile() throws Exception {
        String clusterId = "cluster1";
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        ClusterProfile clusterProfile = new ClusterProfile(clusterId, "pluginid");

        assertThat(clusterProfilesService.getPluginProfiles().size(), is(0));

        clusterProfilesService.create(clusterProfile, new Username("Bob"), result);

        assertThat(result.isSuccessful(), is(true));
        assertThat(clusterProfilesService.getPluginProfiles().size(), is(1));
        assertThat(clusterProfilesService.getPluginProfiles().find(clusterId), is(clusterProfile));
    }

    @Test
    public void shouldNotAllowCreationANewClusterProfileWithSameName() throws Exception {
        String clusterId = "cluster1";
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        ClusterProfile clusterProfile = new ClusterProfile(clusterId, "pluginid");

        assertThat(clusterProfilesService.getPluginProfiles().size(), is(0));

        clusterProfilesService.create(clusterProfile, new Username("Bob"), result);
        assertThat(result.isSuccessful(), is(true));
        assertThat(clusterProfile.getAllErrors(), hasSize(0));
        assertThat(clusterProfilesService.getPluginProfiles().size(), is(1));
        assertThat(clusterProfilesService.getPluginProfiles().find(clusterId), is(clusterProfile));

        clusterProfilesService.create(clusterProfile, new Username("Bob"), result);
        assertThat(clusterProfilesService.getPluginProfiles().size(), is(1));
        assertThat(result.isSuccessful(), is(false));
        assertThat(clusterProfile.getAllErrors(), hasSize(1));
        assertThat(clusterProfile.errors().get("id").get(0), is("Cluster Profile id 'cluster1' is not unique"));
    }

    @Test
    public void shouldDeleteExistingClusterProfile() {
        String clusterId = "cluster1";
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        ClusterProfile clusterProfile = new ClusterProfile(clusterId, "pluginid");

        clusterProfilesService.create(clusterProfile, new Username("Bob"), new HttpLocalizedOperationResult());

        assertThat(clusterProfilesService.getPluginProfiles().size(), is(1));
        assertThat(clusterProfilesService.getPluginProfiles().find(clusterId), is(clusterProfile));

        clusterProfilesService.delete(clusterProfile, new Username("Bob"), result);

        assertThat(result.isSuccessful(), is(true));
        assertThat(clusterProfilesService.getPluginProfiles().size(), is(0));
    }

    @Test
    public void shouldUpdateExistingClusterProfile() {
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();

        String clusterId = "cluster1";
        String pluginid = "pluginid";
        ClusterProfile clusterProfile = new ClusterProfile(clusterId, pluginid);
        ConfigurationProperty property = new ConfigurationProperty(new ConfigurationKey("username"), new ConfigurationValue("view"));
        ClusterProfile newClusterProfile = new ClusterProfile(clusterId, pluginid, property);

        clusterProfilesService.create(clusterProfile, new Username("Bob"), new HttpLocalizedOperationResult());

        assertThat(clusterProfilesService.getPluginProfiles().size(), is(1));
        assertThat(clusterProfilesService.getPluginProfiles().find(clusterId), is(clusterProfile));

        clusterProfilesService.update(newClusterProfile, new Username("Bob"), result);

        assertThat(result.isSuccessful(), is(true));
        assertThat(clusterProfilesService.getPluginProfiles().size(), is(1));
        assertThat(clusterProfilesService.getPluginProfiles().find(clusterId), is(newClusterProfile));
    }
}
