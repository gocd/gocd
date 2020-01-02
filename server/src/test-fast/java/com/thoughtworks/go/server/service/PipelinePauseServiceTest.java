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

import ch.qos.logback.classic.Level;
import com.thoughtworks.go.config.*;
import com.thoughtworks.go.domain.PipelinePauseInfo;
import com.thoughtworks.go.helper.PipelineConfigMother;
import com.thoughtworks.go.server.dao.PipelineSqlMapDao;
import com.thoughtworks.go.server.domain.PipelinePauseChangeListener;
import com.thoughtworks.go.server.domain.PipelinePauseChangeListener.Event;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import com.thoughtworks.go.util.LogFixture;
import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.Test;

import static com.thoughtworks.go.util.LogFixture.logFixtureFor;
import static javax.servlet.http.HttpServletResponse.*;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

public class PipelinePauseServiceTest {
    private PipelineSqlMapDao pipelineDao;
    public PipelinePauseService pipelinePauseService;
    private GoConfigService goConfigService;
    private GoConfigDao goConfigDao;
    private SecurityService securityService;

    private static final String VALID_PIPELINE = "some-pipeline";
    private static final String PIPELINE_WITH_CAPITAL_NAME = "SOME-PIPELINE";
    private static final Username VALID_USER = new Username(new CaseInsensitiveString("admin"));
    private static final String INVALID_PIPELINE = "nonexistent-pipeline";
    private static final Username INVALID_USER = new Username(new CaseInsensitiveString("someone-who-not-operate"));

    @Before
    public void setUp() throws Exception {
        pipelineDao = mock(PipelineSqlMapDao.class);
        goConfigDao = mock(GoConfigDao.class);
        goConfigService = new GoConfigService(goConfigDao, null, (GoConfigMigration) null, null, null, null, null, null, null, null);
        securityService = mock(SecurityService.class);
        pipelinePauseService = new PipelinePauseService(pipelineDao, goConfigService, securityService);
        when(pipelineDao.pauseState(VALID_PIPELINE)).thenReturn(new PipelinePauseInfo(false, "", VALID_USER.getUsername().toString()));
    }

    private void setUpValidPipelineWithAuth() {
        Authorization authorization = new Authorization(new OperationConfig(new AdminUser(VALID_USER.getUsername())));
        CruiseConfig cruiseConfig = new BasicCruiseConfig(new BasicPipelineConfigs("my_group", authorization, PipelineConfigMother.pipelineConfig(VALID_PIPELINE)));
        when(goConfigDao.load()).thenReturn(cruiseConfig);
        when(securityService.hasOperatePermissionForGroup(eq(VALID_USER.getUsername()), any(String.class))).thenReturn(true);
    }

    private void setUpValidPipelineWithInvalidAuth() {
        Authorization authorization = new Authorization(new OperationConfig(new AdminUser(INVALID_USER.getUsername())));
        CruiseConfig cruiseConfig = new BasicCruiseConfig(new BasicPipelineConfigs("my_group", authorization, PipelineConfigMother.pipelineConfig(VALID_PIPELINE)));
        when(goConfigDao.load()).thenReturn(cruiseConfig);
        when(securityService.hasOperatePermissionForGroup(eq(INVALID_USER.getUsername()), any(String.class))).thenReturn(false);
    }

    @Test
    public void shouldPausePipeline() throws Exception {
        setUpValidPipelineWithAuth();

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();

        pipelinePauseService.pause(VALID_PIPELINE, "cause", VALID_USER, result);
        verify(pipelineDao).pause(VALID_PIPELINE, "cause", VALID_USER.getUsername().toString());

        assertThat(result.isSuccessful(), is(true));
        assertThat(result.httpCode(), is(SC_OK));
    }

    @Test
    public void shouldPausePipelineWithCaptialName() throws Exception {
        setUpValidPipelineWithAuth();
        when(pipelineDao.pauseState(PIPELINE_WITH_CAPITAL_NAME)).thenReturn(new PipelinePauseInfo(false, "", VALID_USER.getUsername().toString()));

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();

        pipelinePauseService.pause(PIPELINE_WITH_CAPITAL_NAME, "cause", VALID_USER, result);
        verify(pipelineDao).pause(PIPELINE_WITH_CAPITAL_NAME, "cause", VALID_USER.getUsername().toString());

        assertThat(result.isSuccessful(), is(true));
        assertThat(result.httpCode(), is(SC_OK));
    }

    @Test
    public void shouldUnPausePipeline() throws Exception {

        setUpValidPipelineWithAuth();

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();

        when(pipelineDao.pauseState(VALID_PIPELINE)).thenReturn(new PipelinePauseInfo(true, "", VALID_USER.getUsername().toString()));

        pipelinePauseService.unpause(VALID_PIPELINE, VALID_USER, result);
        verify(pipelineDao).unpause(VALID_PIPELINE);

        assertThat(result.isSuccessful(), is(true));
        assertThat(result.httpCode(), is(SC_OK));
    }

    @Test
    public void shouldPopulateHttpResult404WhenPipelineIsNotFoundForPause() throws Exception {
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        when(goConfigDao.load()).thenReturn(new BasicCruiseConfig());

        pipelinePauseService.pause(INVALID_PIPELINE, "cause", VALID_USER, result);

        assertThat(result.isSuccessful(), is(false));
        assertThat(result.httpCode(), is(SC_NOT_FOUND));
        verify(pipelineDao, never()).pause(INVALID_PIPELINE, "cause", "admin");
    }

    @Test
    public void shouldPopulateHttpResult404WhenPipelineIsNotFoundForUnpause() throws Exception {
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        when(goConfigDao.load()).thenReturn(new BasicCruiseConfig());

        pipelinePauseService.unpause(INVALID_PIPELINE, VALID_USER, result);

        assertThat(result.isSuccessful(), is(false));
        assertThat(result.httpCode(), is(SC_NOT_FOUND));
        verify(pipelineDao, never()).unpause(INVALID_PIPELINE);
    }

    @Test
    public void shouldPopulateHttpResult500WhenPipelinePauseResultsInAnError() throws Exception {

        setUpValidPipelineWithAuth();

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();

        doThrow(new RuntimeException("Failed to pause")).when(pipelineDao).pause(VALID_PIPELINE, "cause", VALID_USER.getUsername().toString());

        pipelinePauseService.pause(VALID_PIPELINE, "cause", VALID_USER, result);

        assertThat(result.isSuccessful(), is(false));
        assertThat(result.httpCode(), is(SC_INTERNAL_SERVER_ERROR));

        verify(pipelineDao).pause(VALID_PIPELINE, "cause", VALID_USER.getUsername().toString());
    }

    @Test
    public void shouldPopulateHttpResult500WhenPipelineUnPauseResultsInAnError() throws Exception {

        setUpValidPipelineWithAuth();

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();

        when(pipelineDao.pauseState(VALID_PIPELINE)).thenReturn(new PipelinePauseInfo(true, "", VALID_USER.getUsername().toString()));

        doThrow(new RuntimeException("Failed to unpause")).when(pipelineDao).unpause(VALID_PIPELINE);

        pipelinePauseService.unpause(VALID_PIPELINE, VALID_USER, result);

        assertThat(result.isSuccessful(), is(false));
        assertThat(result.httpCode(), is(SC_INTERNAL_SERVER_ERROR));

        verify(pipelineDao).unpause(VALID_PIPELINE);
    }

    @Test
    public void shouldPopulateHttpResult403WhenPipelineIsNotAuthorizedForPausing() throws Exception {
        setUpValidPipelineWithInvalidAuth();

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();

        pipelinePauseService.pause(VALID_PIPELINE, "cause", INVALID_USER, result);

        assertThat(result.isSuccessful(), is(false));
        assertThat(result.httpCode(), is(SC_FORBIDDEN));
        verify(pipelineDao, never()).pause(VALID_PIPELINE, "cause", "admin");
    }

    @Test
    public void shouldPopulateHttpResult403WhenPipelineIsNotAuthorizedForUnPausing() throws Exception {
        setUpValidPipelineWithInvalidAuth();

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();

        pipelinePauseService.unpause(VALID_PIPELINE, INVALID_USER, result);

        assertThat(result.isSuccessful(), is(false));
        assertThat(result.httpCode(), is(SC_FORBIDDEN));
        verify(pipelineDao, never()).unpause(VALID_PIPELINE);
    }

    @Test
    public void shouldPausePipelineEvenWhenPauseCauseIsNull() throws Exception {
        setUpValidPipelineWithAuth();

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();

        pipelinePauseService.pause(VALID_PIPELINE, null, VALID_USER, result);

        assertThat(result.isSuccessful(), is(true));
        assertThat(result.httpCode(), is(SC_OK));
        verify(pipelineDao).pause(VALID_PIPELINE, "", VALID_USER.getUsername().toString());
    }

    @Test
    public void shouldNotifyListenersAfterPipelineIsPaused() throws Exception {
        setUpValidPipelineWithAuth();

        PipelinePauseChangeListener listener1 = mock(PipelinePauseChangeListener.class);
        PipelinePauseChangeListener listener2 = mock(PipelinePauseChangeListener.class);

        pipelinePauseService.registerListener(listener1);
        pipelinePauseService.registerListener(listener2);
        pipelinePauseService.pause(VALID_PIPELINE, null, VALID_USER, new HttpLocalizedOperationResult());

        verify(listener1).pauseStatusChanged(Event.pause(VALID_PIPELINE, VALID_USER));
        verify(listener2).pauseStatusChanged(Event.pause(VALID_PIPELINE, VALID_USER));
    }

    @Test
    public void shouldNotifyListenersAfterPipelineIsUnpaused() throws Exception {
        setUpValidPipelineWithAuth();

        PipelinePauseChangeListener listener1 = mock(PipelinePauseChangeListener.class);
        PipelinePauseChangeListener listener2 = mock(PipelinePauseChangeListener.class);

        pipelinePauseService.registerListener(listener1);
        pipelinePauseService.registerListener(listener2);
        when(pipelineDao.pauseState(VALID_PIPELINE)).thenReturn(new PipelinePauseInfo(true, "", VALID_USER.getUsername().toString()));
        pipelinePauseService.unpause(VALID_PIPELINE, VALID_USER, new HttpLocalizedOperationResult());

        verify(listener1).pauseStatusChanged(Event.unPause(VALID_PIPELINE, VALID_USER));
        verify(listener2).pauseStatusChanged(Event.unPause(VALID_PIPELINE, VALID_USER));
    }

    @Test
    public void shouldLogAndIgnoreAnyExceptionsWhileNotifyingListeners() throws Exception {
        setUpValidPipelineWithAuth();

        PipelinePauseChangeListener listener1 = mock(PipelinePauseChangeListener.class);
        PipelinePauseChangeListener listener2 = mock(PipelinePauseChangeListener.class, "ListenerWhichFails");
        doThrow(new RuntimeException("Ouch.")).when(listener2).pauseStatusChanged(org.mockito.ArgumentMatchers.<Event>anyObject());
        PipelinePauseChangeListener listener3 = mock(PipelinePauseChangeListener.class);

        try (LogFixture logFixture = logFixtureFor(PipelinePauseService.class, Level.WARN)) {
            HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();

            pipelinePauseService.registerListener(listener1);
            pipelinePauseService.registerListener(listener2);
            pipelinePauseService.registerListener(listener3);
            pipelinePauseService.pause(VALID_PIPELINE, "reason", VALID_USER, result);

            synchronized (logFixture) {
                assertTrue(logFixture.getLog(), logFixture.contains(Level.WARN, "Failed to notify listener (ListenerWhichFails)"));
            }
            assertThat(result.isSuccessful(), is(true));
            assertThat(result.httpCode(), is(HttpStatus.SC_OK));
        }

        verify(pipelineDao).pause(VALID_PIPELINE, "reason", VALID_USER.getUsername().toString());

        verify(listener1).pauseStatusChanged(Event.pause(VALID_PIPELINE, VALID_USER));
        verify(listener2).pauseStatusChanged(Event.pause(VALID_PIPELINE, VALID_USER));
        verify(listener3).pauseStatusChanged(Event.pause(VALID_PIPELINE, VALID_USER));

    }

    @Test
    public void shouldNotPausePipelineWhenPipelineIsAlreadyPaused() throws Exception {
        setUpValidPipelineWithAuth();

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        when(pipelineDao.pauseState(VALID_PIPELINE)).thenReturn(new PipelinePauseInfo(true, "", VALID_USER.getUsername().toString()));
        pipelinePauseService.pause(VALID_PIPELINE, null, VALID_USER, result);

        assertThat(result.isSuccessful(), is(false));
        assertThat(result.httpCode(), is(409));
    }

    @Test
    public void shouldNotUnpausePipelineWhenPipelineIsAlreadyUnaused() throws Exception {
        setUpValidPipelineWithAuth();

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        when(pipelineDao.pauseState(VALID_PIPELINE)).thenReturn(new PipelinePauseInfo(false, "", VALID_USER.getUsername().toString()));
        pipelinePauseService.unpause(VALID_PIPELINE, VALID_USER, result);

        assertThat(result.isSuccessful(), is(false));
        assertThat(result.httpCode(), is(409));
    }

    @Test
    public void shouldPopulatePausePipelineSuccessResult() throws Exception {
        setUpValidPipelineWithAuth();

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        pipelinePauseService.pause(VALID_PIPELINE, null, VALID_USER, result);

        assertThat(result.isSuccessful(), is(true));
        assertThat(result.httpCode(), is(SC_OK));

        assertThat(result.message(), is("Pipeline 'some-pipeline' paused successfully."));
    }

    @Test
    public void shouldPopulateUnpausePipelineSuccessResult() throws Exception {
        setUpValidPipelineWithAuth();

        when(pipelineDao.pauseState(VALID_PIPELINE)).thenReturn(new PipelinePauseInfo(true, "", VALID_USER.getUsername().toString()));
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        pipelinePauseService.unpause(VALID_PIPELINE, VALID_USER, result);

        assertThat(result.isSuccessful(), is(true));
        assertThat(result.httpCode(), is(SC_OK));

        assertThat(result.message(), is("Pipeline 'some-pipeline' unpaused successfully."));
    }

}
