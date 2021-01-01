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

package com.thoughtworks.go.apiv1.templateauthorization.representers

import com.thoughtworks.go.apiv1.internalagent.representers.WorkRepresenter
import com.thoughtworks.go.config.*
import com.thoughtworks.go.config.elastic.ClusterProfile
import com.thoughtworks.go.config.elastic.ElasticProfile
import com.thoughtworks.go.domain.*
import com.thoughtworks.go.domain.buildcause.BuildCause
import com.thoughtworks.go.domain.builder.*
import com.thoughtworks.go.domain.config.ConfigurationProperty
import com.thoughtworks.go.domain.materials.Material
import com.thoughtworks.go.domain.packagerepository.ConfigurationPropertyMother
import com.thoughtworks.go.helper.MaterialsMother
import com.thoughtworks.go.helper.ModificationsMother
import com.thoughtworks.go.remote.work.*
import com.thoughtworks.go.util.command.EnvironmentVariableContext
import org.apache.commons.lang.builder.EqualsBuilder
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

import static com.thoughtworks.go.domain.packagerepository.ConfigurationPropertyMother.create
import static com.thoughtworks.go.helper.MaterialsMother.*
import static org.assertj.core.api.Assertions.assertThat

class WorkRepresenterTest {
    @Test
    void 'should ensure the serialized and deserialized DeniedAgentWork objects are same'() {
        def work = new DeniedAgentWork("uuid")
        def workJSON = WorkRepresenter.toJSON(work)

        assertThat(EqualsBuilder.reflectionEquals(work, WorkRepresenter.fromJSON(workJSON))).isTrue()
    }

    @Test
    void 'should ensure the serialized and deserialized NoWork objects are same'() {
        def work = new NoWork()
        def workJSON = WorkRepresenter.toJSON(work)

        assertThat(EqualsBuilder.reflectionEquals(work, WorkRepresenter.fromJSON(workJSON))).isTrue()
    }

    @Test
    void 'should ensure the serialized and deserialized UnregisteredAgentWork objects are same'() {
        def work = new UnregisteredAgentWork("uuid")
        def workJSON = WorkRepresenter.toJSON(work)

        assertThat(EqualsBuilder.reflectionEquals(work, WorkRepresenter.fromJSON(workJSON))).isTrue()
    }

    @Nested
    class Build_Work {
        @Test
        void 'should ensure the serialized and deserialized BuildWork objects are same'() {
            def buildCause = BuildCause.createWithModifications(materialRevisions(), "user1")
            def assignment = BuildAssignment.create(jobPlan(), buildCause,builders(), null, new EnvironmentVariableContext(), artifactStores())

            def work = new com.thoughtworks.go.remote.work.BuildWork(assignment, "utf-8")
            def workJSON = WorkRepresenter.toJSON(work)

            BuildWork deserializedWork = WorkRepresenter.fromJSON(workJSON)

            def assignmentBeforeSerialization = work.getAssignment()
            def deserializedAssignment = deserializedWork.getAssignment()

            assertThat(assignmentBeforeSerialization.shouldFetchMaterials()).isEqualTo(deserializedAssignment.shouldFetchMaterials())
            assertThat(assignmentBeforeSerialization.shouldCleanWorkingDir()).isEqualTo(deserializedAssignment.shouldCleanWorkingDir())
            assertThat(EqualsBuilder.reflectionEquals(assignmentBeforeSerialization.getArtifactPlans(), deserializedAssignment.getArtifactPlans()))
            assertThat(EqualsBuilder.reflectionEquals(assignmentBeforeSerialization.materialRevisions(), deserializedAssignment.materialRevisions()))
            assertThat(EqualsBuilder.reflectionEquals(assignmentBeforeSerialization.initialEnvironmentVariableContext(), deserializedAssignment.initialEnvironmentVariableContext()))
            assertThat(EqualsBuilder.reflectionEquals(assignmentBeforeSerialization.getArtifactStores(), deserializedAssignment.getArtifactStores()))

            assertThat(EqualsBuilder.reflectionEquals(assignmentBeforeSerialization.getBuilders().get(0), deserializedAssignment.getBuilders().get(0))).isTrue()
            assertThat(EqualsBuilder.reflectionEquals(assignmentBeforeSerialization.getBuilders().get(1), deserializedAssignment.getBuilders().get(1))).isTrue()
            //BuilderForKillAllChildTask cannot be compared using reflectionEquals, hence using equals
            assertThat(assignmentBeforeSerialization.getBuilders().get(2).equals(deserializedAssignment.getBuilders().get(2))).isTrue()
            assertThat(EqualsBuilder.reflectionEquals(assignmentBeforeSerialization.getBuilders().get(3), deserializedAssignment.getBuilders().get(3))).isTrue()
        }
    }


    private MaterialRevisions materialRevisions() {
        def materials = new HashMap<Material, String>()
        materials.put(gitMaterial("http://somegitrepo.com"), "rev1")
        materials.put(svnMaterial(), "rev2")
        materials.put(hgMaterial(), "rev3")
        materials.put(p4Material(), "rev4")
        materials.put(tfsMaterial("https://tfsrepo"), "rev5")
        materials.put(packageMaterial(), "rev6")
        materials.put(pluggableSCMMaterial(), "rev8")

        def dependencyMaterial = MaterialsMother.dependencyMaterial()
        def dependencyMaterialRevision = ModificationsMother.dependencyMaterialRevision(0,
                "label", 1, dependencyMaterial, new Date());
        def revisions = ModificationsMother.getMaterialRevisions(materials)
        revisions.addRevision(dependencyMaterialRevision)

        return revisions
    }

    private JobPlan jobPlan() {
        def resources = new Resources("r1,r2,r3")
        def jobIdentifier = new JobIdentifier("up_42", 100, "100", "up42_stage",
                "1", "some_job", 1111L)
        ConfigurationProperty prop1 = ConfigurationPropertyMother.create("key1", true,"secret1");
        ConfigurationProperty prop2 = ConfigurationPropertyMother.create("key2", false,"non_secret");
        def clusterProfile = new ClusterProfile("prod-cluster", "plugin_id", prop1, prop2)
        ElasticProfile elasticProfile = new ElasticProfile("docker.unit-test", "prod-cluster", prop1, prop2);

        return new DefaultJobPlan(resources, artifactPlans(), 10L, jobIdentifier, "agent_uuid",
                new EnvironmentVariables(), new EnvironmentVariables(), elasticProfile, clusterProfile)
    }

    private List<Builder> builders() {
        String[] args = { "some thing" };
        return List.of(
                new CommandBuilder("echo", "some thing", null, new RunIfConfigs(RunIfConfig.ANY),
                        null, "some desc"),
                new CommandBuilderWithArgList("echo", args, null, new RunIfConfigs(RunIfConfig.ANY),
                        null, "some desc"),
                new BuilderForKillAllChildTask(),
                new NullBuilder()
        )
    }

    private List<ArtifactPlan> artifactPlans() {
        return ArtifactPlan.toArtifactPlans(new ArtifactTypeConfigs(Arrays.asList(
                new BuildArtifactConfig("source", "destination"),
                new TestArtifactConfig("test-source", "test-destination"),
                new PluggableArtifactConfig("id", "storeId", create("Foo", true, "Bar"))
        )))
    }

    private ArtifactStores artifactStores() {
        return new ArtifactStores(new ArtifactStore("storeId", "plugin_id", create("username", true, "some_value")))

    }
}
