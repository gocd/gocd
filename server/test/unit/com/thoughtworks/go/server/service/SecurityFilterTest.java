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

package com.thoughtworks.go.server.service;

import java.util.ArrayList;

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.domain.PipelineGroupVisitor;
import com.thoughtworks.go.helper.PipelineConfigMother;
import com.thoughtworks.go.util.ClassMockery;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JMock;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static com.thoughtworks.go.helper.PipelineConfigMother.pipelineConfig;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

public class SecurityFilterTest {
    @Rule
    public final JUnitRuleMockery context = new JUnitRuleMockery();
    private SecurityFilter securityFilter;
    private SecurityService securityService;
    private PipelineGroupVisitor pipelineGroupVisitor;
    private GoConfigService goConfigService;

    @Before
    public void setUp() {
        pipelineGroupVisitor = mock(PipelineGroupVisitor.class);
        securityService = mock(SecurityService.class);
        goConfigService = mock(GoConfigService.class);
        securityFilter = new SecurityFilter(pipelineGroupVisitor, goConfigService, securityService, "anyone");
    }

    @Test
    public void shouldVisitPipelineConfigsIfPassViewPermissionCheck() {
        final PipelineConfigs group = new BasicPipelineConfigs("group1", new Authorization(), pipelineConfig("pipeline1"));

        when(securityService.hasViewPermissionForGroup("anyone", "group1")).thenReturn(true);
        securityFilter.visit(group);
        verify(pipelineGroupVisitor).visit(group);
    }

    @Test
    public void shouldNotVisitPipelineConfigsIfNotPassViewPermissionCheck() {
        final PipelineConfigs group = new BasicPipelineConfigs("group1", new Authorization(), pipelineConfig("pipeline1"));

        when(securityService.hasViewPermissionForGroup("anyone", "group1")).thenReturn(false);
        securityFilter.visit(group);
        verifyNoMoreInteractions(pipelineGroupVisitor);
    }

    @Test
    public void shouldCallBackOnTheVisitorIfTheUserIsAPipelineGroupAdmin() throws Exception {
        final PipelineConfigs group = new BasicPipelineConfigs("group1", new Authorization(new AdminsConfig(new AdminUser(new CaseInsensitiveString("anyone")))), pipelineConfig("pipeline1"));

        when(securityService.hasViewPermissionForGroup("anyone", "group1")).thenReturn(true);
        when(goConfigService.rolesForUser(new CaseInsensitiveString("anyone"))).thenReturn(new ArrayList<Role>());
        securityFilter.visit(group);
        verify(pipelineGroupVisitor).visit(group);
    }
}
