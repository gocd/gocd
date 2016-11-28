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

package com.thoughtworks.go.config;

import com.thoughtworks.go.helper.ConfigFileFixture;
import com.thoughtworks.go.util.GoConfigFileHelper;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class GoConfigDaoBasicTest extends GoConfigDaoTestBase {

    public  GoConfigDaoBasicTest()
    {
        configHelper = new GoConfigFileHelper();
        goConfigDao = configHelper.getGoConfigDao();
        cachedGoConfig = configHelper.getCachedGoConfig();
    }

    @Before
    public void setup() throws Exception {
        configHelper.initializeConfigFile();
    }

   @Test
    public void shouldUpgradeOldXmlWhenRequestedTo() throws Exception {
        cachedGoConfig.save(ConfigFileFixture.VERSION_5, true);
        CruiseConfig cruiseConfig = goConfigDao.load();
        assertThat(cruiseConfig.getAllPipelineConfigs().size(), is(1));
        assertThat(cruiseConfig.getAllPipelineConfigs().get(0).name(), is(new CaseInsensitiveString("framework")));
    }
}
