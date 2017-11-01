/*
 * Copyright 2017 ThoughtWorks, Inc.
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
package com.thoughtworks.go.plugin.access.configrepo.contract;

import com.thoughtworks.go.plugin.access.configrepo.ErrorCollection;
import com.thoughtworks.go.plugin.access.configrepo.contract.CRBaseTest;
import com.thoughtworks.go.plugin.access.configrepo.contract.CRTimer;
import org.junit.Test;

import java.util.Map;

import static junit.framework.TestCase.assertTrue;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class CRTimerTest extends CRBaseTest<CRTimer> {

    private final CRTimer timer;
    private final CRTimer invalidNoTimerSpec;

    public CRTimerTest()
    {
        timer = new CRTimer("0 15 10 * * ? *");

        invalidNoTimerSpec = new CRTimer();
    }

    @Override
    public void addGoodExamples(Map<String, CRTimer> examples) {
        examples.put("timer",timer);
    }

    @Override
    public void addBadExamples(Map<String, CRTimer> examples) {
        examples.put("invalidNoTimerSpec",invalidNoTimerSpec);
    }

    @Test
    public void shouldDeserializeFromAPILikeObject()
    {
        String json = "{\n" +
                "    \"spec\": \"0 0 22 ? * MON-FRI\",\n" +
                "    \"only_on_changes\": true\n" +
                "  }";
        CRTimer deserializedValue = gson.fromJson(json,CRTimer.class);

        assertThat(deserializedValue.getTimerSpec(),is("0 0 22 ? * MON-FRI"));
        assertThat(deserializedValue.isOnlyOnChanges(),is(true));

        ErrorCollection errors = deserializedValue.getErrors();
        assertTrue(errors.isEmpty());
    }
}