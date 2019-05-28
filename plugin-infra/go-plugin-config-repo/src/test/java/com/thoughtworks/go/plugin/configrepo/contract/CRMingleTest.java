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
package com.thoughtworks.go.plugin.configrepo.contract;

import junit.framework.TestCase;
import org.junit.Test;

import java.util.Map;

import static junit.framework.TestCase.assertTrue;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class CRMingleTest extends AbstractCRTest<CRMingle> {

    private final CRMingle mingle;
    private final CRMingle invalidNoUrl;
    private final CRMingle invalidNoId;

    public CRMingleTest()
    {
        mingle = new CRMingle("http://mingle.example.com","my_project");
        invalidNoUrl = new CRMingle(null,"my_project");
        invalidNoId = new CRMingle("http://mingle.example.com",null);

    }

    @Override
    public void addGoodExamples(Map<String, CRMingle> examples) {
        examples.put("mingle",mingle);
    }

    @Override
    public void addBadExamples(Map<String, CRMingle> examples) {
        examples.put("invalidNoUrl",invalidNoUrl);
        examples.put("invalidNoId",invalidNoId);
    }

    @Test
    public void shouldDeserializeFromAPILikeObject()
    {
        String json = "{\n" +
                "    \"base_url\": \"https://mingle.example.com\",\n" +
                "    \"project_identifier\": \"foobar_widgets\",\n" +
                "    \"mql_grouping_conditions\": \"status > 'In Dev'\"\n" +
                "  }";
        CRMingle deserializedValue = gson.fromJson(json,CRMingle.class);

        assertThat(deserializedValue.getBaseUrl(),is("https://mingle.example.com"));
        assertThat(deserializedValue.getProjectIdentifier(),is("foobar_widgets"));
        assertThat(deserializedValue.getMqlGroupingConditions(),is("status > 'In Dev'"));

        ErrorCollection errors = deserializedValue.getErrors();
        TestCase.assertTrue(errors.isEmpty());
    }
}
