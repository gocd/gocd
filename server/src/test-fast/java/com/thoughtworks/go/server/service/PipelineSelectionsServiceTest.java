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
import com.thoughtworks.go.config.materials.MaterialConfigs;
import com.thoughtworks.go.domain.User;
import com.thoughtworks.go.server.dao.UserDao;
import com.thoughtworks.go.server.domain.user.*;
import com.thoughtworks.go.server.persistence.PipelineRepository;
import com.thoughtworks.go.util.Clock;
import com.thoughtworks.go.util.SystemEnvironment;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.Date;

import static com.thoughtworks.go.helper.ConfigFileFixture.configWith;
import static com.thoughtworks.go.server.domain.user.DashboardFilter.DEFAULT_NAME;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.mockito.Mockito.*;

public class PipelineSelectionsServiceTest {
    private static final Date FIXED_DATE = new DateTime(2000, 1, 1, 1, 1, 1, 1).toDate();

    private PipelineSelectionsService pipelineSelectionsService;
    private GoConfigDao goConfigDao;
    private GoConfigService goConfigService;
    private PipelineRepository pipelineRepository;
    private static final String PIPELINE = "pipeline1";
    private static final String STAGE = "stage1";
    private static final String JOB = "Job1";
    private UserDao userDao;

    @Before
    public void setup() throws Exception {
        new SystemEnvironment().setProperty(SystemEnvironment.ENFORCE_SERVER_IMMUTABILITY, "N");

        goConfigDao = mock(GoConfigDao.class);
        pipelineRepository = mock(PipelineRepository.class);

        CruiseConfig cruiseConfig = unchangedConfig();
        expectLoad(cruiseConfig);
        Clock clock = mock(Clock.class);
        when(clock.currentTime()).thenReturn(FIXED_DATE);
        userDao = mock(UserDao.class);

        goConfigService = mock(GoConfigService.class);
        pipelineSelectionsService = new PipelineSelectionsService(pipelineRepository, goConfigService, clock);
    }

    @Test
    public void shouldPersistPipelineSelections_WhenSecurityIsDisabled() {
        disableSecurity();
        final Filters filters = Filters.single(includes("pipeline1"));
        when(pipelineRepository.saveSelectedPipelines(pipelineSelectionsWithFilters(filters))).thenReturn(2L);
        assertEquals(2L, pipelineSelectionsService.save(null, null, filters));
        verify(pipelineRepository).saveSelectedPipelines(pipelineSelectionsWithFilters(filters));
    }

    @Test
    public void shouldPersistPipelineSelectionsForUser_WhenUserHasNoSelections() {
        enableSecurity();

        User user = getUser("badger");
        final Filters filters = Filters.single(includes("pipeline1"));

        when(pipelineRepository.findPipelineSelectionsByUserId(user.getId())).thenReturn(null);
        when(pipelineRepository.saveSelectedPipelines(pipelineSelectionsWithFilters(filters))).thenReturn(2L);

        long pipelineSelectionsId = pipelineSelectionsService.save("1", user.getId(), filters);

        assertEquals(2L, pipelineSelectionsId);
        verify(pipelineRepository).findPipelineSelectionsByUserId(user.getId());
        verify(pipelineRepository, never()).findPipelineSelectionsById(any());
        verify(pipelineRepository).saveSelectedPipelines(pipelineSelectionsWithFilters(filters));
    }

    @Test
    public void shouldUpdateExistingPersistedSelection_WhenSecurityIsEnabled() {
        enableSecurity();

        User user = getUser("badger");
        PipelineSelections pipelineSelections = PipelineSelectionsHelper.with(Collections.singletonList("pipeline2"), null, user.getId(), true);
        when(pipelineRepository.findPipelineSelectionsByUserId(user.getId())).thenReturn(pipelineSelections);
        when(pipelineRepository.saveSelectedPipelines(pipelineSelections)).thenReturn(2L);

        final Filters newFilters = Filters.single(excludes("pipelineX", "pipeline3"));
        long pipelineSelectionId = pipelineSelectionsService.save("1", user.getId(), newFilters);

        assertEquals(2L, pipelineSelectionId);
        assertEquals(newFilters, pipelineSelections.viewFilters());
        verify(pipelineRepository).saveSelectedPipelines(pipelineSelections);
        verify(pipelineRepository).findPipelineSelectionsByUserId(user.getId());
        verify(pipelineRepository, never()).findPipelineSelectionsById(any());
    }

    @Test
    public void shouldUpdateExistingPersistedSelection_WhenSecurityIsDisabled() {
        disableSecurity();

        PipelineSelections pipelineSelections = PipelineSelectionsHelper.with(Collections.singletonList("pip1"));
        when(pipelineRepository.findPipelineSelectionsById("123")).thenReturn(pipelineSelections);

        final Filters newFilters = Filters.single(excludes("pipelineX", "pipeline3"));
        assertNotEquals(newFilters, pipelineSelections.viewFilters()); // sanity check

        pipelineSelectionsService.save("123", null, newFilters);

        assertEquals(FIXED_DATE, pipelineSelections.lastUpdated());
        verify(pipelineRepository).findPipelineSelectionsById("123");
        verify(pipelineRepository).saveSelectedPipelines(pipelineSelectionsWithFilters(newFilters));
    }

    @Test
    public void shouldNotUpdatePipelineSelectionsWhenThereAreNoCustomFilters() {
        enableSecurity();
        when(pipelineRepository.findPipelineSelectionsByUserId(2L)).thenReturn(PipelineSelections.ALL);

        pipelineSelectionsService.update(null, 2L, new CaseInsensitiveString("newly-created-pipeline"));

        verify(pipelineRepository, never()).saveSelectedPipelines(any(PipelineSelections.class));
    }

    @Test
    public void shouldReturnPersistedPipelineSelectionsUsingCookieId_WhenSecurityisDisabled() {
        disableSecurity();

        PipelineSelections pipelineSelections = PipelineSelectionsHelper.with(Collections.singletonList("pip1"));
        when(pipelineRepository.findPipelineSelectionsById("123")).thenReturn(pipelineSelections);

        assertEquals(pipelineSelections, pipelineSelectionsService.load("123", null));

        // control case
        assertEquals(PipelineSelections.ALL, pipelineSelectionsService.load("345", null));
    }

    @Test
    public void shouldReturnPersistedPipelineSelectionsForUser_WhenSecurityIsEnabled() {
        enableSecurity();

        User loser = getUser("loser");
        PipelineSelections pipelineSelections = PipelineSelectionsHelper.with(Collections.singletonList("pip1"));

        when(pipelineRepository.findPipelineSelectionsByUserId(loser.getId())).thenReturn(pipelineSelections);

        assertEquals(pipelineSelections, pipelineSelectionsService.load(null, loser.getId()));
    }

    @Test
    public void shouldReturnAllPipelineSelections_WhenSecurityIsEnabled_AndNoPersistedSelections() {
        enableSecurity();

        User user = getUser("loser");
        when(pipelineRepository.findPipelineSelectionsByUserId(user.getId())).thenReturn(null);

        assertEquals(PipelineSelections.ALL, pipelineSelectionsService.load(null, user.getId()));
    }

    @Test
    public void shouldReturnAllPipelineSelections_WhenSecurityIsDisabled_AndNoPersistedSelections() {
        disableSecurity();

        when(pipelineRepository.findPipelineSelectionsById("5")).thenReturn(null);

        PipelineSelections selectedPipelines = pipelineSelectionsService.load("5", null);
        assertEquals(PipelineSelections.ALL, selectedPipelines);
    }

    private PipelineConfig createPipelineConfig(String... buildNames) {
        PipelineConfig pipeline = new PipelineConfig(new CaseInsensitiveString(PipelineSelectionsServiceTest.PIPELINE), new MaterialConfigs());
        pipeline.add(new StageConfig(new CaseInsensitiveString(PipelineSelectionsServiceTest.STAGE), jobConfigs(buildNames)));
        return pipeline;
    }

    private JobConfigs jobConfigs(String... buildNames) {
        JobConfigs jobConfigs = new JobConfigs();
        for (String buildName : buildNames) {
            jobConfigs.add(new JobConfig(buildName));
        }
        return jobConfigs;
    }

    private User getUser(final String userName) {
        User user = new User(userName);
        user.setId(10L);
        when(userDao.findUser(userName)).thenReturn(user);
        return user;
    }

    private PipelineSelections pipelineSelectionsWithFilters(Filters filters) {
        return argThat(ps -> {
            assertEquals(filters, ps.viewFilters());
            return true;
        });
    }

    private DashboardFilter excludes(String... pipelines) {
        return new ExcludesFilter(DEFAULT_NAME, CaseInsensitiveString.list(pipelines), Collections.emptySet());
    }

    private DashboardFilter includes(String... pipelines) {
        return new IncludesFilter(DEFAULT_NAME, CaseInsensitiveString.list(pipelines), Collections.emptySet());
    }

    private void expectLoad(final CruiseConfig result) throws Exception {
        when(goConfigDao.load()).thenReturn(result);
    }

    private CruiseConfig unchangedConfig() {
        return configWith(createPipelineConfig(JOB));
    }

    private void disableSecurity() {
        when(goConfigService.isSecurityEnabled()).thenReturn(false);
    }

    private void enableSecurity() {
        when(goConfigService.isSecurityEnabled()).thenReturn(true);
    }
}
