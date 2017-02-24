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

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.domain.Pipeline;
import com.thoughtworks.go.domain.PipelineIdentifier;
import com.thoughtworks.go.domain.StageIdentifier;
import com.thoughtworks.go.server.dao.PipelineSqlMapDao;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.result.HttpOperationResult;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

public class PipelineUnlockApiServiceTest {
    private PipelineUnlockApiService pipelineUnlockApiService;
    private PipelineSqlMapDao pipelineDao;
    private GoConfigService goConfigService;
    private CachedCurrentActivityService currentActivityService;
    private SecurityService securityService;
    private PipelineLockService pipelineLockService;

    @Before public void setup() throws Exception {
        pipelineDao = mock(PipelineSqlMapDao.class);
        goConfigService = mock(GoConfigService.class);
        currentActivityService = Mockito.mock(CachedCurrentActivityService.class);
        securityService = Mockito.mock(SecurityService.class);
        pipelineLockService = Mockito.mock(PipelineLockService.class);
        pipelineUnlockApiService = new PipelineUnlockApiService(goConfigService, pipelineLockService, currentActivityService, securityService);
    }

    @Test public void unlockShouldSetResultToOkWhenSuccessful() throws Exception {
        Mockito.when(securityService.hasOperatePermissionForPipeline(new CaseInsensitiveString("username"), "pipeline-name")).thenReturn(true);
        Mockito.when(goConfigService.hasPipelineNamed(new CaseInsensitiveString("pipeline-name"))).thenReturn(true);
        Mockito.when(goConfigService.isLockable("pipeline-name")).thenReturn(true);
        Mockito.when(pipelineLockService.lockedPipeline("pipeline-name")).thenReturn(new StageIdentifier("pipeline-name", 10, "10", "stage", "2"));
        Pipeline pipeline = new Pipeline();
        Mockito.when(pipelineDao.findPipelineByNameAndCounter("pipeline-name", 10)).thenReturn(pipeline);
        HttpOperationResult result = new HttpOperationResult();

        pipelineUnlockApiService.unlock("pipeline-name", new Username(new CaseInsensitiveString("username")), result);

        assertThat(result.httpCode(), is(200));
        assertThat(result.message(), is("pipeline lock released for pipeline-name"));
        Mockito.verify(pipelineLockService).unlock("pipeline-name");
    }

    @Test public void unlockShouldSetResultToNotFoundWhenPipelineDoesNotExist() throws Exception {
        Mockito.when(securityService.hasOperatePermissionForPipeline(new CaseInsensitiveString("username"), "pipeline-name")).thenReturn(true);
        Mockito.when(goConfigService.hasPipelineNamed(new CaseInsensitiveString("does-not-exist"))).thenReturn(false);
        Pipeline pipeline = new Pipeline();
        HttpOperationResult result = new HttpOperationResult();

        pipelineUnlockApiService.unlock("does-not-exist", new Username(new CaseInsensitiveString("username")), result);

        assertThat(result.httpCode(), is(404));
        assertThat(result.message(), is("pipeline name does-not-exist is incorrect"));
        Mockito.verify(goConfigService).hasPipelineNamed(new CaseInsensitiveString("does-not-exist"));
        Mockito.verify(pipelineLockService, never()).unlock("does-not-exist");
    }

    @Test public void unlockShouldSetResultToNotAcceptableWhenPipelineIsNotLockable() throws Exception {
        Mockito.when(securityService.hasOperatePermissionForPipeline(new CaseInsensitiveString("username"), "pipeline-name")).thenReturn(true);
        Mockito.when(goConfigService.hasPipelineNamed(new CaseInsensitiveString("pipeline-name"))).thenReturn(true);
        Mockito.when(goConfigService.isLockable("pipeline-name")).thenReturn(false);

        HttpOperationResult result = new HttpOperationResult();
        pipelineUnlockApiService.unlock("pipeline-name", new Username(new CaseInsensitiveString("username")), result);

        assertThat(result.httpCode(), is(406));
        assertThat(result.message(), is("no lock exists within the pipeline configuration for pipeline-name"));
        Mockito.verify(pipelineLockService, never()).unlock(Mockito.any(String.class));
    }

    @Test public void unlockShouldSetResultToNotAcceptableWhenNoPipelineInstanceIsCurrentlyLocked() throws Exception {
        Mockito.when(securityService.hasOperatePermissionForPipeline(new CaseInsensitiveString("username"), "pipeline-name")).thenReturn(true);
        Mockito.when(goConfigService.hasPipelineNamed(new CaseInsensitiveString("pipeline-name"))).thenReturn(true);
        Mockito.when(goConfigService.isLockable("pipeline-name")).thenReturn(true);
        Mockito.when(pipelineLockService.lockedPipeline("pipeline-name")).thenReturn(null);

        HttpOperationResult result = new HttpOperationResult();
        pipelineUnlockApiService.unlock("pipeline-name", new Username(new CaseInsensitiveString("username")), result);

        assertThat(result.httpCode(), is(406));
        assertThat(result.message(), is("lock exists within the pipeline configuration but no pipeline instance is currently in progress"));
    }


    @Test public void unlockShouldSetResultToNotAcceptableWhenAPipelineInstanceIsCurrentlyRunning() throws Exception {
        Mockito.when(securityService.hasOperatePermissionForPipeline(new CaseInsensitiveString("username"), "pipeline-name")).thenReturn(true);
        Mockito.when(goConfigService.hasPipelineNamed(new CaseInsensitiveString("pipeline-name"))).thenReturn(true);
        Mockito.when(goConfigService.isLockable("pipeline-name")).thenReturn(true);
        StageIdentifier identifier = new StageIdentifier("pipeline-name", 10, "10", "stage", "1");
        Mockito.when(pipelineLockService.lockedPipeline("pipeline-name")).thenReturn(identifier);
        Mockito.when(currentActivityService.isAnyStageActive(identifier.pipelineIdentifier())).thenReturn(true);

        HttpOperationResult result = new HttpOperationResult();
        pipelineUnlockApiService.unlock("pipeline-name", new Username(new CaseInsensitiveString("username")), result);

        assertThat(result.httpCode(), is(406));
        assertThat(result.message(), is("locked pipeline instance is currently running (one of the stages is in progress)"));
    }

    @Test public void unlockShouldSetResultToNotAuthorizedWhenUserDoesNotHaveOperatePermissions() throws Exception {
        String pipelineName = "pipeline-name";
        Mockito.when(securityService.hasOperatePermissionForPipeline(new CaseInsensitiveString("username"), pipelineName)).thenReturn(false);
        Mockito.when(goConfigService.hasPipelineNamed(new CaseInsensitiveString(pipelineName))).thenReturn(true);
        Mockito.when(goConfigService.isLockable(pipelineName)).thenReturn(true);
        StageIdentifier identifier = new StageIdentifier(pipelineName, 10, "10", "stage", "1");
        Mockito.when(pipelineLockService.lockedPipeline(pipelineName)).thenReturn(identifier);
        Mockito.when(currentActivityService.isAnyStageActive(new PipelineIdentifier(pipelineName, 10, "10"))).thenReturn(false);

        HttpOperationResult result = new HttpOperationResult();
        pipelineUnlockApiService.unlock(pipelineName, new Username(new CaseInsensitiveString("username")), result);

        assertThat(result.httpCode(), is(401));
        assertThat(result.message(), is("user does not have operate permission on the pipeline"));
    }
}
