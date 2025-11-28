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
package com.thoughtworks.go.plugin.access.notification.v3;

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
import java.util.TimeZone;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.fail;

public class JsonMessageHandler3_0_Test {
    private JsonMessageHandler3_0 messageHandler;
    private static final TimeZone UTC = TimeZone.getTimeZone("UTC");
    public static final String DATE_PATTERN_FOR_V3 = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";

    @BeforeEach
    public void setUp() {
        messageHandler = new JsonMessageHandler3_0();
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
        String gitModifiedTime = dateToString(pipeline.getBuildCause().getMaterialRevisions().getMaterialRevision(0).getLatestModification().getModifiedTime());
        String hgModifiedTime = dateToString(pipeline.getBuildCause().getMaterialRevisions().getMaterialRevision(1).getLatestModification().getModifiedTime());
        String svnModifiedTime = dateToString(pipeline.getBuildCause().getMaterialRevisions().getMaterialRevision(2).getLatestModification().getModifiedTime());
        String tfsModifiedTime = dateToString(pipeline.getBuildCause().getMaterialRevisions().getMaterialRevision(3).getLatestModification().getModifiedTime());
        String p4ModifiedTime = dateToString(pipeline.getBuildCause().getMaterialRevisions().getMaterialRevision(4).getLatestModification().getModifiedTime());
        String dependencyModifiedTime = dateToString(pipeline.getBuildCause().getMaterialRevisions().getMaterialRevision(5).getLatestModification().getModifiedTime());
        String packageMaterialModifiedTime = dateToString(pipeline.getBuildCause().getMaterialRevisions().getMaterialRevision(6).getLatestModification().getModifiedTime());
        String pluggableScmModifiedTime = dateToString(pipeline.getBuildCause().getMaterialRevisions().getMaterialRevision(7).getLatestModification().getModifiedTime());
        String expected = """
            {
             "pipeline": {
               "name": "pipeline-name",
               "label": "LABEL-1",
               "counter": "1",
               "group": "pipeline-group",
               "build-cause": [{
                 "material": {
                   "git-configuration": {
                     "shallow-clone": false,
                     "branch": "branch",
                     "url": "http://user:******@gitrepo.com"
                   },
                   "type": "git"
                 },
                 "changed": true,
                 "modifications": [{
                   "revision": "1",
                   "modified-time": "%s",
                   "data": {}
                 }]
               }, {
                 "material": {
                   "type": "mercurial",
                   "mercurial-configuration": {
                     "url": "http://user:******@hgrepo.com"
                   }
                 },
                 "changed": true,
                 "modifications": [{
                   "revision": "1",
                   "modified-time": "%s",
                   "data": {}
                 }]
               }, {
                 "material": {
                   "svn-configuration": {
                     "check-externals": false,
                     "url": "http://user:******@svnrepo.com",
                     "username": "username"
                   },
                   "type": "svn"
                 },
                 "changed": true,
                 "modifications": [{
                   "revision": "1",
                   "modified-time": "%s",
                   "data": {}
                 }]
               }, {
                 "material": {
                   "type": "tfs",
                   "tfs-configuration": {
                     "domain": "domain",
                     "project-path": "project-path",
                     "url": "http://user:******@tfsrepo.com",
                     "username": "username"
                   }
                 },
                 "changed": true,
                 "modifications": [{
                   "revision": "1",
                   "modified-time": "%s",
                   "data": {}
                 }]
               }, {
                 "material": {
                   "perforce-configuration": {
                     "view": "view",
                     "use-tickets": false,
                     "url": "127.0.0.1:1666",
                     "username": "username"
                   },
                   "type": "perforce"
                 },
                 "changed": true,
                 "modifications": [{
                   "revision": "1",
                   "modified-time": "%s",
                   "data": {}
                 }]
               }, {
                 "material": {
                   "pipeline-configuration": {
                     "pipeline-name": "pipeline-name",
                     "stage-name": "stage-name"
                   },
                   "type": "pipeline"
                 },
                 "changed": true,
                 "modifications": [{
                   "revision": "pipeline-name/1/stage-name/1",
                   "modified-time": "%s",
                   "data": {}
                 }]
               }, {
                 "material": {
                   "plugin-id": "pluginid",
                   "package-configuration": {
                     "k3": "package-v1"
                   },
                   "repository-configuration": {
                     "k1": "repo-v1"
                   },
                   "type": "package"
                 },
                 "changed": true,
                 "modifications": [{
                   "revision": "1",
                   "modified-time": "%s",
                   "data": {}
                 }]
               }, {
                 "material": {
                   "plugin-id": "pluginid",
                   "scm-configuration": {
                     "k1": "v1"
                   },
                   "type": "scm"
                 },
                 "changed": true,
                 "modifications": [{
                   "revision": "1",
                   "modified-time": "%s",
                   "data": {}
                 }]
               }],
               "stage": {
                 "name": "stage-name",
                 "counter": "1",
                 "approval-type": "success",
                 "approved-by": "changes",
                 "state": "Passed",
                 "result": "Passed",
                 "create-time": "2011-07-13T14:13:37.100+0000",
                 "last-transition-time": "2011-07-13T14:13:37.100+0000",
                 "jobs": [{
                   "name": "job-name",
                   "schedule-time": "2011-07-13T14:13:37.100+0000",
                   "assign-time": "2011-07-13T14:13:37.100+0000",
                   "complete-time": "2011-07-13T14:13:37.100+0000",
                   "state": "Completed",
                   "result": "Passed",
                   "agent-uuid": "uuid"
                 }]
               }
             }
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
    public void shouldConstructAgentNotificationRequestMessage() {
        Date transition_time = new Date();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(DATE_PATTERN_FOR_V3);
        simpleDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        String time = simpleDateFormat.format(transition_time);

        AgentNotificationData agentNotificationData = new AgentNotificationData("agent_uuid", "agent_hostname",
            true, "127.0.0.1", "rh", "100",
            "enabled", "building", "building", transition_time);

        String expected = """
            {
                "agent_config_state": "enabled",
                "agent_state": "building",
                "build_state": "building",
                "is_elastic": true,
                "free_space": "100",
                "host_name": "agent_hostname",
                "ip_address": "127.0.0.1",
                "operating_system": "rh",
                "uuid": "agent_uuid",
                "transition_time": "%s"
            }
            """.formatted(time);

        String message = messageHandler.requestMessageForNotify(agentNotificationData);

        assertThatJson(expected).isEqualTo(message);
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
        return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ").parse("2011-07-13T19:43:37.100+0530");
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
        job.getTransition(JobState.Assigned).setStateChangeTime(getFixedDate());
        job.getTransition(JobState.Completed).setStateChangeTime(getFixedDate());
        return pipeline;
    }

    public static String dateToString(Date date) {
        if (date != null) {
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat(DATE_PATTERN_FOR_V3);
            simpleDateFormat.setTimeZone(UTC);
            return simpleDateFormat.format(date);
        }

        return "";
    }
}
