/*
 * Copyright 2024 Thoughtworks, Inc.
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

import static org.assertj.core.api.Assertions.assertThat;


public class CRParameterTest extends AbstractCRTest<CRParameter> {
    private final CRParameter validParam1;
    private final CRParameter validParam2;

    private final CRParameter invalidNameNotSet;
    private final CRParameter invalidName;

    public CRParameterTest() {
        validParam1 = new CRParameter("param1", null);
        validParam1.setValue("value");

        validParam2 = new CRParameter("param2", null);

        invalidNameNotSet = new CRParameter();
        invalidNameNotSet.setValue("nameNotSet");

        invalidName = new CRParameter("@E%^^", null);
        invalidName.setValue("invalidName");
    }


    @Override
    public void addGoodExamples(Map<String, CRParameter> examples) {
        examples.put("validParam1", validParam1);
        examples.put("validPram2", validParam2);

    }

    @Override
    public void addBadExamples(Map<String, CRParameter> examples) {
        examples.put("invalidNameNotSet", invalidNameNotSet);
        examples.put("invalidName", invalidName);
    }

    @Test
    public void shouldAddAnErrorIfParameterNameIsBlank() {
        CRParameter crParameter = new CRParameter();
        ErrorCollection errorCollection = new ErrorCollection();
        crParameter.getErrors(errorCollection, "TEST");

        assertThat(errorCollection.getErrorsAsText()).contains("Invalid parameter name 'null'. This must be alphanumeric and can contain underscores, hyphens and periods (however, it cannot start with a period). The maximum allowed length is 255 characters.");
    }

    @Test
    public void shouldAddAnErrorIfParameterNameIsInvalid() {
        CRParameter crParameter = new CRParameter("#$$%@", null);
        ErrorCollection errorCollection = new ErrorCollection();
        crParameter.getErrors(errorCollection, "TEST");

        assertThat(errorCollection.getErrorsAsText()).contains("Invalid parameter name '#$$%@'. This must be alphanumeric and can contain underscores, hyphens and periods (however, it cannot start with a period). The maximum allowed length is 255 characters.");
    }
}
