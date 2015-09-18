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

package com.thoughtworks.go.server.service;

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.exceptions.PipelineGroupNotFoundException;
import com.thoughtworks.go.config.registry.ConfigElementImplementationRegistry;
import com.thoughtworks.go.config.validation.GoConfigValidity;
import com.thoughtworks.go.domain.PipelineGroups;
import com.thoughtworks.go.helper.GoConfigMother;
import com.thoughtworks.go.i18n.Localizable;
import com.thoughtworks.go.i18n.LocalizedKeyValueMessage;
import com.thoughtworks.go.i18n.LocalizedMessage;
import com.thoughtworks.go.i18n.Localizer;
import com.thoughtworks.go.metrics.service.MetricsProbeService;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.responses.GoConfigOperationalResponse;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import com.thoughtworks.go.util.ConfigElementImplementationRegistryMother;
import com.thoughtworks.go.util.ReflectionUtil;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

public class PipelineConfigsServiceTest {

    private PipelineConfigsService service;
    private GoConfigService goConfigService;
    private ConfigCache configCache;
    private ConfigElementImplementationRegistry registry;
    private SecurityService securityService;
    private Username validUser;
    private HttpLocalizedOperationResult result;
    private GoConfigDao dao;
    private CruiseConfig cruiseConfig;

    @Before
    public void setUp() {
        goConfigService = mock(GoConfigService.class);
        securityService = mock(SecurityService.class);
        configCache = new ConfigCache();
        registry = ConfigElementImplementationRegistryMother.withNoPlugins();
        dao = mock(GoConfigDao.class);
        validUser = new Username(new CaseInsensitiveString("validUser"));
        MetricsProbeService metricsProbeService = mock(MetricsProbeService.class);
        service = new PipelineConfigsService(configCache, registry, goConfigService, securityService, metricsProbeService);
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
        Localizer localizer = mock(Localizer.class);
        when(localizer.localize("PIPELINE_GROUP_NOT_FOUND", groupName)).thenReturn("Not found");
        when(securityService.isUserAdminOfGroup(validUser.getUsername(), groupName)).thenThrow(new PipelineGroupNotFoundException());
        
        service.getXml(groupName, validUser, result);
        
        assertThat(result.httpCode(), is(404));
        assertThat(result.isSuccessful(), is(false));
        assertThat(result.message(localizer), is("Not found"));
        verify(securityService, times(1)).isUserAdminOfGroup(validUser.getUsername(), groupName);
        verify(goConfigService, never()).getConfigForEditing();

    }

    @Test
    public void shouldReturnUnauthorizedResultWhenUserIsNotAuthorizedToViewGroup_onGetXml() {
        String groupName = "some-secret-group";
        Localizer localizer = mock(Localizer.class);
        when(localizer.localize("UNAUTHORIZED_TO_EDIT_GROUP", groupName)).thenReturn("Unauthorized!");
        Username invalidUser = new Username(new CaseInsensitiveString("invalidUser"));
        when(securityService.isUserAdminOfGroup(invalidUser.getUsername(), groupName)).thenReturn(false);

        String actual = service.getXml(groupName, invalidUser, result);
        
        assertThat(actual, is(nullValue()));
        assertThat(result.httpCode(), is(401));
        assertThat(result.isSuccessful(), is(false));
        assertThat(result.message(localizer), is("Unauthorized!"));
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
        String errorMessage = "Can not parse xml";
        final String groupName = "group_name";
        Localizer localizer = mock(Localizer.class);
        when(localizer.localize("UPDATE_GROUP_XML_FAILED", groupName, errorMessage)).thenReturn("Invalid");
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
        assertThat(result.message(localizer), is("Invalid"));
        assertThat(validity.isValid(), is(false));
        verify(securityService, times(1)).isUserAdminOfGroup(validUser.getUsername(), groupName);
    }

    @Test
    public void shouldReturnUnsuccessfulResultWhenTheGroupIsNotFound_onUpdateXml() throws Exception {
        String groupName = "non-existent-group_name";
        Localizer localizer = mock(Localizer.class);
        when(localizer.localize("PIPELINE_GROUP_NOT_FOUND", groupName)).thenReturn("Not found");
        when(securityService.isUserAdminOfGroup(validUser.getUsername(), groupName)).thenThrow(new PipelineGroupNotFoundException());
        when(goConfigService.configFileMd5()).thenReturn("md5");

        GoConfigOperationalResponse<PipelineConfigs> actual = service.updateXml(groupName, "", "md5", validUser, result);
        PipelineConfigs configs = actual.getConfigElement();
        GoConfigValidity validity = actual.getValidity();

        assertThat(configs, is(nullValue()));
        assertThat(result.httpCode(), is(404));
        assertThat(result.isSuccessful(), is(false));
        assertThat(result.message(localizer), is("Not found"));
        assertThat(validity.isValid(), is(true));
        verify(securityService, times(1)).isUserAdminOfGroup(validUser.getUsername(), groupName);
    }

    @Test
    public void shouldReturnUnauthorizedResultWhenUserIsNotAuthorizedToViewGroup_onUpdateXml() throws Exception {
        String groupName = "some-secret-group";
        Localizer localizer = mock(Localizer.class);
        when(localizer.localize("UNAUTHORIZED_TO_EDIT_GROUP", groupName)).thenReturn("Unauthorized!");
        Username invalidUser = new Username(new CaseInsensitiveString("invalidUser"));
        when(securityService.isUserAdminOfGroup(invalidUser.getUsername(), groupName)).thenReturn(false);
        when(goConfigService.configFileMd5()).thenReturn("md5");
        
        GoConfigOperationalResponse<PipelineConfigs> actual = service.updateXml(groupName, "", "md5", invalidUser, result);
        PipelineConfigs configElement = actual.getConfigElement();
        GoConfigValidity validity = actual.getValidity();

        assertThat(configElement, is(nullValue()));
        assertThat(result.httpCode(), is(401));
        assertThat(result.isSuccessful(), is(false));
        assertThat(result.message(localizer), is("Unauthorized!"));
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

        assertThat(result.localizable(), is((Localizable) LocalizedMessage.string("SAVED_CONFIGURATION_SUCCESSFULLY")));
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

        assertThat(result.localizable(), is(LocalizedMessage.composite(LocalizedMessage.string("SAVED_CONFIGURATION_SUCCESSFULLY"), LocalizedMessage.string("CONFIG_MERGED"))));
        assertThat(validity.isValid(), is(true));
    }

    @Test
    public void shouldThrowUpWithDifferentMessageForMergeExceptions() throws Exception {
        String groupName = "renamed_group_name";
        GoConfigService.XmlPartialSaver groupSaver = mock(GoConfigService.XmlPartialSaver.class);
        when(securityService.isUserAdminOfGroup(validUser.getUsername(), groupName)).thenReturn(true);
        when(goConfigService.configFileMd5()).thenReturn("old-md5");
        when(goConfigService.groupSaver(groupName)).thenReturn(groupSaver);
        when(groupSaver.saveXml(null, "md5")).thenReturn(GoConfigValidity.invalid("some error").mergeConflict());

        GoConfigOperationalResponse<PipelineConfigs> actual = service.updateXml(groupName, null, "md5", validUser, result);
        PipelineConfigs configElement = actual.getConfigElement();
        GoConfigValidity validity = actual.getValidity();

        assertThat(configElement, is(nullValue()));
        assertThat(result.isSuccessful(), is(false));
        assertThat(validity.isValid(), is(false));
        assertThat(validity.isMergeConflict(), is(true));
        LocalizedKeyValueMessage message = (LocalizedKeyValueMessage) ReflectionUtil.getField(result, "message");
        String key = (String) ReflectionUtil.getField(message, "key");
        assertThat(key, is("FLASH_MESSAGE_ON_CONFLICT"));
    }

    @Test
    public void shouldThrowUpWithDifferentMessageForPostMergeValidationExceptions() throws Exception {
        String groupName = "renamed_group_name";
        GoConfigService.XmlPartialSaver groupSaver = mock(GoConfigService.XmlPartialSaver.class);
        when(securityService.isUserAdminOfGroup(validUser.getUsername(), groupName)).thenReturn(true);
        when(goConfigService.configFileMd5()).thenReturn("old-md5");
        when(goConfigService.groupSaver(groupName)).thenReturn(groupSaver);
        when(groupSaver.saveXml(null, "md5")).thenReturn(GoConfigValidity.invalid("some error").mergePostValidationError());

        GoConfigOperationalResponse<PipelineConfigs> actual = service.updateXml(groupName, null, "md5", validUser, result);
        PipelineConfigs configElement = actual.getConfigElement();
        GoConfigValidity validity = actual.getValidity();

        assertThat(configElement, is(nullValue()));
        assertThat(result.isSuccessful(), is(false));
        assertThat(validity.isValid(), is(false));
        assertThat(validity.isPostValidationError(), is(true));
        LocalizedKeyValueMessage message = (LocalizedKeyValueMessage) ReflectionUtil.getField(result, "message");
        String key = (String) ReflectionUtil.getField(message, "key");
        assertThat(key, is("FLASH_MESSAGE_ON_CONFLICT"));
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

		verify(goConfigService, never()).getAllPipelinesInGroup("group1");
		assertThat(gotPipelineGroups, is(Arrays.asList(group1)));
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

    private String parameterizedGroupXml() {
        return "<pipelines group=\"group_name\">\n"
                + "  <pipeline name=\"new_name\" labeltemplate=\"${COUNT}-#{foo}\">\n"
                + "    <materials>\n"
                + "      <svn url=\"file:///tmp/foo\" />\n"
                + "    </materials>\n"
                + "     <params>\n"
                + "      <param name=\"foo\">test</param>\n"
                + "    </params>"
                + "    <stage name=\"stage_name\">\n"
                + "      <jobs>\n"
                + "        <job name=\"job_name\" />\n"
                + "      </jobs>\n"
                + "    </stage>\n"
                + "  </pipeline>\n"
                + "</pipelines>";

    }

}
