/*
 * Copyright 2017 ThoughtWorks, Inc.
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
import com.thoughtworks.go.config.registry.ConfigElementImplementationRegistry;
import com.thoughtworks.go.domain.User;
import com.thoughtworks.go.server.dao.UserDao;
import com.thoughtworks.go.server.domain.user.PipelineSelections;
import com.thoughtworks.go.server.persistence.PipelineRepository;
import com.thoughtworks.go.util.Clock;
import com.thoughtworks.go.util.ConfigElementImplementationRegistryMother;
import com.thoughtworks.go.util.SystemEnvironment;
import org.apache.commons.lang.StringUtils;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static com.thoughtworks.go.helper.ConfigFileFixture.configWith;
import static com.thoughtworks.go.helper.PipelineConfigMother.pipelineConfig;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.*;

public class PipelineSelectionsServiceTest {
    private PipelineSelectionsService pipelineSelectionsService;
    private GoConfigDao goConfigDao;
    private GoConfigService goConfigService;
    private PipelineRepository pipelineRepository;
    private static final String PIPELINE = "pipeline1";
    private static final String STAGE = "stage1";
    private static final String JOB = "Job1";
    private CruiseConfig cruiseConfig;
    private Clock clock;
    private UserDao userDao;
    public PipelinePauseService pipelinePauseService;
    private SystemEnvironment systemEnvironment;

    @Before
    public void setup() throws Exception {
        new SystemEnvironment().setProperty(SystemEnvironment.ENFORCE_SERVER_IMMUTABILITY, "N");

        goConfigDao = mock(GoConfigDao.class);
        pipelineRepository = mock(PipelineRepository.class);
        pipelinePauseService = mock(PipelinePauseService.class);
        systemEnvironment = mock(SystemEnvironment.class);

        cruiseConfig = unchangedConfig();
        expectLoad(cruiseConfig);
        this.clock = mock(Clock.class);
        userDao = mock(UserDao.class);
        stub(systemEnvironment.optimizeFullConfigSave()).toReturn(false);

        ConfigElementImplementationRegistry registry = ConfigElementImplementationRegistryMother.withNoPlugins();
        goConfigService = mock(GoConfigService.class);
        pipelineSelectionsService = new PipelineSelectionsService(pipelineRepository, goConfigService, clock);
    }

    @Test
    public void shouldNotUpdatePipelineSelectionsWhenTheUserIsAnonymousAndHasNeverSelectedPipelines() {
        pipelineSelectionsService.updateUserPipelineSelections(null, null, new CaseInsensitiveString("pipelineNew"));

        verify(pipelineRepository, times(0)).saveSelectedPipelines(argThat(Matchers.any(PipelineSelections.class)));
    }

    @Test
    public void shouldNotUpdatePipelineSelectionsWhenTheUserIsAnonymousAndHasSelectedPipelines_WithBlacklist() {
        when(pipelineRepository.findPipelineSelectionsById("1")).thenReturn(new PipelineSelections(Arrays.asList("pipeline1", "pipeline2"), null, null, true));

        pipelineSelectionsService.updateUserPipelineSelections("1", null, new CaseInsensitiveString("pipelineNew"));

        verify(pipelineRepository).findPipelineSelectionsById("1");
        verify(pipelineRepository, times(0)).saveSelectedPipelines(argThat(Matchers.any(PipelineSelections.class)));
    }

    @Test
    public void shouldUpdatePipelineSelectionsWhenTheUserIsAnonymousAndHasSelectedPipelines_WithWhitelist() {
        when(pipelineRepository.findPipelineSelectionsById("1")).thenReturn(new PipelineSelections(Arrays.asList("pipeline1", "pipeline2"), null, null, false));

        pipelineSelectionsService.updateUserPipelineSelections("1", null, new CaseInsensitiveString("pipelineNew"));

        verify(pipelineRepository).findPipelineSelectionsById("1");
        verify(pipelineRepository, times(1)).saveSelectedPipelines(argThat(isAPipelineSelectionsInstanceWith(false, "pipeline1", "pipeline2", "pipelineNew")));
    }

    @Test
    public void shouldNotUpdatePipelineSelectionsWhenTheUserIsLoggedIn_WithBlacklist() {
        when(goConfigService.getAllPipelineConfigs()).thenReturn(Arrays.asList(pipelineConfig("pipeline1"), pipelineConfig("pipeline2"), pipelineConfig("pipelineX"), pipelineConfig("pipeline3")));
        when(goConfigService.isSecurityEnabled()).thenReturn(true);
        when(pipelineRepository.findPipelineSelectionsByUserId(1L)).thenReturn(new PipelineSelections(Arrays.asList("pipeline1", "pipeline2"), null, null, true));

        pipelineSelectionsService.updateUserPipelineSelections(null, 1L, new CaseInsensitiveString("pipelineNew"));

        verify(pipelineRepository).findPipelineSelectionsByUserId(1L);
        verify(pipelineRepository, times(0)).saveSelectedPipelines(argThat(Matchers.any(PipelineSelections.class)));
    }

    @Test
    public void shouldUpdatePipelineSelectionsWhenTheUserIsLoggedIn_WithWhitelist() {
        when(goConfigService.getAllPipelineConfigs()).thenReturn(Arrays.asList(pipelineConfig("pipeline1"), pipelineConfig("pipeline2"), pipelineConfig("pipelineX"), pipelineConfig("pipeline3")));
        when(goConfigService.isSecurityEnabled()).thenReturn(true);
        when(pipelineRepository.findPipelineSelectionsByUserId(1L)).thenReturn(new PipelineSelections(Arrays.asList("pipeline1", "pipeline2"), null, null, false));

        pipelineSelectionsService.updateUserPipelineSelections(null, 1L, new CaseInsensitiveString("pipelineNew"));

        verify(pipelineRepository).findPipelineSelectionsByUserId(1L);
        verify(pipelineRepository, times(1)).saveSelectedPipelines(argThat(isAPipelineSelectionsInstanceWith(false, "pipeline1", "pipeline2", "pipelineNew")));
    }

    @Test
    public void shouldPersistPipelineSelections_WhenSecurityIsDisabled() {
        Date date = new DateTime(2000, 1, 1, 1, 1, 1, 1).toDate();
        when(clock.currentTime()).thenReturn(date);
        when(goConfigService.getAllPipelineConfigs()).thenReturn(Arrays.asList(pipelineConfig("pipeline1"), pipelineConfig("pipeline2"), pipelineConfig("pipelineX"), pipelineConfig("pipeline3")));
        Matcher<PipelineSelections> pipelineSelectionsMatcher = hasValues(Arrays.asList("pipelineX", "pipeline3"), Arrays.asList("pipeline1", "pipeline2"), date, null);
        when(pipelineRepository.saveSelectedPipelines(argThat(pipelineSelectionsMatcher))).thenReturn(2L);
        assertThat(pipelineSelectionsService.persistSelectedPipelines(null, null, Arrays.asList("pipelineX", "pipeline3"), true), is(2l));
        verify(pipelineRepository).saveSelectedPipelines(argThat(pipelineSelectionsMatcher));
    }

    @Test
    public void shouldPersistPipelineSelectionsAgainstUser_AlreadyHavingSelections() {
        Date date = new DateTime(2000, 1, 1, 1, 1, 1, 1).toDate();
        when(clock.currentTime()).thenReturn(date);
        when(goConfigService.getAllPipelineConfigs()).thenReturn(Arrays.asList(pipelineConfig("pipeline1"), pipelineConfig("pipeline2"), pipelineConfig("pipelineX"), pipelineConfig("pipeline3")));
        when(goConfigService.isSecurityEnabled()).thenReturn(true);

        User user = getUser("badger", 10L);
        PipelineSelections pipelineSelections = new PipelineSelections(Arrays.asList("pipeline2"), new Date(), user.getId(), true);
        when(pipelineRepository.findPipelineSelectionsByUserId(user.getId())).thenReturn(pipelineSelections);
        when(pipelineRepository.saveSelectedPipelines(pipelineSelections)).thenReturn(2L);

        long pipelineSelectionId = pipelineSelectionsService.persistSelectedPipelines("1", user.getId(), Arrays.asList("pipelineX", "pipeline3"), true);

        assertThat(pipelineSelections.getSelections(), is("pipeline1,pipeline2"));
        assertThat(pipelineSelectionId, is(2l));
        verify(pipelineRepository).saveSelectedPipelines(pipelineSelections);
        verify(pipelineRepository).findPipelineSelectionsByUserId(user.getId());
        verify(pipelineRepository, never()).findPipelineSelectionsById("1");
    }

    @Test
    public void shouldPersistPipelineSelectionsAgainstUser_WhenUserHasNoSelections() {
        Date date = new DateTime(2000, 1, 1, 1, 1, 1, 1).toDate();
        when(clock.currentTime()).thenReturn(date);
        User user = getUser("badger", 10L);
        Matcher<PipelineSelections> pipelineSelectionsMatcher = hasValues(Arrays.asList("pipelineX", "pipeline3"), Arrays.asList("pipeline1", "pipeline2"), date, user.getId());
        when(pipelineRepository.findPipelineSelectionsByUserId(user.getId())).thenReturn(null);
        when(pipelineRepository.saveSelectedPipelines(argThat(pipelineSelectionsMatcher))).thenReturn(2L);
        when(goConfigService.getAllPipelineConfigs()).thenReturn(Arrays.asList(pipelineConfig("pipeline1"), pipelineConfig("pipeline2"), pipelineConfig("pipelineX"), pipelineConfig("pipeline3")));
        when(goConfigService.isSecurityEnabled()).thenReturn(true);

        long pipelineSelectionsId = pipelineSelectionsService.persistSelectedPipelines("1", user.getId(), Arrays.asList("pipelineX", "pipeline3"), true);

        assertThat(pipelineSelectionsId, is(2l));
        verify(pipelineRepository).saveSelectedPipelines(argThat(pipelineSelectionsMatcher));
        verify(pipelineRepository).findPipelineSelectionsByUserId(user.getId());
        verify(pipelineRepository, never()).findPipelineSelectionsById("1");
    }

    @Test
    public void shouldPersistPipelineSelectionsShouldRemovePipelinesFromSelectedGroups() {
        when(goConfigService.getAllPipelineConfigs()).thenReturn(Arrays.asList(pipelineConfig("pipeline1"), pipelineConfig("pipeline2"), pipelineConfig("pipelineX"), pipelineConfig("pipeline3"), pipelineConfig("pipeline4")));
        pipelineSelectionsService.persistSelectedPipelines(null, null, Arrays.asList("pipeline1", "pipeline2", "pipeline3"), true);
        verify(pipelineRepository).saveSelectedPipelines(argThat(hasValues(Arrays.asList("pipeline1", "pipeline2", "pipeline3"), Arrays.asList("pipelineX", "pipeline4"), clock.currentTime(), null)));
    }

    @Test
    public void shouldPersistInvertedListOfPipelineSelections_WhenBlacklistIsSelected() {
        Date date = new DateTime(2000, 1, 1, 1, 1, 1, 1).toDate();
        when(clock.currentTime()).thenReturn(date);
        when(goConfigService.getAllPipelineConfigs()).thenReturn(Arrays.asList(pipelineConfig("pipeline1"), pipelineConfig("pipeline2"), pipelineConfig("pipelineX"), pipelineConfig("pipeline3")));
        when(goConfigService.isSecurityEnabled()).thenReturn(true);

        User user = getUser("badger", 10L);
        PipelineSelections blacklistPipelineSelections = new PipelineSelections(new ArrayList<String>(), date, user.getId(), false);
        when(pipelineRepository.findPipelineSelectionsByUserId(user.getId())).thenReturn(blacklistPipelineSelections);

        pipelineSelectionsService.persistSelectedPipelines(null, user.getId(), Arrays.asList("pipelineX", "pipeline3"), true);

        verify(pipelineRepository).saveSelectedPipelines(argThat(isAPipelineSelectionsInstanceWith(true, "pipeline1", "pipeline2")));
    }

    @Test
    public void shouldPersistNonInvertedListOfPipelineSelections_WhenWhitelistIsSelected() {
        Date date = new DateTime(2000, 1, 1, 1, 1, 1, 1).toDate();
        when(clock.currentTime()).thenReturn(date);
        when(goConfigService.getAllPipelineConfigs()).thenReturn(Arrays.asList(pipelineConfig("pipeline1"), pipelineConfig("pipeline2"), pipelineConfig("pipelineX"), pipelineConfig("pipeline3")));
        when(goConfigService.isSecurityEnabled()).thenReturn(true);

        User user = getUser("badger", 10L);
        PipelineSelections whitelistPipelineSelections = new PipelineSelections(new ArrayList<String>(), date, user.getId(), true);
        when(pipelineRepository.findPipelineSelectionsByUserId(user.getId())).thenReturn(whitelistPipelineSelections);

        pipelineSelectionsService.persistSelectedPipelines(null, user.getId(), Arrays.asList("pipelineX", "pipeline3"), false);

        verify(pipelineRepository).saveSelectedPipelines(argThat(isAPipelineSelectionsInstanceWith(false, "pipelineX", "pipeline3")));
    }

    @Test
    public void shouldUpdateAlreadyPersistedSelection_WhenSecurityIsDisabled() {
        Date date = new DateTime(2000, 1, 1, 1, 1, 1, 1).toDate();
        when(clock.currentTime()).thenReturn(date);
        when(goConfigService.getAllPipelineConfigs()).thenReturn(Arrays.asList(pipelineConfig("pipeline1"), pipelineConfig("pipeline2"), pipelineConfig("pipelineX"), pipelineConfig("pipeline3")));
        PipelineSelections pipelineSelections = new PipelineSelections(Arrays.asList("pip1"));
        when(pipelineRepository.findPipelineSelectionsById("123")).thenReturn(pipelineSelections);
        List<String> newPipelines = Arrays.asList("pipeline1", "pipeline2");

        pipelineSelectionsService.persistSelectedPipelines("123", null, newPipelines, true);

        assertHasSelected(pipelineSelections, newPipelines);
        assertThat(pipelineSelections.lastUpdated(), is(date));
        verify(pipelineRepository).findPipelineSelectionsById("123");
        verify(pipelineRepository).saveSelectedPipelines(argThat(hasValues(Arrays.asList("pipeline1", "pipeline2"), Arrays.asList("pipelineX", "pipeline3"), clock.currentTime(), null)));
    }

    @Test
    public void shouldReturnPersistedPipelineSelectionsAgainstCookieId_WhenSecurityisDisabled() {
        PipelineSelections pipelineSelections = new PipelineSelections(Arrays.asList("pip1"));
        when(pipelineRepository.findPipelineSelectionsById("123")).thenReturn(pipelineSelections);
        assertThat(pipelineSelectionsService.getPersistedSelectedPipelines("123", null), is(pipelineSelections));
        assertThat(pipelineSelectionsService.getPersistedSelectedPipelines("", null), is(PipelineSelections.ALL));
        assertThat(pipelineSelectionsService.getPersistedSelectedPipelines("345", null), is(PipelineSelections.ALL));
    }

    @Test
    public void shouldReturnPersistedPipelineSelectionsAgainstUser_WhenSecurityIsEnabled() {
        User loser = getUser("loser", 10L);
        User newUser = getUser("new user", 20L);
        when(userDao.findUser("new user")).thenReturn(newUser);
        when(goConfigService.getAllPipelineConfigs()).thenReturn(Arrays.asList(pipelineConfig("pipeline1"), pipelineConfig("pipeline2"), pipelineConfig("pipelineX"), pipelineConfig("pipeline3")));
        when(goConfigService.isSecurityEnabled()).thenReturn(true);
        PipelineSelections pipelineSelections = new PipelineSelections(Arrays.asList("pip1"));

        when(pipelineRepository.findPipelineSelectionsByUserId(loser.getId())).thenReturn(pipelineSelections);

        assertThat(pipelineSelectionsService.getPersistedSelectedPipelines("1", loser.getId()), is(pipelineSelections));
        assertThat(pipelineSelectionsService.getPersistedSelectedPipelines("1", newUser.getId()), is(PipelineSelections.ALL));
    }

    @Test
    public void shouldReturnAllPipelineSelections_WhenSecurityIsEnabled_AndNoPersistedSelections() {
        User user = getUser("loser", 10L);
        User newUser = getUser("new user", 20L);
        when(userDao.findUser("new user")).thenReturn(newUser);
        when(goConfigService.getAllPipelineConfigs()).thenReturn(Arrays.asList(pipelineConfig("pipeline1"), pipelineConfig("pipeline2"), pipelineConfig("pipelineX"), pipelineConfig("pipeline3")));
        when(goConfigService.isSecurityEnabled()).thenReturn(true);

        when(pipelineRepository.findPipelineSelectionsByUserId(user.getId())).thenReturn(null);
        when(pipelineRepository.findPipelineSelectionsById("1")).thenReturn(null);

        assertThat(pipelineSelectionsService.getPersistedSelectedPipelines("1", newUser.getId()), is(PipelineSelections.ALL));
    }

    @Test
    public void shouldReturnPersistedPipelineSelectionsAgainstUserId_WhenSecurityIsEnabled_AndUserSelectionsDoesNotExist() {
        User user = getUser("loser", 10L);
        when(goConfigService.getAllPipelineConfigs()).thenReturn(Arrays.asList(pipelineConfig("pipeline1"), pipelineConfig("pipeline2"), pipelineConfig("pipelineX"), pipelineConfig("pipeline3")));
        when(goConfigService.isSecurityEnabled()).thenReturn(true);
        PipelineSelections pipelineSelectionsForCookie = new PipelineSelections(Arrays.asList("pipeline2"));

        when(pipelineRepository.findPipelineSelectionsByUserId(user.getId())).thenReturn(null);
        when(pipelineRepository.findPipelineSelectionsById("1")).thenReturn(pipelineSelectionsForCookie);

        assertThat(pipelineSelectionsService.getPersistedSelectedPipelines("1", user.getId()), is(PipelineSelections.ALL));
    }

    @Test
    public void shouldReturnSelectedPipelinesRegardlessOfPersistedPipelineSelections() throws Exception {
        User user = getUser("loser", 10L);
        when(goConfigService.getAllPipelineConfigs()).thenReturn(Arrays.asList(pipelineConfig("pipeline1"), pipelineConfig("pipeline2"), pipelineConfig("pipelineX"), pipelineConfig("pipeline3")));
        when(goConfigService.isSecurityEnabled()).thenReturn(true);
        PipelineSelections pipelineSelections = new PipelineSelections(Arrays.asList("pipeline2"));

        when(pipelineRepository.findPipelineSelectionsByUserId(user.getId())).thenReturn(pipelineSelections);

        assertThat(pipelineSelectionsService.getSelectedPipelines("1", user.getId()), is(pipelineSelections));
    }

    private PipelineConfig createPipelineConfig(String pipelineName, String stageName, String... buildNames) {
        PipelineConfig pipeline = new PipelineConfig(new CaseInsensitiveString(pipelineName), new MaterialConfigs());
        pipeline.add(new StageConfig(new CaseInsensitiveString(stageName), jobConfigs(buildNames)));
        return pipeline;
    }

    private JobConfigs jobConfigs(String... buildNames) {
        JobConfigs jobConfigs = new JobConfigs();
        for (String buildName : buildNames) {
            jobConfigs.add(new JobConfig(buildName));
        }
        return jobConfigs;
    }

    private User getUser(final String userName, long id) {
        long userId = id;
        User user = new User(userName);
        user.setId(userId);
        when(userDao.findUser(userName)).thenReturn(user);
        return user;
    }

    private Matcher<PipelineSelections> hasValues(final List<String> isVisible, final List<String> isNotVisible, final Date today, final Long userId) {
        return new BaseMatcher<PipelineSelections>() {
            public boolean matches(Object o) {
                PipelineSelections pipelineSelections = (PipelineSelections) o;
                assertHasSelected(pipelineSelections, isVisible);
                assertHasSelected(pipelineSelections, isNotVisible, false);
                assertThat(pipelineSelections.lastUpdated(), is(today));
                assertThat(pipelineSelections.userId(), is(userId));
                return true;
            }

            public void describeTo(Description description) {
            }
        };
    }

    private Matcher<PipelineSelections> isAPipelineSelectionsInstanceWith(final boolean isBlacklist, final String... pipelineSelectionsInInstance) {
        return new BaseMatcher<PipelineSelections>() {
            public boolean matches(Object o) {
                PipelineSelections pipelineSelections = (PipelineSelections) o;
                assertThat(pipelineSelections.isBlacklist(), is(isBlacklist));

                List<String> expectedSelectionsAsList = Arrays.asList(pipelineSelectionsInInstance);
                assertEquals(pipelineSelections.getSelections(), StringUtils.join(expectedSelectionsAsList, ","));

                return true;
            }

            public void describeTo(Description description) {
            }
        };
    }

    private void assertHasSelected(PipelineSelections pipelineSelections, List<String> pipelines) {
        assertHasSelected(pipelineSelections, pipelines, true);
    }

    private void assertHasSelected(PipelineSelections pipelineSelections, List<String> pipelines, boolean has) {
        String message = "Expected: " + pipelines + " to include " + pipelineSelections + ": (" + has + ").";
        for (String pipeline : pipelines) {
            assertThat(message + ". Failed to find: " + pipeline, pipelineSelections.includesPipeline(pipelineConfig(pipeline)), is(has));
        }
    }

    private void expectLoad(final CruiseConfig result) throws Exception {
        when(goConfigDao.load()).thenReturn(result);
    }

    private CruiseConfig unchangedConfig() {
        return configWith(createPipelineConfig(PIPELINE, STAGE, JOB));
    }
}