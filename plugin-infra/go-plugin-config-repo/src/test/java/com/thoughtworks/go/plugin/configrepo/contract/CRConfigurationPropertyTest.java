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

public class CRConfigurationPropertyTest extends AbstractCRTest<CRConfigurationProperty> {

    private final CRConfigurationProperty configProperty;
    private final CRConfigurationProperty configPropertyEncrypted;
    private final CRConfigurationProperty invalid2ValuesSet;
    private final CRConfigurationProperty invalidEmpty;

    public CRConfigurationPropertyTest()
    {
        configProperty = new CRConfigurationProperty("key1", "value1");
        configPropertyEncrypted = new CRConfigurationProperty("key1");
        configPropertyEncrypted.setKey("213476%$");

        invalid2ValuesSet = new CRConfigurationProperty("key1", "value1","213476%$");
        invalidEmpty = new CRConfigurationProperty();
    }

    @Override
    public void addGoodExamples(Map<String, CRConfigurationProperty> examples) {
        examples.put("configProperty",configProperty);
        examples.put("configPropertyEncrypted",configPropertyEncrypted);
    }

    @Override
    public void addBadExamples(Map<String, CRConfigurationProperty> examples) {
        examples.put("invalid2ValuesSet",invalid2ValuesSet);
    }
}