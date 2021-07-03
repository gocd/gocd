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
import net.javacrumbs.jsonunit.fluent.JsonFluentAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.*;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.fail;

public class JsonMessageHandler3_0_Test {
    private JsonMessageHandler3_0 messageHandler;
    private static final TimeZone UTC = TimeZone.getTimeZone("UTC");
    public static final String DATE_PATTERN_FOR_V3 = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";

    @BeforeEach
    public void setUp() throws Exception {
        messageHandler = new JsonMessageHandler3_0();
    }

    @Test
    public void shouldBuildNotificationsInterestedInFromResponseBody() {
        String responseBody = "{notifications=[\"pipeline-status\",\"stage-status\"]}";
        List<String> notificationsInterestedIn = messageHandler.responseMessageForNotificationsInterestedIn(responseBody);

        assertThat(notificationsInterestedIn, is(Arrays.asList("pipeline-status", "stage-status")));
    }

    @Test
    public void shouldValidateIncorrectJsonResponseForNotificationsInterestedIn() {
        assertThat(errorMessageForNotificationsInterestedIn("{\"notifications\":{}}"), is("Unable to de-serialize json response. 'notifications' should be of type list of string"));
        assertThat(errorMessageForNotificationsInterestedIn("{\"notifications\":[{},{}]}"), is("Unable to de-serialize json response. Notification 'name' should be of type string"));
    }

    @Test
    public void shouldBuildSuccessResultFromNotify() {
        String responseBody = "{\"status\":\"success\",messages=[\"message-one\",\"message-two\"]}";
        Result result = messageHandler.responseMessageForNotify(responseBody);

        assertSuccessResult(result, asList("message-one", "message-two"));
    }

    @Test
    public void shouldBuildFailureResultFromNotify() {
        String responseBody = "{\"status\":\"failure\",messages=[\"message-one\",\"message-two\"]}";
        Result result = messageHandler.responseMessageForNotify(responseBody);

        assertFailureResult(result, asList("message-one", "message-two"));
    }

    @Test
    public void shouldHandleNullMessagesForNotify() {
        assertSuccessResult(messageHandler.responseMessageForNotify("{\"status\":\"success\"}"), new ArrayList<>());
        assertFailureResult(messageHandler.responseMessageForNotify("{\"status\":\"failure\"}"), new ArrayList<>());
    }

    @Test
    public void shouldValidateIncorrectJsonForResult() {
        assertThat(errorMessageForNotifyResult(""), is("Unable to de-serialize json response. Empty response body"));
        assertThat(errorMessageForNotifyResult("[{\"result\":\"success\"}]"), is("Unable to de-serialize json response. Notify result should be returned as map, with key represented as string and messages represented as list"));
        assertThat(errorMessageForNotifyResult("{\"status\":true}"), is("Unable to de-serialize json response. Notify result 'status' should be of type string"));
        assertThat(errorMessageForNotifyResult("{\"result\":true}"), is("Unable to de-serialize json response. Notify result 'status' is a required field"));

        assertThat(errorMessageForNotifyResult("{\"status\":\"success\",\"messages\":{}}"), is("Unable to de-serialize json response. Notify result 'messages' should be of type list of string"));
        assertThat(errorMessageForNotifyResult("{\"status\":\"success\",\"messages\":[{},{}]}"), is("Unable to de-serialize json response. Notify result 'message' should be of type string"));
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
        String expected = "{\n" +
                " \"pipeline\": {\n" +
                "   \"name\": \"pipeline-name\",\n" +
                "   \"label\": \"LABEL-1\",\n" +
                "   \"counter\": \"1\",\n" +
                "   \"group\": \"pipeline-group\",\n" +
                "   \"build-cause\": [{\n" +
                "     \"material\": {\n" +
                "       \"git-configuration\": {\n" +
                "         \"shallow-clone\": false,\n" +
                "         \"branch\": \"branch\",\n" +
                "         \"url\": \"http://user:******@gitrepo.com\"\n" +
                "       },\n" +
                "       \"type\": \"git\"\n" +
                "     },\n" +
                "     \"changed\": true,\n" +
                "     \"modifications\": [{\n" +
                "       \"revision\": \"1\",\n" +
                "       \"modified-time\": \"" + gitModifiedTime + "\",\n" +
                "       \"data\": {}\n" +
                "     }]\n" +
                "   }, {\n" +
                "     \"material\": {\n" +
                "       \"type\": \"mercurial\",\n" +
                "       \"mercurial-configuration\": {\n" +
                "         \"url\": \"http://user:******@hgrepo.com\"\n" +
                "       }\n" +
                "     },\n" +
                "     \"changed\": true,\n" +
                "     \"modifications\": [{\n" +
                "       \"revision\": \"1\",\n" +
                "       \"modified-time\": \"" + hgModifiedTime + "\",\n" +
                "       \"data\": {}\n" +
                "     }]\n" +
                "   }, {\n" +
                "     \"material\": {\n" +
                "       \"svn-configuration\": {\n" +
                "         \"check-externals\": false,\n" +
                "         \"url\": \"http://user:******@svnrepo.com\",\n" +
                "         \"username\": \"username\"\n" +
                "       },\n" +
                "       \"type\": \"svn\"\n" +
                "     },\n" +
                "     \"changed\": true,\n" +
                "     \"modifications\": [{\n" +
                "       \"revision\": \"1\",\n" +
                "       \"modified-time\": \"" + svnModifiedTime + "\",\n" +
                "       \"data\": {}\n" +
                "     }]\n" +
                "   }, {\n" +
                "     \"material\": {\n" +
                "       \"type\": \"tfs\",\n" +
                "       \"tfs-configuration\": {\n" +
                "         \"domain\": \"domain\",\n" +
                "         \"project-path\": \"project-path\",\n" +
                "         \"url\": \"http://user:******@tfsrepo.com\",\n" +
                "         \"username\": \"username\"\n" +
                "       }\n" +
                "     },\n" +
                "     \"changed\": true,\n" +
                "     \"modifications\": [{\n" +
                "       \"revision\": \"1\",\n" +
                "       \"modified-time\": \"" + tfsModifiedTime + "\",\n" +
                "       \"data\": {}\n" +
                "     }]\n" +
                "   }, {\n" +
                "     \"material\": {\n" +
                "       \"perforce-configuration\": {\n" +
                "         \"view\": \"view\",\n" +
                "         \"use-tickets\": false,\n" +
                "         \"url\": \"127.0.0.1:1666\",\n" +
                "         \"username\": \"username\"\n" +
                "       },\n" +
                "       \"type\": \"perforce\"\n" +
                "     },\n" +
                "     \"changed\": true,\n" +
                "     \"modifications\": [{\n" +
                "       \"revision\": \"1\",\n" +
                "       \"modified-time\": \"" + p4ModifiedTime + "\",\n" +
                "       \"data\": {}\n" +
                "     }]\n" +
                "   }, {\n" +
                "     \"material\": {\n" +
                "       \"pipeline-configuration\": {\n" +
                "         \"pipeline-name\": \"pipeline-name\",\n" +
                "         \"stage-name\": \"stage-name\"\n" +
                "       },\n" +
                "       \"type\": \"pipeline\"\n" +
                "     },\n" +
                "     \"changed\": true,\n" +
                "     \"modifications\": [{\n" +
                "       \"revision\": \"pipeline-name/1/stage-name/1\",\n" +
                "       \"modified-time\": \"" + dependencyModifiedTime + "\",\n" +
                "       \"data\": {}\n" +
                "     }]\n" +
                "   }, {\n" +
                "     \"material\": {\n" +
                "       \"plugin-id\": \"pluginid\",\n" +
                "       \"package-configuration\": {\n" +
                "         \"k3\": \"package-v1\"\n" +
                "       },\n" +
                "       \"repository-configuration\": {\n" +
                "         \"k1\": \"repo-v1\"\n" +
                "       },\n" +
                "       \"type\": \"package\"\n" +
                "     },\n" +
                "     \"changed\": true,\n" +
                "     \"modifications\": [{\n" +
                "       \"revision\": \"1\",\n" +
                "       \"modified-time\": \"" + packageMaterialModifiedTime + "\",\n" +
                "       \"data\": {}\n" +
                "     }]\n" +
                "   }, {\n" +
                "     \"material\": {\n" +
                "       \"plugin-id\": \"pluginid\",\n" +
                "       \"scm-configuration\": {\n" +
                "         \"k1\": \"v1\"\n" +
                "       },\n" +
                "       \"type\": \"scm\"\n" +
                "     },\n" +
                "     \"changed\": true,\n" +
                "     \"modifications\": [{\n" +
                "       \"revision\": \"1\",\n" +
                "       \"modified-time\": \"" + pluggableScmModifiedTime + "\",\n" +
                "       \"data\": {}\n" +
                "     }]\n" +
                "   }],\n" +
                "   \"stage\": {\n" +
                "     \"name\": \"stage-name\",\n" +
                "     \"counter\": \"1\",\n" +
                "     \"approval-type\": \"success\",\n" +
                "     \"approved-by\": \"changes\",\n" +
                "     \"state\": \"Passed\",\n" +
                "     \"result\": \"Passed\",\n" +
                "     \"create-time\": \"2011-07-13T14:13:37.100+0000\",\n" +
                "     \"last-transition-time\": \"2011-07-13T14:13:37.100+0000\",\n" +
                "     \"jobs\": [{\n" +
                "       \"name\": \"job-name\",\n" +
                "       \"schedule-time\": \"2011-07-13T14:13:37.100+0000\",\n" +
                "       \"assign-time\": \"2011-07-13T14:13:37.100+0000\",\n" +
                "       \"complete-time\": \"2011-07-13T14:13:37.100+0000\",\n" +
                "       \"state\": \"Completed\",\n" +
                "       \"result\": \"Passed\",\n" +
                "       \"agent-uuid\": \"uuid\"\n" +
                "     }]\n" +
                "   }\n" +
                " }\n" +
                "}";

        String request = messageHandler.requestMessageForNotify(new StageNotificationData(pipeline.getFirstStage(), pipeline.getBuildCause(), "pipeline-group"));
        JsonFluentAssert.assertThatJson(expected).isEqualTo(request);
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

        String expected = "{\n" +
                "    \"agent_config_state\": \"enabled\",\n" +
                "    \"agent_state\": \"building\",\n" +
                "    \"build_state\": \"building\",\n" +
                "    \"is_elastic\": true,\n" +
                "    \"free_space\": \"100\",\n" +
                "    \"host_name\": \"agent_hostname\",\n" +
                "    \"ip_address\": \"127.0.0.1\",\n" +
                "    \"operating_system\": \"rh\",\n" +
                "    \"uuid\": \"agent_uuid\",\n" +
                "    \"transition_time\": \"" + time + "\"\n" +
                "}\n";

        String message = messageHandler.requestMessageForNotify(agentNotificationData);

        JsonFluentAssert.assertThatJson(expected).isEqualTo(message);
    }

    private void assertSuccessResult(Result result, List<String> messages) {
        assertThat(result.isSuccessful(), is(true));
        assertThat(result.getMessages(), is(messages));
    }

    private void assertFailureResult(Result result, List<String> messages) {
        assertThat(result.isSuccessful(), is(false));
        assertThat(result.getMessages(), is(messages));
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
