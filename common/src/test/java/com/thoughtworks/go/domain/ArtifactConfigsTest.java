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

package com.thoughtworks.go.domain;

import com.thoughtworks.go.config.ArtifactConfig;
import com.thoughtworks.go.config.ArtifactConfigs;
import com.thoughtworks.go.config.PluggableArtifactConfig;
import com.thoughtworks.go.config.TestArtifactConfig;
import com.thoughtworks.go.config.validation.FilePathTypeValidator;
import org.hamcrest.Matchers;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static com.thoughtworks.go.config.ArtifactConfig.DEST;
import static com.thoughtworks.go.config.ArtifactConfig.SRC;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;

public class ArtifactConfigsTest {
    @Test
    public void shouldAddDuplicatedArtifactSoThatValidationKicksIn() throws Exception {
        final ArtifactConfigs artifactConfigs = new ArtifactConfigs();
        assertThat(artifactConfigs.size(), is(0));
        artifactConfigs.add(new ArtifactConfig("src", "dest"));
        artifactConfigs.add(new ArtifactConfig("src", "dest"));
        assertThat(artifactConfigs.size(), is(2));
    }

    @Test
    public void shouldLoadArtifactPlans() {
        HashMap<String, String> artifactPlan1 = new HashMap<>();
        artifactPlan1.put(SRC, "blah");
        artifactPlan1.put(DEST, "something");
        artifactPlan1.put("artifactTypeValue", TestArtifactConfig.TEST_PLAN_DISPLAY_NAME);
        HashMap<String, String> artifactPlan2 = new HashMap<>();
        artifactPlan2.put(SRC, "blah2");
        artifactPlan2.put(DEST, "something2");
        artifactPlan2.put("artifactTypeValue", ArtifactConfig.ARTIFACT_PLAN_DISPLAY_NAME);

        List<HashMap> artifactPlansList = new ArrayList<>();
        artifactPlansList.add(artifactPlan1);
        artifactPlansList.add(artifactPlan2);

        ArtifactConfigs artifactConfigs = new ArtifactConfigs();
        artifactConfigs.setConfigAttributes(artifactPlansList);

        assertThat(artifactConfigs.size(), is(2));
        TestArtifactConfig plan = new TestArtifactConfig();
        plan.setSource("blah");
        plan.setDestination("something");
        assertThat(artifactConfigs.get(0), is(plan));
        assertThat(artifactConfigs.get(1), is(new ArtifactConfig("blah2", "something2")));
    }

    @Test
    public void setConfigAttributes_shouldIgnoreEmptySourceAndDest() {
        HashMap<String, String> artifactPlan1 = new HashMap<>();
        artifactPlan1.put(SRC, "blah");
        artifactPlan1.put(DEST, "something");
        artifactPlan1.put("artifactTypeValue", TestArtifactConfig.TEST_PLAN_DISPLAY_NAME);
        HashMap<String, String> artifactPlan2 = new HashMap<>();
        artifactPlan2.put(SRC, "blah2");
        artifactPlan2.put(DEST, "something2");
        artifactPlan2.put("artifactTypeValue", ArtifactConfig.ARTIFACT_PLAN_DISPLAY_NAME);

        HashMap<String, String> artifactPlan3 = new HashMap<>();
        artifactPlan3.put(SRC, "");
        artifactPlan3.put(DEST, "");
        artifactPlan3.put("artifactTypeValue", ArtifactConfig.ARTIFACT_PLAN_DISPLAY_NAME);

        List<HashMap> artifactPlansList = new ArrayList<>();
        artifactPlansList.add(artifactPlan1);
        artifactPlansList.add(artifactPlan3);
        artifactPlansList.add(artifactPlan2);

        ArtifactConfigs artifactConfigs = new ArtifactConfigs();
        artifactConfigs.setConfigAttributes(artifactPlansList);

        assertThat(artifactConfigs.size(), is(2));
        TestArtifactConfig plan = new TestArtifactConfig();
        plan.setSource("blah");
        plan.setDestination("something");
        assertThat(artifactConfigs.get(0), is(plan));
        assertThat(artifactConfigs.get(1), is(new ArtifactConfig("blah2", "something2")));
    }

    @Test
    public void shouldClearAllArtifactsWhenTheMapIsNull() {
        ArtifactConfigs artifactConfigs = new ArtifactConfigs();
        artifactConfigs.add(new ArtifactConfig("src", "dest"));

        artifactConfigs.setConfigAttributes(null);

        assertThat(artifactConfigs.size(), is(0));
    }

    @Test
    public void shouldValidateTree() {
        ArtifactConfigs artifactConfigs = new ArtifactConfigs();
        artifactConfigs.add(new ArtifactConfig("src", "dest"));
        artifactConfigs.add(new ArtifactConfig("src", "dest"));
        artifactConfigs.add(new ArtifactConfig("src", "../a"));

        artifactConfigs.validateTree(null);
        assertThat(artifactConfigs.get(0).errors().on(ArtifactConfig.DEST), is("Duplicate artifacts defined."));
        assertThat(artifactConfigs.get(0).errors().on(ArtifactConfig.SRC), is("Duplicate artifacts defined."));
        assertThat(artifactConfigs.get(1).errors().on(ArtifactConfig.DEST), is("Duplicate artifacts defined."));
        assertThat(artifactConfigs.get(1).errors().on(ArtifactConfig.SRC), is("Duplicate artifacts defined."));
        assertThat(artifactConfigs.get(2).errors().on(ArtifactConfig.DEST), is("Invalid destination path. Destination path should match the pattern " + FilePathTypeValidator.PATH_PATTERN));
    }

    @Test
    public void shouldErrorOutWhenDuplicateArtifactConfigExists() {
        final ArtifactConfigs artifactConfigs = new ArtifactConfigs(new ArtifactConfig("src", "dest"));
        artifactConfigs.add(new ArtifactConfig("src", "dest"));
        artifactConfigs.add(new ArtifactConfig("src", "dest"));

        artifactConfigs.validate(null);

        assertFalse(artifactConfigs.get(0).errors().isEmpty());
        assertThat(artifactConfigs.get(0).errors().on(ArtifactConfig.SRC), Matchers.is("Duplicate artifacts defined."));
        assertThat(artifactConfigs.get(0).errors().on(ArtifactConfig.DEST), Matchers.is("Duplicate artifacts defined."));

        assertFalse(artifactConfigs.get(1).errors().isEmpty());
        assertThat(artifactConfigs.get(1).errors().on(ArtifactConfig.SRC), Matchers.is("Duplicate artifacts defined."));
        assertThat(artifactConfigs.get(1).errors().on(ArtifactConfig.DEST), Matchers.is("Duplicate artifacts defined."));

        assertFalse(artifactConfigs.get(2).errors().isEmpty());
        assertThat(artifactConfigs.get(2).errors().on(ArtifactConfig.SRC), Matchers.is("Duplicate artifacts defined."));
        assertThat(artifactConfigs.get(2).errors().on(ArtifactConfig.DEST), Matchers.is("Duplicate artifacts defined."));
    }

    @Test
    public void getArtifactConfigs_shouldReturnBuiltinArtifactConfigs() {
        ArtifactConfigs allConfigs = new ArtifactConfigs();
        allConfigs.add(new ArtifactConfig("src", "dest"));
        allConfigs.add(new ArtifactConfig("java", null));
        allConfigs.add(new PluggableArtifactConfig("s3", "cd.go.s3"));
        allConfigs.add(new PluggableArtifactConfig("docker", "cd.go.docker"));

        final List<ArtifactConfig> artifactConfigs = allConfigs.getArtifactConfigs();

        assertThat(artifactConfigs, hasSize(2));
        assertThat(artifactConfigs, containsInAnyOrder(
                new ArtifactConfig("src", "dest"),
                new ArtifactConfig("java", null)
        ));
    }

    @Test
    public void getPluggableArtifactConfigs_shouldReturnPluggableArtifactConfigs() {
        ArtifactConfigs allConfigs = new ArtifactConfigs();
        allConfigs.add(new ArtifactConfig("src", "dest"));
        allConfigs.add(new ArtifactConfig("java", null));
        allConfigs.add(new PluggableArtifactConfig("s3", "cd.go.s3"));
        allConfigs.add(new PluggableArtifactConfig("docker", "cd.go.docker"));

        final List<PluggableArtifactConfig> artifactConfigs = allConfigs.getPluggableArtifactConfigs();

        assertThat(artifactConfigs, hasSize(2));
        assertThat(artifactConfigs, containsInAnyOrder(
                new PluggableArtifactConfig("s3", "cd.go.s3"),
                new PluggableArtifactConfig("docker", "cd.go.docker")
        ));
    }

    @Test
    public void findByArtifactId_shouldReturnPluggableArtifactConfigs() {
        ArtifactConfigs allConfigs = new ArtifactConfigs();
        allConfigs.add(new PluggableArtifactConfig("s3", "cd.go.s3"));
        allConfigs.add(new PluggableArtifactConfig("docker", "cd.go.docker"));

        final PluggableArtifactConfig s3 = allConfigs.findByArtifactId("s3");
        assertThat(s3, is(new PluggableArtifactConfig("s3", "cd.go.s3")));
    }

    @Test
    public void findByArtifactId_shouldReturnNullWhenPluggableArtifactConfigNotExistWithGivenId() {
        ArtifactConfigs allConfigs = new ArtifactConfigs();
        allConfigs.add(new PluggableArtifactConfig("s3", "cd.go.s3"));
        allConfigs.add(new PluggableArtifactConfig("docker", "cd.go.docker"));

        final PluggableArtifactConfig s3 = allConfigs.findByArtifactId("foo");
        assertNull(s3);
    }
}
