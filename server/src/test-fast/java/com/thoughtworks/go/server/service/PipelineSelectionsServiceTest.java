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
import com.thoughtworks.go.server.dao.PipelineSelectionDao;
import com.thoughtworks.go.server.dao.UserDao;
import com.thoughtworks.go.server.domain.user.*;
import com.thoughtworks.go.util.Clock;
import com.thoughtworks.go.util.SystemEnvironment;
import org.joda.time.DateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.sql.Timestamp;
import java.util.Collections;

import static com.thoughtworks.go.helper.ConfigFileFixture.configWith;
import static com.thoughtworks.go.server.domain.user.DashboardFilter.DEFAULT_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class PipelineSelectionsServiceTest {
    private static final Timestamp FIXED_DATE = new Timestamp(new DateTime(2000, 1, 1, 1, 1, 1, 1).toInstant().getMillis());

    private PipelineSelectionsService pipelineSelectionsService;
    @Mock
    private GoConfigDao goConfigDao;
    @Mock
    private GoConfigService goConfigService;
    @Mock
    private PipelineSelectionDao pipelineSelectionDao;
    @Mock
    private UserDao userDao;
    private static final String PIPELINE = "pipeline1";
    private static final String STAGE = "stage1";
    private static final String JOB = "Job1";

    @BeforeEach
    void setup() throws Exception {
        MockitoAnnotations.initMocks(this);
        new SystemEnvironment().setProperty(SystemEnvironment.ENFORCE_SERVER_IMMUTABILITY, "N");

        CruiseConfig cruiseConfig = unchangedConfig();
        expectLoad(cruiseConfig);
        Clock clock = mock(Clock.class);
        when(clock.currentTimestamp()).thenReturn(FIXED_DATE);
        pipelineSelectionsService = new PipelineSelectionsService(goConfigService, clock, pipelineSelectionDao);
    }

    @Test
    void shouldPersistPipelineSelections_WhenSecurityIsDisabled() {
        disableSecurity();
        final Filters filters = Filters.single(whitelist("pipeline1"));
        when(pipelineSelectionDao.saveOrUpdate(pipelineSelectionsWithFilters(filters))).thenReturn(2L);
        assertThat(pipelineSelectionsService.save(null, null, filters)).isEqualTo(2L);
        verify(pipelineSelectionDao).saveOrUpdate(pipelineSelectionsWithFilters(filters));
    }

    @Test
    void shouldPersistPipelineSelectionsForUser_WhenUserHasNoSelections() {
        enableSecurity();

        User user = getUser("badger");
        final Filters filters = Filters.single(whitelist("pipeline1"));

        when(pipelineSelectionDao.findPipelineSelectionsByUserId(user.getId())).thenReturn(null);
        when(pipelineSelectionDao.saveOrUpdate(pipelineSelectionsWithFilters(filters))).thenReturn(2L);

        long pipelineSelectionsId = pipelineSelectionsService.save("1", user.getId(), filters);

        assertThat(pipelineSelectionsId).isEqualTo(2L);
        verify(pipelineSelectionDao).findPipelineSelectionsByUserId(user.getId());
        verify(pipelineSelectionDao, never()).findPipelineSelectionsById(any());
        verify(pipelineSelectionDao).saveOrUpdate(pipelineSelectionsWithFilters(filters));
    }

    @Test
    void shouldUpdateExistingPersistedSelection_WhenSecurityIsEnabled() {
        enableSecurity();

        User user = getUser("badger");
        PipelineSelections pipelineSelections = PipelineSelectionsHelper.with(Collections.singletonList("pipeline2"), null, user.getId(), true);
        when(pipelineSelectionDao.findPipelineSelectionsByUserId(user.getId())).thenReturn(pipelineSelections);
        when(pipelineSelectionDao.saveOrUpdate(pipelineSelections)).thenReturn(2L);

        final Filters newFilters = Filters.single(blacklist("pipelineX", "pipeline3"));
        long pipelineSelectionId = pipelineSelectionsService.save("1", user.getId(), newFilters);

        assertThat(pipelineSelectionId).isEqualTo(2L);
        assertThat(pipelineSelections.getViewFilters()).isEqualTo(newFilters);
        verify(pipelineSelectionDao).saveOrUpdate(pipelineSelections);
        verify(pipelineSelectionDao).findPipelineSelectionsByUserId(user.getId());
        verify(pipelineSelectionDao, never()).findPipelineSelectionsById(any());
    }

    @Test
    void shouldUpdateExistingPersistedSelection_WhenSecurityIsDisabled() {
        disableSecurity();

        PipelineSelections pipelineSelections = PipelineSelectionsHelper.with(Collections.singletonList("pip1"));
        when(pipelineSelectionDao.findPipelineSelectionsById("123")).thenReturn(pipelineSelections);

        final Filters newFilters = Filters.single(blacklist("pipelineX", "pipeline3"));
        assertThat(pipelineSelections.getViewFilters()).isNotEqualTo(newFilters); // sanity check

        pipelineSelectionsService.save("123", null, newFilters);

        assertThat(pipelineSelections.getLastUpdated()).isEqualTo(FIXED_DATE);
        verify(pipelineSelectionDao).findPipelineSelectionsById("123");
        verify(pipelineSelectionDao).saveOrUpdate(pipelineSelectionsWithFilters(newFilters));
    }

    @Test
    void shouldNotUpdatePipelineSelectionsWhenThereAreNoCustomFilters() {
        enableSecurity();
        when(pipelineSelectionDao.findPipelineSelectionsByUserId(2L)).thenReturn(PipelineSelections.ALL);

        pipelineSelectionsService.update(null, 2L, new CaseInsensitiveString("newly-created-pipeline"));

        verify(pipelineSelectionDao, never()).saveOrUpdate(any(PipelineSelections.class));
    }

    @Test
    void shouldReturnPersistedPipelineSelectionsUsingCookieId_WhenSecurityisDisabled() {
        disableSecurity();

        PipelineSelections pipelineSelections = PipelineSelectionsHelper.with(Collections.singletonList("pip1"));
        when(pipelineSelectionDao.findPipelineSelectionsById("123")).thenReturn(pipelineSelections);

        assertThat(pipelineSelectionsService.load("123", null)).isEqualTo(pipelineSelections);

        // control case
        assertThat(pipelineSelectionsService.load("345", null)).isEqualTo(PipelineSelections.ALL);
    }

    @Test
    void shouldReturnPersistedPipelineSelectionsForUser_WhenSecurityIsEnabled() {
        enableSecurity();

        User loser = getUser("loser");
        PipelineSelections pipelineSelections = PipelineSelectionsHelper.with(Collections.singletonList("pip1"));

        when(pipelineSelectionDao.findPipelineSelectionsByUserId(loser.getId())).thenReturn(pipelineSelections);

        assertThat(pipelineSelectionsService.load(null, loser.getId())).isEqualTo(pipelineSelections);
    }

    @Test
    void shouldReturnAllPipelineSelections_WhenSecurityIsEnabled_AndNoPersistedSelections() {
        enableSecurity();

        User user = getUser("loser");
        when(pipelineSelectionDao.findPipelineSelectionsByUserId(user.getId())).thenReturn(null);

        assertThat(pipelineSelectionsService.load(null, user.getId())).isEqualTo(PipelineSelections.ALL);
    }

    @Test
    void shouldReturnAllPipelineSelections_WhenSecurityIsDisabled_AndNoPersistedSelections() {
        disableSecurity();

        when(pipelineSelectionDao.findPipelineSelectionsById("5")).thenReturn(null);

        PipelineSelections selectedPipelines = pipelineSelectionsService.load("5", null);
        assertThat(selectedPipelines).isEqualTo(PipelineSelections.ALL);
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
            assertThat(ps.getViewFilters()).isEqualTo(filters);
            return true;
        });
    }

    private DashboardFilter blacklist(String... pipelines) {
        return new BlacklistFilter(DEFAULT_NAME, CaseInsensitiveString.list(pipelines), Collections.emptySet());
    }

    private DashboardFilter whitelist(String... pipelines) {
        return new WhitelistFilter(DEFAULT_NAME, CaseInsensitiveString.list(pipelines), Collections.emptySet());
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
