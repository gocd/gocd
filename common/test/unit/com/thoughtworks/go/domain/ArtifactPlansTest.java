/*
 * Copyright 2015 ThoughtWorks, Inc.
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
 */

package com.thoughtworks.go.domain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static com.thoughtworks.go.config.ArtifactPlan.DEST;
import static com.thoughtworks.go.config.ArtifactPlan.SRC;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import com.thoughtworks.go.config.ArtifactPlan;
import com.thoughtworks.go.config.ArtifactPlans;
import com.thoughtworks.go.config.TestArtifactPlan;
import com.thoughtworks.go.config.validation.FilePathTypeValidator;
import org.junit.Test;

public class ArtifactPlansTest {
    @Test
    public void shouldAddDuplicatedArtifactSoThatValidationKicksIn() throws Exception {
        final ArtifactPlans artifactPlans = new ArtifactPlans();
        assertThat(artifactPlans.size(), is(0));
        artifactPlans.add(new ArtifactPlan("src", "dest"));
        artifactPlans.add(new ArtifactPlan("src", "dest"));
        assertThat(artifactPlans.size(), is(2));
    }

    @Test
    public void shouldLoadArtifactPlans() {
        HashMap<String, String> artifactPlan1 = new HashMap<String, String>();
        artifactPlan1.put(SRC, "blah");
        artifactPlan1.put(DEST, "something");
        artifactPlan1.put("artifactTypeValue", TestArtifactPlan.TEST_PLAN_DISPLAY_NAME);
        HashMap<String, String> artifactPlan2 = new HashMap<String, String>();
        artifactPlan2.put(SRC, "blah2");
        artifactPlan2.put(DEST, "something2");
        artifactPlan2.put("artifactTypeValue", ArtifactPlan.ARTIFACT_PLAN_DISPLAY_NAME);

        List<HashMap> artifactPlansList = new ArrayList<HashMap>();
        artifactPlansList.add(artifactPlan1);
        artifactPlansList.add(artifactPlan2);

        ArtifactPlans artifactPlans = new ArtifactPlans();
        artifactPlans.setConfigAttributes(artifactPlansList);

        assertThat(artifactPlans.size(), is(2));
        TestArtifactPlan plan = new TestArtifactPlan();
        plan.setSrc("blah");
        plan.setDest("something");
        assertThat((TestArtifactPlan) artifactPlans.get(0), is(plan));
        assertThat(artifactPlans.get(1), is(new ArtifactPlan("blah2", "something2")));
    }

    @Test
    public void setConfigAttributes_shouldIgnoreEmptySourceAndDest() {
        HashMap<String, String> artifactPlan1 = new HashMap<String, String>();
        artifactPlan1.put(SRC, "blah");
        artifactPlan1.put(DEST, "something");
        artifactPlan1.put("artifactTypeValue", TestArtifactPlan.TEST_PLAN_DISPLAY_NAME);
        HashMap<String, String> artifactPlan2 = new HashMap<String, String>();
        artifactPlan2.put(SRC, "blah2");
        artifactPlan2.put(DEST, "something2");
        artifactPlan2.put("artifactTypeValue", ArtifactPlan.ARTIFACT_PLAN_DISPLAY_NAME);

        HashMap<String, String> artifactPlan3 = new HashMap<String, String>();
        artifactPlan3.put(SRC, "");
        artifactPlan3.put(DEST, "");
        artifactPlan3.put("artifactTypeValue", ArtifactPlan.ARTIFACT_PLAN_DISPLAY_NAME);

        List<HashMap> artifactPlansList = new ArrayList<HashMap>();
        artifactPlansList.add(artifactPlan1);
        artifactPlansList.add(artifactPlan3);
        artifactPlansList.add(artifactPlan2);

        ArtifactPlans artifactPlans = new ArtifactPlans();
        artifactPlans.setConfigAttributes(artifactPlansList);

        assertThat(artifactPlans.size(), is(2));
        TestArtifactPlan plan = new TestArtifactPlan();
        plan.setSrc("blah");
        plan.setDest("something");
        assertThat((TestArtifactPlan) artifactPlans.get(0), is(plan));
        assertThat(artifactPlans.get(1), is(new ArtifactPlan("blah2", "something2")));
    }

    @Test
    public void shouldClearAllArtifactsWhenTheMapIsNull() {
        ArtifactPlans artifactPlans = new ArtifactPlans();
        artifactPlans.add(new ArtifactPlan("src", "dest"));

        artifactPlans.setConfigAttributes(null);

        assertThat(artifactPlans.size(), is(0));
    }

    @Test
    public void shouldValidateTree(){
        ArtifactPlans artifactPlans = new ArtifactPlans();
        artifactPlans.add(new ArtifactPlan("src", "dest"));
        artifactPlans.add(new ArtifactPlan("src", "dest"));
        artifactPlans.add(new ArtifactPlan("src", "../a"));

        artifactPlans.validateTree(null);
        assertThat(artifactPlans.get(0).errors().on(ArtifactPlan.DEST), is("Duplicate artifacts defined."));
        assertThat(artifactPlans.get(0).errors().on(ArtifactPlan.SRC), is("Duplicate artifacts defined."));
        assertThat(artifactPlans.get(1).errors().on(ArtifactPlan.DEST), is("Duplicate artifacts defined."));
        assertThat(artifactPlans.get(1).errors().on(ArtifactPlan.SRC), is("Duplicate artifacts defined."));
        assertThat(artifactPlans.get(2).errors().on(ArtifactPlan.DEST), is("Invalid destination path. Destination path should match the pattern " + FilePathTypeValidator.PATH_PATTERN));
    }
}
