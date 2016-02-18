/*
 * Copyright 2016 ThoughtWorks, Inc.
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

package com.thoughtworks.go.server.controller;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.ConfigSaveState;
import com.thoughtworks.go.config.validation.GoConfigValidity;
import com.thoughtworks.go.helper.ConfigFileFixture;
import com.thoughtworks.go.i18n.Localizer;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.AdminService;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.util.GoConstants;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.servlet.ModelAndView;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

public class AdminControllerTest {

    private MockHttpServletRequest request;
    private AdminController controller;

    private AdminService mockAdminService;
    private Localizer localizer;
    private GoConfigService mockGoConfigService;
    private Map<String, String> model = new HashMap<String, String>();
    private String userName;

    @Before
    public void setUp() throws Exception {
        request = new MockHttpServletRequest();
        mockGoConfigService = mock(GoConfigService.class);
        mockAdminService = mock(AdminService.class);

        localizer = mock(Localizer.class);
        controller = new AdminController(mockGoConfigService, mockAdminService, localizer);
        userName = CaseInsensitiveString.str(Username.ANONYMOUS.getUsername());
        model.put("active", "");
        model.put("current_tab", "");
        when(mockAdminService.populateModel(model)).thenReturn(new HashMap<String, String>());
        when(mockGoConfigService.checkConfigFileValid()).thenReturn(GoConfigValidity.valid(ConfigSaveState.UPDATED));
    }

    @Test
    public void testSaveConfigFileCorrectlyAndShowSuccessMessage() throws Exception {
        String configFileContent = "test";
        GoConfigService.XmlPartialSaver partialSaver = mock(GoConfigService.XmlPartialSaver.class);
        when(mockGoConfigService.fileSaver(false)).thenReturn(partialSaver);
        GoConfigValidity valid = GoConfigValidity.valid(ConfigSaveState.UPDATED);
        when(partialSaver.saveXml(configFileContent, "md5")).thenReturn(valid);
        when(localizer.localize("SAVED_CONFIGURATION_SUCCESSFULLY",new Object[]{})).thenReturn("Saved configuration successfully.");
        ModelAndView modelAndView = controller.handleEditConfiguration(configFileContent, "md5", false, request);

        verify(partialSaver).saveXml(configFileContent, "md5");
        assertThat((String) modelAndView.getModelMap().get(GoConstants.SUCCESS_MESSAGE), is("Saved configuration successfully."));
    }

    @Test
    public void testSaveConfigFileCorrectlyAndShowsMessageSayingItHasBeenMerged() throws Exception {
        String configFileContent = "test";
        GoConfigService.XmlPartialSaver partialSaver = mock(GoConfigService.XmlPartialSaver.class);
        when(mockGoConfigService.fileSaver(false)).thenReturn(partialSaver);
        when(partialSaver.saveXml(configFileContent, "md5")).thenReturn(GoConfigValidity.valid(ConfigSaveState.MERGED));
        when(localizer.localize("SAVED_CONFIGURATION_SUCCESSFULLY",new Object[]{})).thenReturn("Saved configuration successfully.");
        when(localizer.localize("CONFIG_MERGED",new Object[]{})).thenReturn("The configuration was modified by someone else, but your changes were merged successfully.");
        ModelAndView modelAndView = controller.handleEditConfiguration(configFileContent, "md5", false, request);

        verify(partialSaver).saveXml(configFileContent, "md5");
        assertThat((String) modelAndView.getModelMap().get(GoConstants.SUCCESS_MESSAGE), is("Saved configuration successfully. The configuration was modified by someone else, but your changes were merged successfully."));
    }

    @Test
    public void testShowInvalidMessageForInvalidConfigXMLAndNotMigrating() throws Exception {
        String configFileContent = "test";
        String message = "Invalid Config file";
        GoConfigService.XmlPartialSaver partialSaver = mock(GoConfigService.XmlPartialSaver.class);
        when(mockGoConfigService.fileSaver(false)).thenReturn(partialSaver);
        when(partialSaver.saveXml(configFileContent, "md5")).thenReturn(GoConfigValidity.invalid(new RuntimeException(message)));

        ModelAndView modelAndView = controller.handleEditConfiguration(configFileContent, "md5", null, request);

        verify(partialSaver).saveXml(configFileContent, "md5");
        assertThat((String) modelAndView.getModelMap().get(GoConstants.ERROR_FOR_PAGE), is(message));
    }

    @Test
    public void shouldUpgradeOldConfigXMLWhenChoosingMigration() throws Exception {
        String configFileContent = ConfigFileFixture.VERSION_0;
        GoConfigService.XmlPartialSaver partialSaver = mock(GoConfigService.XmlPartialSaver.class);
        when(localizer.localize("SAVED_CONFIGURATION_SUCCESSFULLY", new Object[]{})).thenReturn("Saved configuration successfully.");
        when(mockGoConfigService.fileSaver(true)).thenReturn(partialSaver);
        GoConfigValidity goConfigValidity = GoConfigValidity.valid(ConfigSaveState.UPDATED);
        when(partialSaver.saveXml(configFileContent, "md5")).thenReturn(goConfigValidity);

        ModelAndView modelAndView = controller.handleEditConfiguration(configFileContent, "md5", true, request);
        verify(partialSaver).saveXml(configFileContent, "md5");
        assertThat((String) modelAndView.getModelMap().get(GoConstants.SUCCESS_MESSAGE), is("Saved configuration successfully."));
    }

    @Test
    public void shouldReturnOldMd5InCaseOfValidationFailure() throws Exception {
        String configFileContent = "test";
        String message = "Invalid Config file";
        String latestContent = "content";
        String latestMd5 = "latest-md5";
        String oldMd5 = "old-md5";

        GoConfigService.XmlPartialSaver partialSaver = mock(GoConfigService.XmlPartialSaver.class);
        when(mockGoConfigService.fileSaver(false)).thenReturn(partialSaver);
        when(partialSaver.saveXml(configFileContent, oldMd5)).thenReturn(GoConfigValidity.invalid(new RuntimeException(message)));
        when(partialSaver.asXml()).thenReturn(latestContent);
        when(partialSaver.getMd5()).thenReturn(latestMd5);

        ModelAndView modelAndView = controller.handleEditConfiguration(configFileContent, oldMd5, null, request);

        verify(partialSaver).saveXml(configFileContent, oldMd5);
        assertThat((String) modelAndView.getModelMap().get("editing_content"), is(configFileContent));
        assertThat((String) modelAndView.getModelMap().get("editing_md5"), is(oldMd5));
    }

    @Test
    public void shouldReturnEditingMd5AsNullInCaseOfValidationFailure() throws Exception {
        String configFileContent = "test";
        String message = "Invalid Config file";
        String latestContent = "content";
        String latestMd5 = "latest-md5";
        String oldMd5 = "old-md5";

        GoConfigService.XmlPartialSaver partialSaver = mock(GoConfigService.XmlPartialSaver.class);
        when(mockGoConfigService.fileSaver(false)).thenReturn(partialSaver);
        when(partialSaver.saveXml(configFileContent, oldMd5)).thenReturn(GoConfigValidity.valid());
        when(partialSaver.asXml()).thenReturn(latestContent);
        when(partialSaver.getMd5()).thenReturn(latestMd5);

        ModelAndView modelAndView = controller.handleEditConfiguration(configFileContent, oldMd5, null, request);

        verify(partialSaver).saveXml(configFileContent, oldMd5);
        assertThat(modelAndView.getModelMap().get("editing_md5"), is(nullValue()));
        assertThat(modelAndView.getModelMap().get("editing_content"), is(nullValue()));
    }


}
