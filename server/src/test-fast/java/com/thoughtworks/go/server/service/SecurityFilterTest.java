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
package com.thoughtworks.go.server.service;

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.domain.PipelineGroupVisitor;
import com.thoughtworks.go.helper.PipelineConfigMother;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.mockito.Mockito.*;

public class SecurityFilterTest {
    private SecurityFilter securityFilter;
    private SecurityService securityService;
    private PipelineGroupVisitor pipelineGroupVisitor;
    private GoConfigService goConfigService;

    @BeforeEach
    public void setUp() {
        pipelineGroupVisitor = mock(PipelineGroupVisitor.class);
        securityService = mock(SecurityService.class);
        goConfigService = mock(GoConfigService.class);
        securityFilter = new SecurityFilter(pipelineGroupVisitor, goConfigService, securityService, "anyone");
    }

    @Test
    public void shouldVisitPipelineConfigsIfPassViewPermissionCheck() {
        final PipelineConfigs group = new BasicPipelineConfigs("group1", new Authorization(), PipelineConfigMother.pipelineConfig("pipeline1"));

        when(securityService.hasViewPermissionForGroup("anyone", "group1")).thenReturn(true);
        securityFilter.visit(group);
        verify(pipelineGroupVisitor).visit(group);
    }

    @Test
    public void shouldNotVisitPipelineConfigsIfNotPassViewPermissionCheck() {
        final PipelineConfigs group = new BasicPipelineConfigs("group1", new Authorization(), PipelineConfigMother.pipelineConfig("pipeline1"));

        when(securityService.hasViewPermissionForGroup("anyone", "group1")).thenReturn(false);
        securityFilter.visit(group);
        verifyNoMoreInteractions(pipelineGroupVisitor);
    }

    @Test
    public void shouldCallBackOnTheVisitorIfTheUserIsAPipelineGroupAdmin() throws Exception {
        final PipelineConfigs group = new BasicPipelineConfigs("group1", new Authorization(new AdminsConfig(new AdminUser(new CaseInsensitiveString("anyone")))), PipelineConfigMother.pipelineConfig("pipeline1"));

        when(securityService.hasViewPermissionForGroup("anyone", "group1")).thenReturn(true);
        when(goConfigService.rolesForUser(new CaseInsensitiveString("anyone"))).thenReturn(new ArrayList<>());
        securityFilter.visit(group);
        verify(pipelineGroupVisitor).visit(group);
    }
}
