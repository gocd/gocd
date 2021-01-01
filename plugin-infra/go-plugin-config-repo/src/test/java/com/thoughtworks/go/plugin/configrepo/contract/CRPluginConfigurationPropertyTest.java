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
package com.thoughtworks.go.plugin.configrepo.contract;

import java.util.Map;

public class CRPluginConfigurationPropertyTest extends AbstractCRTest<CRPluginConfiguration> {

    private final CRPluginConfiguration pluginConfig;
    private final CRPluginConfiguration invalidPluginConfigNoId;
    private final CRPluginConfiguration invalidPluginConfigNoVersion;

    public CRPluginConfigurationPropertyTest()
    {
        pluginConfig = new CRPluginConfiguration("curl.task.plugin","1");

        invalidPluginConfigNoId = new CRPluginConfiguration(null,"1");
        invalidPluginConfigNoVersion = new CRPluginConfiguration("curl.task.plugin",null);
    }

    @Override
    public void addGoodExamples(Map<String, CRPluginConfiguration> examples) {
        examples.put("pluginConfig",pluginConfig);
    }

    @Override
    public void addBadExamples(Map<String, CRPluginConfiguration> examples) {
        examples.put("invalidPluginConfigNoId",invalidPluginConfigNoId);
        examples.put("invalidPluginConfigNoVersion",invalidPluginConfigNoVersion);
    }
}