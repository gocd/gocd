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

import java.util.Arrays;
import java.util.Map;

import static com.thoughtworks.go.util.TestUtils.contains;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CRApprovalTest extends AbstractCRTest<CRApproval> {

    private final CRApproval manual;
    private final CRApproval success;
    private final CRApproval manualWithAuth;
    private final CRApproval badType;

    public CRApprovalTest() {
        manual = new CRApproval(CRApprovalCondition.manual);
        success = new CRApproval(CRApprovalCondition.success);

        manualWithAuth = new CRApproval(CRApprovalCondition.manual);
        manualWithAuth.setRoles(Arrays.asList("manager"));

        badType = new CRApproval();
    }

    @Override
    public void addGoodExamples(Map<String, CRApproval> examples) {
        examples.put("manual",manual);
        examples.put("success",success);
        examples.put("manualWithAuth",manualWithAuth);
    }

    @Override
    public void addBadExamples(Map<String, CRApproval> examples) {
        examples.put("invalidBadType",badType);
    }


    @Test
    public void shouldDeserializeFromAPILikeObject() {
        String json = "{\n" +
                "    \"type\": \"manual\",\n" +
                "      \"roles\": [\n" +
                "\n" +
                "      ],\n" +
                "      \"users\": [\n" +
                "\n\"joe\"" +
                "      ]\n" +
                "  }";
        CRApproval deserializedValue = gson.fromJson(json,CRApproval.class);

        assertThat(deserializedValue.getType(),is(CRApprovalCondition.manual));
        assertThat(deserializedValue.getUsers().isEmpty(),is(false));
        assertThat(deserializedValue.getRoles().isEmpty(),is(true));

        ErrorCollection errors = deserializedValue.getErrors();
        assertTrue(errors.isEmpty());
    }


    @Test
    public void shouldAppendPrettyLocationInErrors() {
        CRApproval a = new CRApproval();

        ErrorCollection errors = new ErrorCollection();
        a.getErrors(errors,"Pipeline abc");

        String fullError = errors.getErrorsAsText();

        assertThat(fullError,contains("Pipeline abc; Approval"));
        assertThat(fullError,contains("Missing field 'type'."));
    }
}
