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
package com.thoughtworks.go.remote.work;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.SecretParam;
import com.thoughtworks.go.config.exceptions.UnresolvedSecretParamException;
import com.thoughtworks.go.config.materials.dependency.DependencyMaterial;
import com.thoughtworks.go.config.materials.svn.SvnMaterial;
import com.thoughtworks.go.domain.JobIdentifier;
import com.thoughtworks.go.domain.MaterialRevision;
import com.thoughtworks.go.domain.MaterialRevisions;
import com.thoughtworks.go.domain.materials.dependency.DependencyMaterialRevision;
import com.thoughtworks.go.helper.MaterialsMother;
import com.thoughtworks.go.helper.ModificationsMother;
import com.thoughtworks.go.util.SystemEnvironment;
import com.thoughtworks.go.util.TempDirUtils;
import com.thoughtworks.go.util.command.EnvironmentVariableContext;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;
import uk.org.webcompere.systemstubs.properties.SystemProperties;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Date;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

@ExtendWith(SystemStubsExtension.class)
public class EnvironmentVariableContextTest {
    @TempDir
    Path tempDir;

    @SystemStub
    private SystemProperties systemProperties;

    private String pipelineName = "pipeline-name";
    private String pipelineLabel = "pipeline-label";
    private String stageName = "stage-name";
    private String stageCounter = "stage-counter";
    private String jobName = "build-name";

    @Test
    void shouldPopulateEnvironmentForServerUrl() {
        new SystemEnvironment().setProperty("serviceUrl", "some_random_place");

        EnvironmentVariableContext context = new EnvironmentVariableContext();

        context.setProperty("GO_SERVER_URL", SystemEnvironment.getProperty("serviceUrl"), false);
        jobIdentifier().populateEnvironmentVariables(context);

        assertThat(context.getProperty("GO_SERVER_URL")).isEqualTo("some_random_place");
    }

    @Test
    void shouldPopulateEnvironmentForJobIdentifier() {
        EnvironmentVariableContext context = new EnvironmentVariableContext();

        context.setProperty("GO_SERVER_URL", SystemEnvironment.getProperty("serviceUrl"), false);
        jobIdentifier().populateEnvironmentVariables(context);

        assertThat(context.getProperty("GO_PIPELINE_NAME")).isEqualTo(pipelineName);
        assertThat(context.getProperty("GO_PIPELINE_LABEL")).isEqualTo(pipelineLabel);
        assertThat(context.getProperty("GO_STAGE_NAME")).isEqualTo(stageName);
        assertThat(context.getProperty("GO_STAGE_COUNTER")).isEqualTo(stageCounter);
        assertThat(context.getProperty("GO_JOB_NAME")).isEqualTo(jobName);
    }

    @Test
    void shouldPopulateEnvironmentForMaterialUsingMaterialName() throws IOException {
        SvnMaterial svn = MaterialsMother.svnMaterial();
        svn.setName(new CaseInsensitiveString("svn"));
        svn.setFolder("svn-dir");
        MaterialRevision revision = new MaterialRevision(svn, ModificationsMother.oneModifiedFile("revision1"));
        MaterialRevisions materialRevisions = new MaterialRevisions(revision);

        EnvironmentVariableContext context = new EnvironmentVariableContext();

        context.setProperty("GO_SERVER_URL", SystemEnvironment.getProperty("serviceUrl"), false);
        jobIdentifier().populateEnvironmentVariables(context);

        materialRevisions.populateEnvironmentVariables(context, TempDirUtils.createRandomDirectoryIn(tempDir).toFile());

        assertThat(context.getProperty("GO_REVISION_SVN")).isEqualTo("revision1");
        assertThat(context.getProperty("GO_MATERIAL_SVN_HAS_CHANGED")).isEqualTo("false");
    }

    @Test
    void shouldPopulateEnvironmentForMaterialUsingDest() throws IOException {
        SvnMaterial svn = MaterialsMother.svnMaterial();
        svn.setFolder("svn-dir");
        MaterialRevision revision = new MaterialRevision(svn,
                ModificationsMother.oneModifiedFile("revision1"));
        MaterialRevisions materialRevisions = new MaterialRevisions(revision);

        EnvironmentVariableContext context = new EnvironmentVariableContext();

        context.setProperty("GO_SERVER_URL", SystemEnvironment.getProperty("serviceUrl"), false);
        jobIdentifier().populateEnvironmentVariables(context);

        materialRevisions.populateEnvironmentVariables(context, TempDirUtils.createRandomDirectoryIn(tempDir).toFile());

        assertThat(context.getProperty("GO_REVISION_SVN_DIR")).isEqualTo("revision1");
    }

    @Test
    void shouldPopulateEnvironmentForDependencyMaterialUsingMaterialName() throws IOException {
        String materialName = "upstreamPipeline";
        MaterialRevision revision = materialRevision(materialName, "pipeline-name", 1, "pipeline-label", "stage-name", 1);
        MaterialRevisions materialRevisions = new MaterialRevisions(revision);

        EnvironmentVariableContext context = new EnvironmentVariableContext();

        context.setProperty("GO_SERVER_URL", SystemEnvironment.getProperty("serviceUrl"), false);
        jobIdentifier().populateEnvironmentVariables(context);

        materialRevisions.populateEnvironmentVariables(context, TempDirUtils.createRandomDirectoryIn(tempDir).toFile());

        assertThat(context.getProperty("GO_DEPENDENCY_LABEL_UPSTREAMPIPELINE")).isEqualTo("pipeline-label");
        assertThat(context.getProperty("GO_DEPENDENCY_LOCATOR_UPSTREAMPIPELINE")).isEqualTo("pipeline-name/1/stage-name/1");
    }

    @Test
    void shouldPopulateEnvironmentForDependencyMaterialUsingPipelineNameStageName() throws IOException {
        String EMPTY_NAME = "";
        MaterialRevision revision = materialRevision(EMPTY_NAME, "pipeline-name", 1, "pipeline-label", "stage-name", 1);
        MaterialRevisions materialRevisions = new MaterialRevisions(revision);

        EnvironmentVariableContext context = new EnvironmentVariableContext();

        context.setProperty("GO_SERVER_URL", SystemEnvironment.getProperty("serviceUrl"), false);
        jobIdentifier().populateEnvironmentVariables(context);

        materialRevisions.populateEnvironmentVariables(context, TempDirUtils.createRandomDirectoryIn(tempDir).toFile());

        assertThat(context.getProperty("GO_DEPENDENCY_LABEL_PIPELINE_NAME")).isEqualTo("pipeline-label");
        assertThat(context.getProperty("GO_DEPENDENCY_LOCATOR_PIPELINE_NAME")).isEqualTo("pipeline-name/1/stage-name/1");
    }

    @Test
    void shouldConsiderEnvironmentVariableSecureIfItHasSecretParams() {
        EnvironmentVariableContext context = new EnvironmentVariableContext();
        context.setProperty("GO_SERVER_URL", "{{SECRET:[secret_config_id][test]}}", false);

        assertThat(context.getSecureEnvironmentVariables()).hasSize(1);
        assertThat(context.getSecureEnvironmentVariables().get(0).isSecure()).isTrue();
    }

    @Nested
    class hasSecretParams {
        @Test
        void shouldBeTrueIfEnvironmentVariableContextHasSecretParams() {
            EnvironmentVariableContext environmentVariableContext = new EnvironmentVariableContext();
            environmentVariableContext.setProperty("Foo", "{{SECRET:[secret_config_id][lookup_password]}}", true);
            environmentVariableContext.setProperty("Bar", "some-value", false);

            assertThat(environmentVariableContext.hasSecretParams()).isTrue();
        }

        @Test
        void shouldBeFalseIfNoneOfTheEnvironmentVariablesInEnvironmentVariablesContextHasSecretParams() {
            EnvironmentVariableContext environmentVariableContext = new EnvironmentVariableContext();
            environmentVariableContext.setProperty("Foo", "plain-text", true);
            environmentVariableContext.setProperty("Bar", "some-value", false);

            assertThat(environmentVariableContext.hasSecretParams()).isFalse();
        }
    }

    @Nested
    class getSecretParams {
        @Test
        void shouldReturnAListOfSecretParams() {
            EnvironmentVariableContext environmentVariableContext = new EnvironmentVariableContext();
            environmentVariableContext.setProperty("Foo", "{{SECRET:[secret_config_id][username]}}@{{SECRET:[secret_config_id][password]}}", true);
            environmentVariableContext.setProperty("Baz", "{{SECRET:[secret_config_id][test]}}", false);
            environmentVariableContext.setProperty("Bar", "some-value", false);

            assertThat(environmentVariableContext.getSecretParams())
                    .hasSize(3)
                    .contains(
                            new SecretParam("secret_config_id", "username"),
                            new SecretParam("secret_config_id", "password"),
                            new SecretParam("secret_config_id", "test")
                    );
        }

        @Test
        void shouldBeAnEmptyListInAbsenceOfSecretParamsInEnvironmentVariablesContext() {
            EnvironmentVariableContext environmentVariableContext = new EnvironmentVariableContext();
            environmentVariableContext.setProperty("Foo", "plain-text", true);
            environmentVariableContext.setProperty("Bar", "some-value", false);

            assertThat(environmentVariableContext.getSecretParams()).hasSize(0);
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    class GetValue {

        Stream<Arguments> secretCombinationsToTest() {
            return Stream.of(
                    Arguments.of("{{SECRET:[id][password]}}", "some-password"),
                    Arguments.of("abc_{{SECRET:[id][username]}}", "abc_some-username"),
                    Arguments.of("abc_{{SECRET:[id][username]}}_xyz", "abc_some-username_xyz"),
                    Arguments.of("{{SECRET:[id][username]}}:{{SECRET:[id][password]}}", "some-username:some-password"),
                    Arguments.of("abc_{{SECRET:[id][username]}}@foo@{{SECRET:[id][password]}}_xyz", "abc_some-username@foo@some-password_xyz")
            );
        }

        @ParameterizedTest
        @MethodSource("secretCombinationsToTest")
        void shouldReturnResolvedValues(String secretPattern, String expectedResultPostSubstitution) {
            EnvironmentVariableContext environmentVariableContext = new EnvironmentVariableContext();
            environmentVariableContext.setProperty("Foo", secretPattern, true);
            environmentVariableContext.setProperty("Bar", "some-value", false);

            assertThatCode(() -> environmentVariableContext.getProperty("Foo"))
                    .isInstanceOf(UnresolvedSecretParamException.class);
            assertThat(environmentVariableContext.getProperty("Bar")).isEqualTo("some-value");

            environmentVariableContext.getSecretParams().findFirst("password").ifPresent(s -> s.setValue("some-password"));
            environmentVariableContext.getSecretParams().findFirst("username").ifPresent(s -> s.setValue("some-username"));

            assertThat(environmentVariableContext.getProperty("Foo")).isEqualTo(expectedResultPostSubstitution);
            assertThat(environmentVariableContext.getProperty("Bar")).isEqualTo("some-value");
        }

        @Test
        void shouldThrowWhenEnvironmentVariableIsUsedBeforeItIsResolved() {
            EnvironmentVariableContext environmentVariableContext = new EnvironmentVariableContext();
            environmentVariableContext.setProperty("Foo", "{{SECRET:[id][password]}}", true);
            environmentVariableContext.setProperty("Bar", "some-value", false);

            assertThat(environmentVariableContext.getProperty("Bar")).isEqualTo("some-value");
            assertThatCode(() -> environmentVariableContext.getProperty("Foo"))
                    .isInstanceOf(UnresolvedSecretParamException.class)
                    .hasMessage("SecretParam 'password' is used before it is resolved.");
        }
    }

    private MaterialRevision materialRevision(String materialName, String pipelineName, Integer pipelineCounter,
                                              String pipelineLabel,
                                              String stageName, int stageCounter) {
        DependencyMaterial material = new DependencyMaterial(new CaseInsensitiveString(pipelineName), new CaseInsensitiveString(stageName));
        if (!StringUtils.isEmpty(materialName)) {
            material.setName(new CaseInsensitiveString(materialName));
        }

        DependencyMaterialRevision revision = DependencyMaterialRevision.create(pipelineName, pipelineCounter,
                pipelineLabel, stageName, stageCounter);
        MaterialRevision materialRevision = revision.convert(material, new Date());
        return materialRevision;
    }

    private JobIdentifier jobIdentifier() {
        JobIdentifier jobIdentifier = new JobIdentifier(pipelineName, 1, pipelineLabel, stageName, stageCounter, jobName, 1L);
        return jobIdentifier;
    }
}
