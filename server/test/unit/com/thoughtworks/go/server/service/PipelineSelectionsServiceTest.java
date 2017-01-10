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

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.server.domain.user.PipelineSelections;
import com.thoughtworks.go.server.persistence.PipelineRepository;
import com.thoughtworks.go.util.Clock;
import com.thoughtworks.go.util.ListUtil;
import com.thoughtworks.go.util.SystemEnvironment;
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

import static com.thoughtworks.go.helper.PipelineConfigMother.pipelineConfig;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.*;

public class PipelineSelectionsServiceTest {
    private GoConfigService goConfigService;
    private PipelineRepository pipelineRepository;
    private Clock clock;
    private PipelineSelectionsService pipelineSelectionsService;

    @Before
    public void setup() throws Exception {
        new SystemEnvironment().setProperty(SystemEnvironment.ENFORCE_SERVERID_MUTABILITY, "N");
        pipelineRepository = mock(PipelineRepository.class);
        this.clock = mock(Clock.class);
        goConfigService = mock(GoConfigService.class);
        pipelineSelectionsService = new PipelineSelectionsService(pipelineRepository, goConfigService, clock);
    }

    @Test
    public void shouldPersistPipelineSelections_WhenSecurityIsDisabled() {
        Date date = new DateTime(2000, 1, 1, 1, 1, 1, 1).toDate();
        when(clock.currentTime()).thenReturn(date);
        Matcher<PipelineSelections> pipelineSelectionsMatcher = hasValues(Arrays.asList("pipelineX", "pipeline3"), Arrays.asList("pipeline1", "pipeline2"), date, null);
        when(pipelineRepository.saveSelectedPipelines(argThat(pipelineSelectionsMatcher))).thenReturn(2L);
        when(goConfigService.getAllPipelineConfigs()).thenReturn(Arrays.asList(pipelineConfig("pipeline1"), pipelineConfig("pipeline2"), pipelineConfig("pipelineX"), pipelineConfig("pipeline3")));

        assertThat(pipelineSelectionsService.persistSelectedPipelines(null, Arrays.asList("pipelineX", "pipeline3"), true), is(2l));
        verify(pipelineRepository).saveSelectedPipelines(argThat(pipelineSelectionsMatcher));
    }

    @Test
    public void shouldPersistPipelineSelectionsAgainstUser_AlreadyHavingSelections() {
        Date date = new DateTime(2000, 1, 1, 1, 1, 1, 1).toDate();
        when(clock.currentTime()).thenReturn(date);
        long userId = 10;
        PipelineSelections pipelineSelections = new PipelineSelections(Arrays.asList("pipeline2"), new Date(), userId, true);
        when(pipelineRepository.findPipelineSelectionsByUserId(userId)).thenReturn(pipelineSelections);
        when(pipelineRepository.saveSelectedPipelines(pipelineSelections)).thenReturn(2L);
        when(goConfigService.getAllPipelineConfigs()).thenReturn(Arrays.asList(pipelineConfig("pipeline1"), pipelineConfig("pipeline2"), pipelineConfig("pipelineX"), pipelineConfig("pipeline3")));

        long pipelineSelectionId = pipelineSelectionsService.persistSelectedPipelines(userId, Arrays.asList("pipelineX", "pipeline3"), true);

        assertThat(pipelineSelections.getSelections(), is("pipeline1,pipeline2"));
        assertThat(pipelineSelectionId, is(2l));
        verify(pipelineRepository).saveSelectedPipelines(pipelineSelections);
        verify(pipelineRepository).findPipelineSelectionsByUserId(userId);
    }

    @Test
    public void shouldPersistPipelineSelectionsAgainstUser_WhenUserHasNoSelections() {
        Date date = new DateTime(2000, 1, 1, 1, 1, 1, 1).toDate();
        when(clock.currentTime()).thenReturn(date);
        Long userId = 10L;
        Matcher<PipelineSelections> pipelineSelectionsMatcher = hasValues(Arrays.asList("pipelineX", "pipeline3"), Arrays.asList("pipeline1", "pipeline2"), date, userId);
        when(pipelineRepository.findPipelineSelectionsByUserId(userId)).thenReturn(null);
        when(pipelineRepository.saveSelectedPipelines(argThat(pipelineSelectionsMatcher))).thenReturn(2L);
        when(goConfigService.getAllPipelineConfigs()).thenReturn(Arrays.asList(pipelineConfig("pipeline1"), pipelineConfig("pipeline2"), pipelineConfig("pipelineX"), pipelineConfig("pipeline3")));

        long pipelineSelectionsId = pipelineSelectionsService.persistSelectedPipelines(userId, Arrays.asList("pipelineX", "pipeline3"), true);

        assertThat(pipelineSelectionsId, is(2l));
        verify(pipelineRepository).saveSelectedPipelines(argThat(pipelineSelectionsMatcher));
        verify(pipelineRepository).findPipelineSelectionsByUserId(userId);
    }

    @Test
    public void shouldPersistPipelineSelectionsShouldRemovePipelinesFromSelectedGroups() {
        when(goConfigService.getAllPipelineConfigs()).thenReturn(Arrays.asList(pipelineConfig("pipeline1"), pipelineConfig("pipeline2"), pipelineConfig("pipelineX"), pipelineConfig("pipeline3"), pipelineConfig("pipeline4")));
        pipelineSelectionsService.persistSelectedPipelines(null, Arrays.asList("pipeline1", "pipeline2", "pipeline3"), true);
        verify(pipelineRepository).saveSelectedPipelines(argThat(hasValues(Arrays.asList("pipeline1", "pipeline2", "pipeline3"), Arrays.asList("pipelineX", "pipeline4"), clock.currentTime(), null)));
    }

    @Test
    public void shouldPersistInvertedListOfPipelineSelections_WhenBlacklistIsSelected() {
        Date date = new DateTime(2000, 1, 1, 1, 1, 1, 1).toDate();
        Long userId = 10L;
        PipelineSelections blacklistPipelineSelections = new PipelineSelections(new ArrayList<String>(), date, userId, false);
        when(clock.currentTime()).thenReturn(date);
        when(pipelineRepository.findPipelineSelectionsByUserId(userId)).thenReturn(blacklistPipelineSelections);
        when(goConfigService.getAllPipelineConfigs()).thenReturn(Arrays.asList(pipelineConfig("pipeline1"), pipelineConfig("pipeline2"), pipelineConfig("pipelineX"), pipelineConfig("pipeline3")));

        pipelineSelectionsService.persistSelectedPipelines(userId, Arrays.asList("pipelineX", "pipeline3"), true);

        verify(pipelineRepository).saveSelectedPipelines(argThat(isAPipelineSelectionsInstanceWith(true, "pipeline1", "pipeline2")));
    }

    @Test
    public void shouldPersistNonInvertedListOfPipelineSelections_WhenWhitelistIsSelected() {
        Date date = new DateTime(2000, 1, 1, 1, 1, 1, 1).toDate();
        when(clock.currentTime()).thenReturn(date);

        Long userId = 10L;
        PipelineSelections whitelistPipelineSelections = new PipelineSelections(new ArrayList<>(), date, userId, true);
        when(pipelineRepository.findPipelineSelectionsByUserId(userId)).thenReturn(whitelistPipelineSelections);

        pipelineSelectionsService.persistSelectedPipelines(userId, Arrays.asList("pipelineX", "pipeline3"), false);

        verify(pipelineRepository).saveSelectedPipelines(argThat(isAPipelineSelectionsInstanceWith(false, "pipelineX", "pipeline3")));
    }

    @Test
    public void shouldUpdateAlreadyPersistedSelection_WhenSecurityIsDisabled() {
        Date date = new DateTime(2000, 1, 1, 1, 1, 1, 1).toDate();
        when(clock.currentTime()).thenReturn(date);
        when(goConfigService.getAllPipelineConfigs()).thenReturn(Arrays.asList(pipelineConfig("pipeline1"), pipelineConfig("pipeline2"), pipelineConfig("pipelineX"), pipelineConfig("pipeline3")));
        PipelineSelections pipelineSelections = new PipelineSelections(Arrays.asList("pip1"));
        when(pipelineRepository.findPipelineSelectionsByUserId(null)).thenReturn(pipelineSelections);
        List<String> newPipelines = Arrays.asList("pipeline1", "pipeline2");

        pipelineSelectionsService.persistSelectedPipelines(null, newPipelines, true);

        assertHasSelected(pipelineSelections, newPipelines);
        assertThat(pipelineSelections.lastUpdated(), is(date));
        verify(pipelineRepository).findPipelineSelectionsByUserId(null);
        verify(pipelineRepository).saveSelectedPipelines(argThat(hasValues(Arrays.asList("pipeline1", "pipeline2"), Arrays.asList("pipelineX", "pipeline3"), clock.currentTime(), null)));
    }

    @Test
    public void shouldReturnLatestPersistedPipelineSelectionsAgainstNullUser_WhenSecurityisDisabled() {
        PipelineSelections pipelineSelections = new PipelineSelections(Arrays.asList("pip1"));
        when(pipelineRepository.findPipelineSelectionsByUserId(null)).thenReturn(pipelineSelections);
        assertThat(pipelineSelectionsService.getSelectedPipelines(null), is(pipelineSelections));
    }

    @Test
    public void shouldReturnAllPipelinesAgainstNullUser_WhenSecurityisDisabledAndNoSelectionsArePersistedForNullUser() {
        when(pipelineRepository.findPipelineSelectionsByUserId(null)).thenReturn(null);
        assertThat(pipelineSelectionsService.getSelectedPipelines(null), is(PipelineSelections.ALL));
    }

    @Test
    public void shouldReturnPersistedPipelineSelectionsAgainstUser_WhenSecurityIsEnabled() {
        PipelineSelections pipelineSelections = new PipelineSelections(Arrays.asList("pip1"));

        long loser1Id = 10L;
        when(pipelineRepository.findPipelineSelectionsByUserId(loser1Id)).thenReturn(pipelineSelections);

        assertThat(pipelineSelectionsService.getSelectedPipelines(loser1Id), is(pipelineSelections));
        assertThat(pipelineSelectionsService.getSelectedPipelines(20L), is(PipelineSelections.ALL));
    }

    @Test
    public void shouldReturnAllPipelineSelections_WhenSecurityIsEnabled_AndNoPersistedSelections() {
        when(pipelineRepository.findPipelineSelectionsByUserId(10L)).thenReturn(null);

        assertThat(pipelineSelectionsService.getSelectedPipelines(20L), is(PipelineSelections.ALL));
    }

    @Test
    public void shouldNotUpdatePipelineSelectionsWhenTheUserIsAnonymousAndHasNeverSelectedPipelines() {
        pipelineSelectionsService.updateUserPipelineSelections(null, new CaseInsensitiveString("pipelineNew"));

        verify(pipelineRepository, times(0)).saveSelectedPipelines(argThat(Matchers.any(PipelineSelections.class)));
    }

    @Test
    public void shouldNotUpdatePipelineSelectionsWhenTheUserIsAnonymousAndHasSelectedPipelines_WithBlacklist() {
        when(pipelineRepository.findPipelineSelectionsByUserId(null)).thenReturn(new PipelineSelections(Arrays.asList("pipeline1", "pipeline2"), null, null, true));

        pipelineSelectionsService.updateUserPipelineSelections(null, new CaseInsensitiveString("pipelineNew"));

        verify(pipelineRepository).findPipelineSelectionsByUserId(null);
        verify(pipelineRepository, times(0)).saveSelectedPipelines(argThat(Matchers.any(PipelineSelections.class)));
    }

    @Test
    public void shouldUpdatePipelineSelectionsWhenTheUserIsAnonymousAndHasSelectedPipelines_WithWhitelist() {
        when(pipelineRepository.findPipelineSelectionsByUserId(null)).thenReturn(new PipelineSelections(Arrays.asList("pipeline1", "pipeline2"), null, null, false));

        pipelineSelectionsService.updateUserPipelineSelections(null, new CaseInsensitiveString("pipelineNew"));

        verify(pipelineRepository).findPipelineSelectionsByUserId(null);
        verify(pipelineRepository, times(1)).saveSelectedPipelines(argThat(isAPipelineSelectionsInstanceWith(false, "pipeline1", "pipeline2", "pipelineNew")));
    }

    @Test
    public void shouldNotUpdatePipelineSelectionsWhenTheUserIsLoggedIn_WithBlacklist() {
        when(pipelineRepository.findPipelineSelectionsByUserId(1L)).thenReturn(new PipelineSelections(Arrays.asList("pipeline1", "pipeline2"), null, null, true));

        pipelineSelectionsService.updateUserPipelineSelections(1L, new CaseInsensitiveString("pipelineNew"));

        verify(pipelineRepository).findPipelineSelectionsByUserId(1L);
        verify(pipelineRepository, times(0)).saveSelectedPipelines(argThat(Matchers.any(PipelineSelections.class)));
    }

    @Test
    public void shouldUpdatePipelineSelectionsWhenTheUserIsLoggedIn_WithWhitelist() {
        when(pipelineRepository.findPipelineSelectionsByUserId(1L)).thenReturn(new PipelineSelections(Arrays.asList("pipeline1", "pipeline2"), null, null, false));

        pipelineSelectionsService.updateUserPipelineSelections(1L, new CaseInsensitiveString("pipelineNew"));

        verify(pipelineRepository).findPipelineSelectionsByUserId(1L);
        verify(pipelineRepository, times(1)).saveSelectedPipelines(argThat(isAPipelineSelectionsInstanceWith(false, "pipeline1", "pipeline2", "pipelineNew")));
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

    private void assertHasSelected(PipelineSelections pipelineSelections, List<String> pipelines) {
        assertHasSelected(pipelineSelections, pipelines, true);
    }

    private void assertHasSelected(PipelineSelections pipelineSelections, List<String> pipelines, boolean has) {
        String message = "Expected: " + pipelines + " to include " + pipelineSelections + ": (" + has + ").";
        for (String pipeline : pipelines) {
            assertThat(message + ". Failed to find: " + pipeline, pipelineSelections.includesPipeline(pipelineConfig(pipeline)), is(has));
        }
    }

    private Matcher<PipelineSelections> isAPipelineSelectionsInstanceWith(final boolean isBlacklist, final String... pipelineSelectionsInInstance) {
        return new BaseMatcher<PipelineSelections>() {
            public boolean matches(Object o) {
                PipelineSelections pipelineSelections = (PipelineSelections) o;
                assertThat(pipelineSelections.isBlacklist(), is(isBlacklist));

                List<String> expectedSelectionsAsList = Arrays.asList(pipelineSelectionsInInstance);
                assertEquals(pipelineSelections.getSelections(), ListUtil.join(expectedSelectionsAsList, ","));

                return true;
            }

            public void describeTo(Description description) {
            }
        };
    }

}