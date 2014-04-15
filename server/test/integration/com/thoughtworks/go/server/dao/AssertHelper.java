/*************************GO-LICENSE-START*********************************
 * Copyright 2014 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *************************GO-LICENSE-END***********************************/

package com.thoughtworks.go.server.dao;

import java.util.Collection;

import com.thoughtworks.go.config.ArtifactPlan;
import com.thoughtworks.go.config.ArtifactPlans;
import com.thoughtworks.go.config.Resources;
import com.thoughtworks.go.domain.JobStateTransition;
import com.thoughtworks.go.domain.JobStateTransitions;
import com.thoughtworks.go.domain.Properties;
import com.thoughtworks.go.domain.Property;
import com.thoughtworks.go.config.Resource;

import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertThat;

public class AssertHelper {

    private static void assertTransitions(JobStateTransitions actual, JobStateTransitions expect) {
        String msg = "BuildStateTransitions should match";
        nullsafeAssert(msg, actual, expect);
        assertThat(actual.size(), is(expect.size()));
        for (JobStateTransition expectTransition : expect) {
            JobStateTransition actualTransition = actual.byState(expectTransition.getCurrentState());
            assertThat(msg, actualTransition, is(not(nullValue())));
            assertThat(msg, actualTransition.getStateChangeTime(),
                    is(expectTransition.getStateChangeTime()));
        }
    }

    private static void assertArtifactPlans(ArtifactPlans actual, ArtifactPlans expect) {
        String msg = "ArtifactPlans should match";
        nullsafeAssert(msg, actual, expect);
        assertThat(actual.size(), is(expect.size()));
        for (int i = 0; i < expect.size(); i++) {
            ArtifactPlan expectPlan = expect.get(i);
            ArtifactPlan actualPlan = actual.get(i);
            assertThat(msg, actualPlan.getArtifactType(), is(expectPlan.getArtifactType()));
            assertThat(msg, actualPlan.getSrc(), is(expectPlan.getSrc()));
            assertThat(msg, actualPlan.getDest(), is(expectPlan.getDest()));
        }
    }

    private static void assertResources(Resources actual, Resources expect) {
        assertThat(actual.size(), is(expect.size()));
        for (int i = 0; i < expect.size(); i++) {
            Resource expectResource = expect.get(i);
            Resource actualResource = actual.get(i);
            assertThat("Resources should match", actualResource.getName(), is(expectResource.getName()));
        }
    }

    private static void assertProperties(Properties actual, Properties expect) {
        String msg = "Properties should match";
        nullsafeAssert(msg, actual, expect);
        assertThat(actual.size(), is(expect.size()));
        for (int i = 0; i < expect.size(); i++) {
            Property expectProperty = expect.get(i);
            Property actualProperty = actual.get(i);
            assertThat(msg, actualProperty.getKey(), is(expectProperty.getKey()));
            assertThat(msg, actualProperty.getValue(), is(expectProperty.getValue()));
        }
    }

    private static void nullsafeAssert(String msg, Collection actual, Collection expect) {
        if (actual == null) {
            assertThat(msg, expect == null || expect.size() == 0, is(true));
        }
        if (expect == null) {
            assertThat(msg, actual == null || actual.size() == 0, is(true));
        }
    }
}
