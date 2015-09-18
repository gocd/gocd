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

package com.thoughtworks.go.server.service;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.CruiseConfig;
import com.thoughtworks.go.config.GoConfigDao;
import com.thoughtworks.go.config.materials.MaterialConfigs;
import com.thoughtworks.go.domain.MaterialRevision;
import com.thoughtworks.go.domain.MaterialRevisions;
import com.thoughtworks.go.server.cache.GoCache;
import com.thoughtworks.go.server.domain.PipelineTimeline;
import com.thoughtworks.go.server.persistence.MaterialRepository;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        "classpath:WEB-INF/applicationContext-global.xml",
        "classpath:WEB-INF/applicationContext-dataLocalAccess.xml",
        "classpath:WEB-INF/applicationContext-acegi-security.xml"
})
@Ignore("To be used for Fanin production support")
public class FanInProductionVerificationTest {
    @Autowired private GoCache goCache;
    @Autowired private GoConfigDao goConfigDao;
    @Autowired private PipelineService pipelineService;
    @Autowired private MaterialRepository materialRepository;
    @Autowired private GoConfigService goConfigService;
    @Autowired private PipelineTimeline pipelineTimeline;

    @Before
    public void setUp() throws Exception {
        goCache.clear();
    }
// This test expects the config at /home/cruise/projects/cruise/server/config/cruise-config.xml and db at /home/cruise/projects/cruise/server/db/h2db/cruise.h2.db
    @Test
    public void shouldGetRevisionsBasedOnDependencies(){
        CruiseConfig config = goConfigDao.load();
        CaseInsensitiveString pipelineName = new CaseInsensitiveString("DPD11");
        MaterialConfigs materialConfigs = goConfigService.materialConfigsFor(pipelineName);
        MaterialRevisions latestRevisions = materialRepository.findLatestRevisions(materialConfigs);

        MaterialRevisions finalRevisions = getRevisionsBasedOnDependencies(config, latestRevisions, pipelineName);
        for (MaterialRevision finalRevision : finalRevisions) {
            System.out.println(finalRevision.toString());
        }
    }

    private MaterialRevisions getRevisionsBasedOnDependencies(CruiseConfig cruiseConfig, MaterialRevisions given, CaseInsensitiveString pipelineName) {
        pipelineTimeline.update();
        return pipelineService.getRevisionsBasedOnDependencies(given, cruiseConfig, pipelineName);
    }

}
