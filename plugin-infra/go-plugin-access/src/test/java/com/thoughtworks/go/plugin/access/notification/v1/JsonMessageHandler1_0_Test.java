/*
 * Copyright Thoughtworks, Inc.
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
package com.thoughtworks.go.plugin.access.notification.v1;

import com.thoughtworks.go.config.materials.PackageMaterial;
import com.thoughtworks.go.config.materials.PluggableSCMMaterial;
import com.thoughtworks.go.domain.*;
import com.thoughtworks.go.domain.notificationdata.AgentNotificationData;
import com.thoughtworks.go.domain.notificationdata.StageNotificationData;
import com.thoughtworks.go.domain.packagerepository.PackageDefinition;
import com.thoughtworks.go.domain.scm.SCM;
import com.thoughtworks.go.helper.PipelineMother;
import com.thoughtworks.go.plugin.api.response.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static com.thoughtworks.go.plugin.access.notification.v1.StageConverter.DATE_PATTERN;
import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.fail;

public class JsonMessageHandler1_0_Test {
    private JsonMessageHandler1_0 messageHandler;

    @BeforeEach
    public void setUp() {
        messageHandler = new JsonMessageHandler1_0();
    }

    @Test
    public void shouldBuildNotificationsInterestedInFromResponseBody() {
        String responseBody = "{notifications=[\"pipeline-status\",\"stage-status\"]}";
        List<String> notificationsInterestedIn = messageHandler.responseMessageForNotificationsInterestedIn(responseBody);

        assertThat(notificationsInterestedIn).isEqualTo(List.of("pipeline-status", "stage-status"));
    }

    @Test
    public void shouldValidateIncorrectJsonResponseForNotificationsInterestedIn() {
        assertThat(errorMessageForNotificationsInterestedIn("{\"notifications\":{}}")).isEqualTo("Unable to de-serialize json response. 'notifications' should be of type list of string");
        assertThat(errorMessageForNotificationsInterestedIn("{\"notifications\":[{},{}]}")).isEqualTo("Unable to de-serialize json response. Notification 'name' should be of type string");
    }

    @Test
    public void shouldBuildSuccessResultFromNotify() {
        String responseBody = "{\"status\":\"success\",messages=[\"message-one\",\"message-two\"]}";
        Result result = messageHandler.responseMessageForNotify(responseBody);

        assertSuccessResult(result, List.of("message-one", "message-two"));
    }

    @Test
    public void shouldBuildFailureResultFromNotify() {
        String responseBody = "{\"status\":\"failure\",messages=[\"message-one\",\"message-two\"]}";
        Result result = messageHandler.responseMessageForNotify(responseBody);

        assertFailureResult(result, List.of("message-one", "message-two"));
    }

    @Test
    public void shouldHandleNullMessagesForNotify() {
        assertSuccessResult(messageHandler.responseMessageForNotify("{\"status\":\"success\"}"), new ArrayList<>());
        assertFailureResult(messageHandler.responseMessageForNotify("{\"status\":\"failure\"}"), new ArrayList<>());
    }

    @Test
    public void shouldValidateIncorrectJsonForResult() {
        assertThat(errorMessageForNotifyResult("")).isEqualTo("Unable to de-serialize json response. Empty response body");
        assertThat(errorMessageForNotifyResult("[{\"result\":\"success\"}]")).isEqualTo("Unable to de-serialize json response. Notify result should be returned as map, with key represented as string and messages represented as list");
        assertThat(errorMessageForNotifyResult("{\"status\":true}")).isEqualTo("Unable to de-serialize json response. Notify result 'status' should be of type string");
        assertThat(errorMessageForNotifyResult("{\"result\":true}")).isEqualTo("Unable to de-serialize json response. Notify result 'status' is a required field");

        assertThat(errorMessageForNotifyResult("{\"status\":\"success\",\"messages\":{}}")).isEqualTo("Unable to de-serialize json response. Notify result 'messages' should be of type list of string");
        assertThat(errorMessageForNotifyResult("{\"status\":\"success\",\"messages\":[{},{}]}")).isEqualTo("Unable to de-serialize json response. Notify result 'message' should be of type string");
    }

    @Test
    public void shouldConstructTheStageNotificationRequest() throws Exception {
        Pipeline pipeline = createPipeline();
        String gitModifiedTime = new SimpleDateFormat(DATE_PATTERN).format(pipeline.getBuildCause().getMaterialRevisions().getMaterialRevision(0).getLatestModification().getModifiedTime());
        String hgModifiedTime = new SimpleDateFormat(DATE_PATTERN).format(pipeline.getBuildCause().getMaterialRevisions().getMaterialRevision(1).getLatestModification().getModifiedTime());
        String svnModifiedTime = new SimpleDateFormat(DATE_PATTERN).format(pipeline.getBuildCause().getMaterialRevisions().getMaterialRevision(2).getLatestModification().getModifiedTime());
        String tfsModifiedTime = new SimpleDateFormat(DATE_PATTERN).format(pipeline.getBuildCause().getMaterialRevisions().getMaterialRevision(3).getLatestModification().getModifiedTime());
        String p4ModifiedTime = new SimpleDateFormat(DATE_PATTERN).format(pipeline.getBuildCause().getMaterialRevisions().getMaterialRevision(4).getLatestModification().getModifiedTime());
        String dependencyModifiedTime = new SimpleDateFormat(DATE_PATTERN).format(pipeline.getBuildCause().getMaterialRevisions().getMaterialRevision(5).getLatestModification().getModifiedTime());
        String packageMaterialModifiedTime = new SimpleDateFormat(DATE_PATTERN).format(pipeline.getBuildCause().getMaterialRevisions().getMaterialRevision(6).getLatestModification().getModifiedTime());
        String pluggableScmModifiedTime = new SimpleDateFormat(DATE_PATTERN).format(pipeline.getBuildCause().getMaterialRevisions().getMaterialRevision(7).getLatestModification().getModifiedTime());
        String expected = """
            {
            \t"pipeline": {
            \t\t"name": "pipeline-name",
            \t\t"counter": "1",
            \t\t"group": "pipeline-group",
            \t\t"build-cause": [{
            \t\t\t"material": {
            \t\t\t\t"git-configuration": {
            \t\t\t\t\t"shallow-clone": false,
            \t\t\t\t\t"branch": "branch",
            \t\t\t\t\t"url": "http://user:******@gitrepo.com"
            \t\t\t\t},
            \t\t\t\t"type": "git"
            \t\t\t},
            \t\t\t"changed": true,
            \t\t\t"modifications": [{
            \t\t\t\t"revision": "1",
            \t\t\t\t"modified-time": "%s",
            \t\t\t\t"data": {}
            \t\t\t}]
            \t\t}, {
            \t\t\t"material": {
            \t\t\t\t"type": "mercurial",
            \t\t\t\t"mercurial-configuration": {
            \t\t\t\t\t"url": "http://user:******@hgrepo.com"
            \t\t\t\t}
            \t\t\t},
            \t\t\t"changed": true,
            \t\t\t"modifications": [{
            \t\t\t\t"revision": "1",
            \t\t\t\t"modified-time": "%s",
            \t\t\t\t"data": {}
            \t\t\t}]
            \t\t}, {
            \t\t\t"material": {
            \t\t\t\t"svn-configuration": {
            \t\t\t\t\t"check-externals": false,
            \t\t\t\t\t"url": "http://user:******@svnrepo.com",
            \t\t\t\t\t"username": "username"
            \t\t\t\t},
            \t\t\t\t"type": "svn"
            \t\t\t},
            \t\t\t"changed": true,
            \t\t\t"modifications": [{
            \t\t\t\t"revision": "1",
            \t\t\t\t"modified-time": "%s",
            \t\t\t\t"data": {}
            \t\t\t}]
            \t\t}, {
            \t\t\t"material": {
            \t\t\t\t"type": "tfs",
            \t\t\t\t"tfs-configuration": {
            \t\t\t\t\t"domain": "domain",
            \t\t\t\t\t"project-path": "project-path",
            \t\t\t\t\t"url": "http://user:******@tfsrepo.com",
            \t\t\t\t\t"username": "username"
            \t\t\t\t}
            \t\t\t},
            \t\t\t"changed": true,
            \t\t\t"modifications": [{
            \t\t\t\t"revision": "1",
            \t\t\t\t"modified-time": "%s",
            \t\t\t\t"data": {}
            \t\t\t}]
            \t\t}, {
            \t\t\t"material": {
            \t\t\t\t"perforce-configuration": {
            \t\t\t\t\t"view": "view",
            \t\t\t\t\t"use-tickets": false,
            \t\t\t\t\t"url": "127.0.0.1:1666",
            \t\t\t\t\t"username": "username"
            \t\t\t\t},
            \t\t\t\t"type": "perforce"
            \t\t\t},
            \t\t\t"changed": true,
            \t\t\t"modifications": [{
            \t\t\t\t"revision": "1",
            \t\t\t\t"modified-time": "%s",
            \t\t\t\t"data": {}
            \t\t\t}]
            \t\t}, {
            \t\t\t"material": {
            \t\t\t\t"pipeline-configuration": {
            \t\t\t\t\t"pipeline-name": "pipeline-name",
            \t\t\t\t\t"stage-name": "stage-name"
            \t\t\t\t},
            \t\t\t\t"type": "pipeline"
            \t\t\t},
            \t\t\t"changed": true,
            \t\t\t"modifications": [{
            \t\t\t\t"revision": "pipeline-name/1/stage-name/1",
            \t\t\t\t"modified-time": "%s",
            \t\t\t\t"data": {}
            \t\t\t}]
            \t\t}, {
            \t\t\t"material": {
            \t\t\t\t"plugin-id": "pluginid",
            \t\t\t\t"package-configuration": {
            \t\t\t\t\t"k3": "package-v1"
            \t\t\t\t},
            \t\t\t\t"repository-configuration": {
            \t\t\t\t\t"k1": "repo-v1"
            \t\t\t\t},
            \t\t\t\t"type": "package"
            \t\t\t},
            \t\t\t"changed": true,
            \t\t\t"modifications": [{
            \t\t\t\t"revision": "1",
            \t\t\t\t"modified-time": "%s",
            \t\t\t\t"data": {}
            \t\t\t}]
            \t\t}, {
            \t\t\t"material": {
            \t\t\t\t"plugin-id": "pluginid",
            \t\t\t\t"scm-configuration": {
            \t\t\t\t\t"k1": "v1"
            \t\t\t\t},
            \t\t\t\t"type": "scm"
            \t\t\t},
            \t\t\t"changed": true,
            \t\t\t"modifications": [{
            \t\t\t\t"revision": "1",
            \t\t\t\t"modified-time": "%s",
            \t\t\t\t"data": {}
            \t\t\t}]
            \t\t}],
            \t\t"stage": {
            \t\t\t"name": "stage-name",
            \t\t\t"counter": "1",
            \t\t\t"approval-type": "success",
            \t\t\t"approved-by": "changes",
            \t\t\t"state": "Passed",
            \t\t\t"result": "Passed",
            \t\t\t"create-time": "2011-07-13T19:43:37.100Z",
            \t\t\t"last-transition-time": "2011-07-13T19:43:37.100Z",
            \t\t\t"jobs": [{
            \t\t\t\t"name": "job-name",
            \t\t\t\t"schedule-time": "2011-07-13T19:43:37.100Z",
            \t\t\t\t"complete-time": "2011-07-13T19:43:37.100Z",
            \t\t\t\t"state": "Completed",
            \t\t\t\t"result": "Passed",
            \t\t\t\t"agent-uuid": "uuid"
            \t\t\t}]
            \t\t}
            \t}
            }"""
            .formatted(
                gitModifiedTime,
                hgModifiedTime,
                svnModifiedTime,
                tfsModifiedTime,
                p4ModifiedTime,
                dependencyModifiedTime,
                packageMaterialModifiedTime,
                pluggableScmModifiedTime
            );

        String request = messageHandler.requestMessageForNotify(new StageNotificationData(pipeline.getFirstStage(), pipeline.getBuildCause(), "pipeline-group"));
        assertThatJson(expected).isEqualTo(request);
    }

    @Test
    public void shouldThrowExceptionIfAnUnhandledObjectIsPassed() {
        assertThatThrownBy(() -> messageHandler.requestMessageForNotify(new Pipeline()))
            .hasMessageContaining(String.format("Converter for %s not supported", Pipeline.class.getCanonicalName()));
    }

    @Test
    public void shouldNotHandleAgentNotificationRequest() {
        assertThatThrownBy(() -> messageHandler.requestMessageForNotify(new AgentNotificationData(null, null, false,
            null, null, null, null, null, null, null)))
            .isInstanceOf(UnsupportedOperationException.class)
            .hasMessageContaining(String.format("Converter for %s not supported", AgentNotificationData.class.getCanonicalName()));
    }

    private void assertSuccessResult(Result result, List<String> messages) {
        assertThat(result.isSuccessful()).isEqualTo(true);
        assertThat(result.getMessages()).isEqualTo(messages);
    }

    private void assertFailureResult(Result result, List<String> messages) {
        assertThat(result.isSuccessful()).isEqualTo(false);
        assertThat(result.getMessages()).isEqualTo(messages);
    }

    private String errorMessageForNotificationsInterestedIn(String message) {
        try {
            messageHandler.responseMessageForNotificationsInterestedIn(message);
            fail("should have thrown exception");
        } catch (Exception e) {
            return e.getMessage();
        }
        return null;
    }

    private String errorMessageForNotifyResult(String message) {
        try {
            messageHandler.toResult(message);
            fail("should have thrown exception");
        } catch (Exception e) {
            return e.getMessage();
        }
        return null;
    }

    private Date getFixedDate() throws Exception {
        return new SimpleDateFormat(DATE_PATTERN).parse("2011-07-13T19:43:37.100Z");
    }

    private Pipeline createPipeline() throws Exception {
        Pipeline pipeline = PipelineMother.pipelineWithAllTypesOfMaterials("pipeline-name", "stage-name", "job-name", "1");
        List<MaterialRevision> materialRevisions = pipeline.getMaterialRevisions().getRevisions();
        PackageDefinition packageDefinition = ((PackageMaterial) materialRevisions.get(6).getMaterial()).getPackageDefinition();
        packageDefinition.getRepository().getConfiguration().get(1).handleSecureValueConfiguration(true);
        packageDefinition.getConfiguration().addNewConfigurationWithValue("k4", "package-v2", false);
        packageDefinition.getConfiguration().get(1).handleSecureValueConfiguration(true);
        SCM scm = ((PluggableSCMMaterial) materialRevisions.get(7).getMaterial()).getScmConfig();
        scm.getConfiguration().get(1).handleSecureValueConfiguration(true);
        Stage stage = pipeline.getFirstStage();
        stage.setId(1L);
        stage.setPipelineId(1L);
        stage.setCreatedTime(new Timestamp(getFixedDate().getTime()));
        stage.setLastTransitionedTime(new Timestamp(getFixedDate().getTime()));
        JobInstance job = stage.getJobInstances().get(0);
        job.setScheduledDate(getFixedDate());
        job.getTransition(JobState.Completed).setStateChangeTime(getFixedDate());
        return pipeline;
    }
}
