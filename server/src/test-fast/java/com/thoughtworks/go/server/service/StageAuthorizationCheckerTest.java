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

import com.thoughtworks.go.server.service.result.ServerHealthStateOperationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class StageAuthorizationCheckerTest {
    private String pipelineName;
    private StageAuthorizationChecker checker;
    private String stageName;
    private String username;
    private SecurityService securityService;

    @BeforeEach
    public void setUp() throws Exception {
        securityService = mock(SecurityService.class);
        pipelineName = "cruise";
        stageName = "dev";
        username = "gli";
        checker = new StageAuthorizationChecker(pipelineName, stageName, username, securityService);
    }

    @Test
    public void shouldFailIfUserHasNoPermission() {
        when(securityService.hasOperatePermissionForStage(pipelineName, stageName, username)).thenReturn(false);

        ServerHealthStateOperationResult result = new ServerHealthStateOperationResult();
        checker.check(result);
        assertThat(result.getServerHealthState().isSuccess(), is(false));
        assertThat(result.getServerHealthState().getDescription(),
                is("User gli does not have permission to schedule cruise/dev"));
    }


    @Test
    public void shouldPassIfUserHasPermission() {
        when(securityService.hasOperatePermissionForStage(pipelineName, stageName, username)).thenReturn(true);

        ServerHealthStateOperationResult result = new ServerHealthStateOperationResult();
        checker.check(result);
        assertThat(result.getServerHealthState().isSuccess(), is(true));
    }
}
