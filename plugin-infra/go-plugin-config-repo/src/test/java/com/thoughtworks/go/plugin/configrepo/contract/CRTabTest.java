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

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CRTabTest extends AbstractCRTest<CRTab> {

    private final CRTab tab;
    private final CRTab invalidTabNoName;
    private final CRTab invalidTabNoPath;

    public CRTabTest() {
        tab = new CRTab("results","test.xml");

        invalidTabNoName = new CRTab(null,"test.xml");
        invalidTabNoPath = new CRTab("results",null);
    }

    @Override
    public void addGoodExamples(Map<String, CRTab> examples) {
        examples.put("tab",tab);
    }

    @Override
    public void addBadExamples(Map<String, CRTab> examples) {
        examples.put("invalidTabNoName",invalidTabNoName);
        examples.put("invalidTabNoPath",invalidTabNoPath);
    }

    @Test
    public void shouldDeserializeFromAPILikeObject() {
        String json = "{\n" +
                "      \"name\": \"cobertura\",\n" +
                "      \"path\": \"target/site/cobertura/index.html\"\n" +
                "    }";
        CRTab deserializedValue = gson.fromJson(json,CRTab.class);

        assertThat(deserializedValue.getName(),is("cobertura"));
        assertThat(deserializedValue.getPath(),is("target/site/cobertura/index.html"));

        ErrorCollection errors = deserializedValue.getErrors();
        assertTrue(errors.isEmpty());
    }
}
