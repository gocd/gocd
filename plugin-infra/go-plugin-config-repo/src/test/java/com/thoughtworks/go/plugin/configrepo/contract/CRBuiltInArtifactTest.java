/*
 * Copyright 2018 ThoughtWorks, Inc.
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

import com.google.gson.JsonParseException;
import junit.framework.TestCase;
import org.junit.Test;

import java.util.Map;

import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertNull;
import static junit.framework.TestCase.assertTrue;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class CRBuiltInArtifactTest extends AbstractCRTest<CRBuiltInArtifact> {

    private final CRBuiltInArtifact artifact;
    private final CRBuiltInArtifact invalidNoSource;

    public CRBuiltInArtifactTest()
    {
        artifact = new CRBuiltInArtifact("src","dest", CRArtifactType.build);
        invalidNoSource = new CRBuiltInArtifact(null,"dest",CRArtifactType.test);
    }

    @Override
    public void addGoodExamples(Map<String, CRBuiltInArtifact> examples) {
        examples.put("artifact",artifact);
    }

    @Override
    public void addBadExamples(Map<String, CRBuiltInArtifact> examples) {
        examples.put("invalidNoSource",invalidNoSource);
    }


    @Test
    public void shouldDeserializeFromAPILikeObject()
    {
        String json = "{\n" +
                "      \"source\": \"test\",\n" +
                "      \"destination\": \"res1\",\n" +
                "      \"type\": \"test\"\n" +
                "    }";
        CRArtifact deserializedValue = gson.fromJson(json,CRArtifact.class);
        CRBuiltInArtifact crBuiltInArtifact = (CRBuiltInArtifact) deserializedValue;
        assertThat(crBuiltInArtifact.getSource(),is("test"));
        assertThat(crBuiltInArtifact.getDestination(),is("res1"));
        assertThat(crBuiltInArtifact.getType(),is(CRArtifactType.test));

        ErrorCollection errors = deserializedValue.getErrors();
        TestCase.assertTrue(errors.isEmpty());
    }

    @Test
    public void shouldHandleBadArtifactTypeWhenDeserializing()
    {
        String json = "{\n" +
                "      \"source\": \"test\",\n" +
                "      \"destination\": \"res1\",\n" +
                "      \"type\": \"bla\"\n" +
                "    }";

        thrown.expect(JsonParseException.class);
        thrown.expectMessage("Invalid or unknown task type 'bla'");
        gson.fromJson(json,CRArtifact.class);
    }
}
