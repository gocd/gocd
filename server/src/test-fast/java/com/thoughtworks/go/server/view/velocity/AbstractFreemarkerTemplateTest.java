/*
 * Copyright 2022 Thoughtworks, Inc.
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

package com.thoughtworks.go.server.view.velocity;

import com.thoughtworks.go.server.service.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.AdditionalAnswers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AbstractFreemarkerTemplateTest {
    protected TestFreeMarkerView view;
    @Mock
    private RailsAssetsService railsAssetsService;
    @Mock
    private SecurityService securityService;
    @Mock
    private VersionInfoService versionInfoService;
    @Mock
    private WebpackAssetsService webpackAssetsService;
    @Mock
    private MaintenanceModeService maintenanceModeService;

    public void setUp(String template) throws Exception {

        view = spy(new TestFreeMarkerView(template));

        lenient().doReturn(railsAssetsService).when(view).getRailsAssetsService();

        when(railsAssetsService.getAssetPath(any())).then(AdditionalAnswers.returnsFirstArg());

        lenient().doReturn(versionInfoService).when(view).getVersionInfoService();
        lenient().doReturn(webpackAssetsService).when(view).webpackAssetsService();
        lenient().doReturn(securityService).when(view).getSecurityService();
        lenient().doReturn(maintenanceModeService).when(view).getMaintenanceModeService();
    }
}
