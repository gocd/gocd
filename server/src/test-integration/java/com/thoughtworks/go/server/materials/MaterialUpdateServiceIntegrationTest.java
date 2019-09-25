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
package com.thoughtworks.go.server.materials;

import com.thoughtworks.go.config.BasicCruiseConfig;
import com.thoughtworks.go.config.CruiseConfig;
import com.thoughtworks.go.config.GoConfigWatchList;
import com.thoughtworks.go.config.materials.MaterialConfigs;
import com.thoughtworks.go.config.materials.svn.SvnMaterial;
import com.thoughtworks.go.config.materials.svn.SvnMaterialConfig;
import static com.thoughtworks.go.helper.MaterialConfigsMother.svn;
import com.thoughtworks.go.helper.GoConfigMother;
import com.thoughtworks.go.server.perf.MDUPerformanceLogger;
import com.thoughtworks.go.server.service.MaintenanceModeService;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.MaterialConfigConverter;
import com.thoughtworks.go.serverhealth.HealthStateScope;
import com.thoughtworks.go.serverhealth.HealthStateType;
import com.thoughtworks.go.serverhealth.ServerHealthMatcher;
import com.thoughtworks.go.serverhealth.ServerHealthService;
import com.thoughtworks.go.serverhealth.ServerHealthState;
import com.thoughtworks.go.util.SystemEnvironment;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        "classpath:/applicationContext-global.xml",
        "classpath:/applicationContext-dataLocalAccess.xml",
        "classpath:/testPropertyConfigurer.xml",
        "classpath:/spring-all-servlet.xml",
})
public class MaterialUpdateServiceIntegrationTest {
    @Autowired private ServerHealthService serverHealthService;
    @Autowired private SystemEnvironment systemEnvironment;
    @Autowired private MaterialConfigConverter materialConfigConverter;
    @Autowired private MaintenanceModeService maintenanceModeService;

    @Test public void shouldClearServerHealthLogsForMaterialThatNoLongerExistsInCruiseConfig() throws Exception {
        HealthStateScope badScope = HealthStateScope.forMaterial(new SvnMaterial("non-existent-url!", "user", "pwd", false));
        serverHealthService.update(ServerHealthState.error("where's the material!", "fubar", HealthStateType.general(badScope)));

        SvnMaterialConfig goodMaterial = svn("good-url!", "user", "pwd", false);
        HealthStateScope goodScope = HealthStateScope.forMaterialConfig(goodMaterial);
        serverHealthService.update(ServerHealthState.error("could not update!", "why", HealthStateType.general(goodScope)));

        MaterialUpdateService materialUpdateService = new MaterialUpdateService(null,null, mock(MaterialUpdateCompletedTopic.class),
                mock(GoConfigWatchList.class),mock(GoConfigService.class),
                systemEnvironment, serverHealthService, null, mock(MDUPerformanceLogger.class), materialConfigConverter, null, maintenanceModeService, null);

        materialUpdateService.onConfigChange(configWithMaterial(goodMaterial));

        assertThat(serverHealthService, ServerHealthMatcher.containsState(HealthStateType.general(goodScope)));
    }

    @Test public void shouldClearServerHealthLogsForMaterialWhereAutoUpdateChanged() throws Exception {
        SvnMaterialConfig material = svn("non-existent-url!", "user", "pwd2", false);
        HealthStateScope scope = HealthStateScope.forMaterialConfig(material);
        serverHealthService.update(ServerHealthState.error("where's the material!", "fubar", HealthStateType.general(scope)));

        material.setAutoUpdate(false);

        MaterialUpdateService materialUpdateService = new MaterialUpdateService(null,null, mock(MaterialUpdateCompletedTopic.class),
                mock(GoConfigWatchList.class),mock(GoConfigService.class),
                systemEnvironment, serverHealthService, null, mock(MDUPerformanceLogger.class), materialConfigConverter, null, maintenanceModeService, null);

        materialUpdateService.onConfigChange(configWithMaterial(material));

        assertThat(serverHealthService, ServerHealthMatcher.doesNotContainState(HealthStateType.general(scope)));
    }

    private CruiseConfig configWithMaterial(SvnMaterialConfig goodMaterial) {
        CruiseConfig config = new BasicCruiseConfig();
        new GoConfigMother().addPipeline(config, "good-pipeline", "first-stage", new MaterialConfigs(goodMaterial));
        return config;
    }

}
