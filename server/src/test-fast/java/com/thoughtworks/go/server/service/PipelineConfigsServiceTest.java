/*
 * Copyright 2020 ThoughtWorks, Inc.
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
package com.thoughtworks.go.server.service;

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.commands.EntityConfigUpdateCommand;
import com.thoughtworks.go.config.exceptions.EntityType;
import com.thoughtworks.go.config.exceptions.GoConfigInvalidException;
import com.thoughtworks.go.config.exceptions.RecordNotFoundException;
import com.thoughtworks.go.config.registry.ConfigElementImplementationRegistry;
import com.thoughtworks.go.config.update.CreatePipelineConfigsCommand;
import com.thoughtworks.go.config.update.DeletePipelineConfigsCommand;
import com.thoughtworks.go.config.update.UpdatePipelineConfigsCommand;
import com.thoughtworks.go.config.validation.GoConfigValidity;
import com.thoughtworks.go.domain.PipelineGroups;
import com.thoughtworks.go.helper.GoConfigMother;
import com.thoughtworks.go.i18n.LocalizedMessage;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.responses.GoConfigOperationalResponse;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import com.thoughtworks.go.util.ConfigElementImplementationRegistryMother;
import com.thoughtworks.go.util.ReflectionUtil;
import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.*;

public class PipelineConfigsServiceTest {

    private PipelineConfigsService service;
    private GoConfigService goConfigService;
    private ConfigCache configCache;
    private ConfigElementImplementationRegistry registry;
    private SecurityService securityService;
    private EntityHashingService entityHashingService;
    private Username validUser;
    private HttpLocalizedOperationResult result;
    private CruiseConfig cruiseConfig;

    @Before
    public void setUp() {
        goConfigService = mock(GoConfigService.class);
        securityService = mock(SecurityService.class);
        entityHashingService = mock(EntityHashingService.class);
        configCache = new ConfigCache();
        registry = ConfigElementImplementationRegistryMother.withNoPlugins();
        validUser = new Username(new CaseInsensitiveString("validUser"));
        service = new PipelineConfigsService(configCache, registry, goConfigService, securityService, entityHashingService);
        result = new HttpLocalizedOperationResult();

        cruiseConfig = new BasicCruiseConfig();
        ReflectionUtil.setField(cruiseConfig, "md5", "md5");
    }

    @Test
    public void shouldReturnXmlForGivenGroup_onGetXml() {
        CruiseConfig cruiseConfig = new BasicCruiseConfig();
        String groupName = "group_name";
        new GoConfigMother().addPipelineWithGroup(cruiseConfig, groupName, "pipeline_name", "stage_name", "job_name");
        when(goConfigService.getConfigForEditing()).thenReturn(cruiseConfig);
        when(securityService.isUserAdminOfGroup(validUser.getUsername(), groupName)).thenReturn(true);
        String actualXml = service.getXml(groupName, validUser, result);
        String expectedXml = "<pipelines group=\"group_name\">\n  <pipeline name=\"pipeline_name\">\n    <materials>\n      <svn url=\"file:///tmp/foo\" />\n    </materials>\n    <stage name=\"stage_name\">\n      <jobs>\n        <job name=\"job_name\" />\n      </jobs>\n    </stage>\n  </pipeline>\n</pipelines>";
        assertThat(actualXml, is(expectedXml));
        assertThat(result.isSuccessful(), is(true));
        verify(goConfigService, times(1)).getConfigForEditing();
    }

    @Test
    public void shouldThrowExceptionWhenTheGroupIsNotFound_onGetXml() {
        String groupName = "non-existent-group_name";
        when(securityService.isUserAdminOfGroup(validUser.getUsername(), groupName)).thenThrow(new RecordNotFoundException(EntityType.PipelineGroup, groupName));

        service.getXml(groupName, validUser, result);

        assertThat(result.httpCode(), is(404));
        assertThat(result.isSuccessful(), is(false));
        assertThat(result.message(), is(EntityType.PipelineGroup.notFoundMessage(groupName)));
        verify(securityService, times(1)).isUserAdminOfGroup(validUser.getUsername(), groupName);
        verify(goConfigService, never()).getConfigForEditing();

    }

    @Test
    public void shouldReturnUnauthorizedResultWhenUserIsNotAuthorizedToViewGroup_onGetXml() {
        String groupName = "some-secret-group";
        Username invalidUser = new Username(new CaseInsensitiveString("invalidUser"));
        when(securityService.isUserAdminOfGroup(invalidUser.getUsername(), groupName)).thenReturn(false);

        String actual = service.getXml(groupName, invalidUser, result);

        assertThat(actual, is(nullValue()));
        assertThat(result.httpCode(), is(403));
        assertThat(result.isSuccessful(), is(false));
        assertThat(result.message(), is(EntityType.PipelineGroup.forbiddenToEdit(groupName, invalidUser.getUsername())));
        verify(goConfigService, never()).getConfigForEditing();
        verify(securityService, times(1)).isUserAdminOfGroup(invalidUser.getUsername(), groupName);
    }

    @Test
    public void shouldUpdateXmlAndReturnPipelineConfigsIfUserHasEditPermissionsForTheGroupAndUpdateWasSuccessful() throws Exception {
        final String groupName = "group_name";
        when(securityService.isUserAdminOfGroup(validUser.getUsername(), groupName)).thenReturn(true);
        String md5 = "md5";
        when(goConfigService.configFileMd5()).thenReturn(md5);

        GoConfigService.XmlPartialSaver groupSaver = mock(GoConfigService.XmlPartialSaver.class);
        when(goConfigService.groupSaver(groupName)).thenReturn(groupSaver);
        String updatedPartial = groupXml();
        when(groupSaver.saveXml(updatedPartial, md5)).thenReturn(GoConfigValidity.valid());

        GoConfigOperationalResponse<PipelineConfigs> actual = service.updateXml(groupName, updatedPartial, md5, validUser, result);
        PipelineConfigs configs = actual.getConfigElement();
        GoConfigValidity validity = actual.getValidity();

        assertThat(configs, is(not(nullValue())));
        assertThat(configs.getGroup(), is("renamed_group_name"));
        assertThat(result.httpCode(), is(200));
        assertThat(result.isSuccessful(), is(true));
        assertThat(validity.isValid(), is(true));
        verify(groupSaver).saveXml(updatedPartial, md5);
    }

    @Test
    public void shouldReturnUnsuccessfulResultWhenXmlIsInvalid_onUpdateXml() throws Exception {
        String errorMessage = "Can not parse xml.";
        final String groupName = "group_name";
        when(securityService.isUserAdminOfGroup(validUser.getUsername(), groupName)).thenReturn(true);
        String md5 = "md5";
        when(goConfigService.configFileMd5()).thenReturn(md5);
        GoConfigService.XmlPartialSaver groupSaver = mock(GoConfigService.XmlPartialSaver.class);
        when(goConfigService.groupSaver(groupName)).thenReturn(groupSaver);
        String updatedPartial = "foobar";
        when(groupSaver.saveXml(updatedPartial, md5)).thenReturn(GoConfigValidity.invalid(errorMessage));

        GoConfigOperationalResponse<PipelineConfigs> actual = service.updateXml(groupName, updatedPartial, md5, validUser, result);
        PipelineConfigs configs = actual.getConfigElement();
        GoConfigValidity validity = actual.getValidity();

        assertThat(configs, is(nullValue()));
        assertThat(result.httpCode(), is(500));
        assertThat(result.isSuccessful(), is(false));
        assertThat(result.message(), is("Failed to update group 'group_name'. Can not parse xml."));
        assertThat(validity.isValid(), is(false));
        verify(securityService, times(1)).isUserAdminOfGroup(validUser.getUsername(), groupName);
    }

    @Test
    public void shouldReturnUnsuccessfulResultWhenTheGroupIsNotFound_onUpdateXml() throws Exception {
        String groupName = "non-existent-group_name";
        when(securityService.isUserAdminOfGroup(validUser.getUsername(), groupName)).thenThrow(new RecordNotFoundException(EntityType.PipelineGroup, groupName));
        when(goConfigService.configFileMd5()).thenReturn("md5");

        GoConfigOperationalResponse<PipelineConfigs> actual = service.updateXml(groupName, "", "md5", validUser, result);
        PipelineConfigs configs = actual.getConfigElement();
        GoConfigValidity validity = actual.getValidity();

        assertThat(configs, is(nullValue()));
        assertThat(result.httpCode(), is(404));
        assertThat(result.isSuccessful(), is(false));
        assertThat(result.message(), is(EntityType.PipelineGroup.notFoundMessage(groupName)));
        assertThat(validity.isValid(), is(true));
        verify(securityService, times(1)).isUserAdminOfGroup(validUser.getUsername(), groupName);
    }

    @Test
    public void shouldReturnUnauthorizedResultWhenUserIsNotAuthorizedToViewGroup_onUpdateXml() throws Exception {
        String groupName = "some-secret-group";
        Username invalidUser = new Username(new CaseInsensitiveString("invalidUser"));
        when(securityService.isUserAdminOfGroup(invalidUser.getUsername(), groupName)).thenReturn(false);
        when(goConfigService.configFileMd5()).thenReturn("md5");

        GoConfigOperationalResponse<PipelineConfigs> actual = service.updateXml(groupName, "", "md5", invalidUser, result);
        PipelineConfigs configElement = actual.getConfigElement();
        GoConfigValidity validity = actual.getValidity();

        assertThat(configElement, is(nullValue()));
        assertThat(result.httpCode(), is(403));
        assertThat(result.isSuccessful(), is(false));
        assertThat(result.message(), is(EntityType.PipelineGroup.forbiddenToEdit(groupName, invalidUser.getUsername())));
        assertThat(validity.isValid(), is(true));
        verify(securityService, times(1)).isUserAdminOfGroup(invalidUser.getUsername(), groupName);
    }

    @Test
    public void shouldSetSuccessMessageOnSuccessfulUpdate() throws Exception {
        String groupName = "renamed_group_name";
        String md5 = "md5";
        GoConfigService.XmlPartialSaver groupSaver = mock(GoConfigService.XmlPartialSaver.class);
        when(securityService.isUserAdminOfGroup(validUser.getUsername(), groupName)).thenReturn(true);
        when(goConfigService.configFileMd5()).thenReturn(md5);
        when(goConfigService.groupSaver(groupName)).thenReturn(groupSaver);
        when(groupSaver.saveXml(groupXml(), md5)).thenReturn(GoConfigValidity.valid(ConfigSaveState.UPDATED));

        GoConfigOperationalResponse<PipelineConfigs> actual = service.updateXml(groupName, groupXml(), md5, validUser, result);
        GoConfigValidity validity = actual.getValidity();

        assertThat(result.message(), is("Saved configuration successfully."));
        assertThat(validity.isValid(), is(true));
    }

    @Test
    public void shouldSetSuccessMessageOnSuccessfulMerge() throws Exception {
        String groupName = "renamed_group_name";
        GoConfigService.XmlPartialSaver groupSaver = mock(GoConfigService.XmlPartialSaver.class);
        when(securityService.isUserAdminOfGroup(validUser.getUsername(), groupName)).thenReturn(true);
        when(goConfigService.configFileMd5()).thenReturn("old-md5");
        when(goConfigService.groupSaver(groupName)).thenReturn(groupSaver);
        when(groupSaver.saveXml(groupXml(), "md5")).thenReturn(GoConfigValidity.valid(ConfigSaveState.MERGED));

        GoConfigOperationalResponse<PipelineConfigs> actual = service.updateXml(groupName, groupXml(), "md5", validUser, result);
        GoConfigValidity validity = actual.getValidity();

        assertThat(result.message(), is(LocalizedMessage.composite("Saved configuration successfully.", "The configuration was modified by someone else, but your changes were merged successfully.")));
        assertThat(validity.isValid(), is(true));
    }

    @Test
    public void shouldThrowUpWithDifferentMessageForMergeExceptions() throws Exception {
        String groupName = "renamed_group_name";
        GoConfigService.XmlPartialSaver groupSaver = mock(GoConfigService.XmlPartialSaver.class);
        when(securityService.isUserAdminOfGroup(validUser.getUsername(), groupName)).thenReturn(true);
        when(goConfigService.configFileMd5()).thenReturn("old-md5");
        when(goConfigService.groupSaver(groupName)).thenReturn(groupSaver);
        when(groupSaver.saveXml(null, "md5")).thenReturn(GoConfigValidity.mergeConflict("some error"));

        GoConfigOperationalResponse<PipelineConfigs> actual = service.updateXml(groupName, null, "md5", validUser, result);
        PipelineConfigs configElement = actual.getConfigElement();
        GoConfigValidity validity = actual.getValidity();

        assertThat(configElement, is(nullValue()));
        assertThat(result.isSuccessful(), is(false));

        GoConfigValidity.InvalidGoConfig invalidGoConfig = (GoConfigValidity.InvalidGoConfig) validity;
        assertThat(invalidGoConfig.isValid(), is(false));
        assertThat(invalidGoConfig.isMergeConflict(), is(true));
        assertThat(result.message(), is("Someone has modified the configuration and your changes are in conflict. Please review, amend and retry."));
    }

    @Test
    public void shouldThrowUpWithDifferentMessageForPostMergeValidationExceptions() throws Exception {
        String groupName = "renamed_group_name";
        GoConfigService.XmlPartialSaver groupSaver = mock(GoConfigService.XmlPartialSaver.class);
        when(securityService.isUserAdminOfGroup(validUser.getUsername(), groupName)).thenReturn(true);
        when(goConfigService.configFileMd5()).thenReturn("old-md5");
        when(goConfigService.groupSaver(groupName)).thenReturn(groupSaver);
        when(groupSaver.saveXml(null, "md5")).thenReturn(GoConfigValidity.mergePostValidationError("some error"));

        GoConfigOperationalResponse<PipelineConfigs> actual = service.updateXml(groupName, null, "md5", validUser, result);
        PipelineConfigs configElement = actual.getConfigElement();
        GoConfigValidity validity = actual.getValidity();

        assertThat(configElement, is(nullValue()));
        assertThat(result.isSuccessful(), is(false));
        assertThat(validity.isValid(), is(false));

        GoConfigValidity.InvalidGoConfig invalidGoConfig = (GoConfigValidity.InvalidGoConfig) validity;
        assertThat(invalidGoConfig.isPostValidationError(), is(true));
        assertThat(result.message(), is("Someone has modified the configuration and your changes are in conflict. Please review, amend and retry."));
    }

    @Test
    public void shouldGetPipelineGroupsForUser() {
        PipelineConfig pipelineInGroup1 = new PipelineConfig();
        PipelineConfigs group1 = new BasicPipelineConfigs(pipelineInGroup1);
        group1.setGroup("group1");
        PipelineConfig pipelineInGroup2 = new PipelineConfig();
        PipelineConfigs group2 = new BasicPipelineConfigs(pipelineInGroup2);
        group2.setGroup("group2");
        when(goConfigService.groups()).thenReturn(new PipelineGroups(group1, group2));
        String user = "looser";
        when(securityService.hasViewPermissionForGroup(user, "group1")).thenReturn(true);
        when(securityService.hasViewPermissionForGroup(user, "group2")).thenReturn(false);

        List<PipelineConfigs> gotPipelineGroups = service.getGroupsForUser(user);

        verify(goConfigService, never()).getAllPipelinesForEditInGroup("group1");
        assertThat(gotPipelineGroups, is(Arrays.asList(group1)));
    }

    @Test
    public void shouldInvokeUpdateConfigCommand_updateGroupAuthorization() {
        Authorization authorization = new Authorization(new OperationConfig(new AdminUser(validUser.getUsername())));
        PipelineConfigs pipelineConfigs = new BasicPipelineConfigs("group", authorization);

        when(entityHashingService.hashForEntity(pipelineConfigs)).thenReturn("digest");
        service.updateGroup(validUser, pipelineConfigs, pipelineConfigs, result);

        ArgumentCaptor<EntityConfigUpdateCommand> commandCaptor = ArgumentCaptor.forClass(EntityConfigUpdateCommand.class);
        verify(goConfigService).updateConfig(commandCaptor.capture(), eq(validUser));
        UpdatePipelineConfigsCommand command = (UpdatePipelineConfigsCommand) commandCaptor.getValue();

        assertThat(ReflectionUtil.getField(command, "oldPipelineGroup"), is(pipelineConfigs));
        assertThat(ReflectionUtil.getField(command, "newPipelineGroup"), is(pipelineConfigs));
        assertThat(ReflectionUtil.getField(command, "digest"), is("digest"));
        assertThat(ReflectionUtil.getField(command, "result"), is(result));
        assertThat(ReflectionUtil.getField(command, "currentUser"), is(validUser));
        assertThat(ReflectionUtil.getField(command, "entityHashingService"), is(entityHashingService));
        assertThat(ReflectionUtil.getField(command, "securityService"), is(securityService));
    }

    @Test
    public void shouldReturnUpdatedPipelineConfigs_whenSuccessful_updateGroupAuthorization() {
        Authorization authorization = new Authorization(new OperationConfig(new AdminUser(validUser.getUsername())));
        PipelineConfigs pipelineConfigs = new BasicPipelineConfigs("group", authorization);
        doAnswer(invocation -> {
            UpdatePipelineConfigsCommand command = invocation.getArgument(0);
            ReflectionUtil.setField(command, "preprocessedPipelineConfigs", pipelineConfigs);
            return null;
        }).when(goConfigService).updateConfig(any(), eq(validUser));

        PipelineConfigs updatedPipelineConfigs = service.updateGroup(validUser, pipelineConfigs, pipelineConfigs, result);

        assertThat(updatedPipelineConfigs.getAuthorization(), is(authorization));
        assertThat(updatedPipelineConfigs.getGroup(), is("group"));
    }

    @Test
    public void shouldReturnUnprocessableEntity_whenConfigInvalid_updateGroupAuthorization() {
        Authorization authorization = new Authorization(new OperationConfig(new AdminUser(validUser.getUsername())));
        PipelineConfigs pipelineConfigs = new BasicPipelineConfigs("group", authorization);
        doThrow(new GoConfigInvalidException(null, "error message")).when(goConfigService).updateConfig(any(), eq(validUser));

        service.updateGroup(validUser, pipelineConfigs, pipelineConfigs, result);

        assertThat(result.httpCode(), is(HttpStatus.SC_UNPROCESSABLE_ENTITY));
        assertThat(result.message(), is("Validations failed for pipelines 'group'. Error(s): [error message]. Please correct and resubmit."));
    }

    @Test
    public void shouldReturnInternalServerError_whenExceptionThrown_updateGroupAuthorization() {
        Authorization authorization = new Authorization(new OperationConfig(new AdminUser(validUser.getUsername())));
        PipelineConfigs pipelineConfigs = new BasicPipelineConfigs("group", authorization);
        doThrow(new RuntimeException("server error")).when(goConfigService).updateConfig(any(), eq(validUser));

        service.updateGroup(validUser, pipelineConfigs, pipelineConfigs, result);

        assertThat(result.httpCode(), is(HttpStatus.SC_INTERNAL_SERVER_ERROR));
        assertThat(result.message(), is("Save failed. server error"));
    }

    @Test
    public void shouldInvokeDeleteConfigCommand_deleteGroup() {
        Authorization authorization = new Authorization(new OperationConfig(new AdminUser(validUser.getUsername())));
        PipelineConfigs pipelineConfigs = new BasicPipelineConfigs("group", authorization);

        service.deleteGroup(validUser, pipelineConfigs, result);

        ArgumentCaptor<EntityConfigUpdateCommand> commandCaptor = ArgumentCaptor.forClass(EntityConfigUpdateCommand.class);
        verify(goConfigService).updateConfig(commandCaptor.capture(), eq(validUser));
        DeletePipelineConfigsCommand command = (DeletePipelineConfigsCommand) commandCaptor.getValue();

        assertThat(ReflectionUtil.getField(command, "group"), is(pipelineConfigs));
        assertThat(ReflectionUtil.getField(command, "result"), is(result));
        assertThat(ReflectionUtil.getField(command, "currentUser"), is(validUser));
        assertThat(ReflectionUtil.getField(command, "securityService"), is(securityService));
    }

    @Test
    public void shouldReturnUnprocessableEntity_whenConfigInvalid_deleteGroup() {
        Authorization authorization = new Authorization(new OperationConfig(new AdminUser(validUser.getUsername())));
        PipelineConfigs pipelineConfigs = new BasicPipelineConfigs("group", authorization);
        doThrow(new GoConfigInvalidException(null, "error message")).when(goConfigService).updateConfig(any(), eq(validUser));

        service.deleteGroup(validUser, pipelineConfigs, result);

        assertThat(result.httpCode(), is(HttpStatus.SC_UNPROCESSABLE_ENTITY));
        assertThat(result.message(), is("Validations failed for pipelines 'group'. Error(s): [error message]. Please correct and resubmit."));
    }

    @Test
    public void shouldReturnInternalServerError_whenExceptionThrown_deleteGroup() {
        Authorization authorization = new Authorization(new OperationConfig(new AdminUser(validUser.getUsername())));
        PipelineConfigs pipelineConfigs = new BasicPipelineConfigs("group", authorization);
        doThrow(new RuntimeException("server error")).when(goConfigService).updateConfig(any(), eq(validUser));

        service.deleteGroup(validUser, pipelineConfigs, result);

        assertThat(result.httpCode(), is(HttpStatus.SC_INTERNAL_SERVER_ERROR));
        assertThat(result.message(), is("Save failed. server error"));
    }

    @Test
    public void shouldInvokeCreateConfigCommand_createGroup() {
        Authorization authorization = new Authorization(new OperationConfig(new AdminUser(validUser.getUsername())));
        PipelineConfigs pipelineConfigs = new BasicPipelineConfigs("group", authorization);

        service.createGroup(validUser, pipelineConfigs, result);

        ArgumentCaptor<EntityConfigUpdateCommand> commandCaptor = ArgumentCaptor.forClass(EntityConfigUpdateCommand.class);
        verify(goConfigService).updateConfig(commandCaptor.capture(), eq(validUser));
        CreatePipelineConfigsCommand command = (CreatePipelineConfigsCommand) commandCaptor.getValue();

        assertThat(ReflectionUtil.getField(command, "pipelineConfigs"), is(pipelineConfigs));
        assertThat(ReflectionUtil.getField(command, "result"), is(result));
        assertThat(ReflectionUtil.getField(command, "currentUser"), is(validUser));
        assertThat(ReflectionUtil.getField(command, "securityService"), is(securityService));
    }

    @Test
    public void shouldReturnUnprocessableEntity_whenConfigInvalid_createGroup() {
        Authorization authorization = new Authorization(new OperationConfig(new AdminUser(validUser.getUsername())));
        PipelineConfigs pipelineConfigs = new BasicPipelineConfigs("group", authorization);
        doThrow(new GoConfigInvalidException(null, "error message")).when(goConfigService).updateConfig(any(), eq(validUser));

        service.createGroup(validUser, pipelineConfigs, result);

        assertThat(result.httpCode(), is(HttpStatus.SC_UNPROCESSABLE_ENTITY));
        assertThat(result.message(), is("Validations failed for pipelines 'group'. Error(s): [error message]. Please correct and resubmit."));
    }

    @Test
    public void shouldReturnInternalServerError_whenExceptionThrown_createGroup() {
        Authorization authorization = new Authorization(new OperationConfig(new AdminUser(validUser.getUsername())));
        PipelineConfigs pipelineConfigs = new BasicPipelineConfigs("group", authorization);
        doThrow(new RuntimeException("server error")).when(goConfigService).updateConfig(any(), eq(validUser));

        service.createGroup(validUser, pipelineConfigs, result);

        assertThat(result.httpCode(), is(HttpStatus.SC_INTERNAL_SERVER_ERROR));
        assertThat(result.message(), is("Save failed. server error"));
    }

    private String groupXml() {
        return "<pipelines group=\"renamed_group_name\">\n"
                + "  <pipeline name=\"new_name\">\n"
                + "    <materials>\n"
                + "      <svn url=\"file:///tmp/foo\" />\n"
                + "    </materials>\n"
                + "    <stage name=\"stage_name\">\n"
                + "      <jobs>\n"
                + "        <job name=\"job_name\" />\n"
                + "      </jobs>\n"
                + "    </stage>\n"
                + "  </pipeline>\n"
                + "</pipelines>";
    }

}
