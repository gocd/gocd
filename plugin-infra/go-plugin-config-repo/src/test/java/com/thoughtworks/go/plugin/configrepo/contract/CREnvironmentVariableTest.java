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

public class CREnvironmentVariableTest extends AbstractCRTest<CREnvironmentVariable> {

    private CREnvironmentVariable key1;

    private CREnvironmentVariable invalidNameNotSet;
    private CREnvironmentVariable invalidValueNotSet;
    private CREnvironmentVariable invalid2ValuesSet;

    public CREnvironmentVariableTest()
    {
        key1 = new CREnvironmentVariable("key1");
        key1.setValue("value1");

        invalidNameNotSet = new CREnvironmentVariable();
        invalidNameNotSet.setValue("23");

        invalidValueNotSet = new CREnvironmentVariable("key5");

        invalid2ValuesSet = new CREnvironmentVariable("keyd");
        invalid2ValuesSet.setValue("value1");
        invalid2ValuesSet.setEncryptedValue("v123445");
    }

    @Override
    public void addGoodExamples(Map<String, CREnvironmentVariable> examples) {
        examples.put("key1", key1);
    }

    @Override
    public void addBadExamples(Map<String, CREnvironmentVariable> examples) {
        examples.put("invalidNameNotSet",invalidNameNotSet);
        examples.put("invalidValueNotSet",invalidValueNotSet);
        examples.put("invalid2ValuesSet",invalid2ValuesSet);
    }

}