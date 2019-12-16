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
package com.thoughtworks.go.server.dao;

import com.opensymphony.oscache.base.Cache;
import com.rits.cloning.Cloner;
import com.thoughtworks.go.domain.Stage;
import com.thoughtworks.go.domain.StageIdentifier;
import com.thoughtworks.go.helper.StageMother;
import com.thoughtworks.go.presentation.pipelinehistory.StageHistoryEntry;
import com.thoughtworks.go.presentation.pipelinehistory.StageHistoryPage;
import com.thoughtworks.go.server.cache.GoCache;
import com.thoughtworks.go.server.domain.StageIdentity;
import com.thoughtworks.go.server.service.StubGoCache;
import com.thoughtworks.go.server.transaction.SqlMapClientTemplate;
import com.thoughtworks.go.server.transaction.TestTransactionSynchronizationManager;
import com.thoughtworks.go.server.transaction.TransactionSynchronizationManager;
import com.thoughtworks.go.server.transaction.TransactionTemplate;
import com.thoughtworks.go.server.util.Pagination;
import com.thoughtworks.go.util.ReflectionUtil;
import com.thoughtworks.go.util.SystemEnvironment;
import org.apache.ibatis.session.SqlSessionFactory;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.function.Supplier;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

/**
 * @understands: StageSqlMapDaoTest
 */
class StageSqlMapDaoTest {
    private StageSqlMapDao stageSqlMapDao;
    private GoCache goCache;
    private SqlMapClientTemplate sqlMapClientTemplate;
    private Cloner cloner;

    @BeforeEach
    void setUp() {
        goCache = new StubGoCache(new TestTransactionSynchronizationManager());
        sqlMapClientTemplate = mock(SqlMapClientTemplate.class);
        stageSqlMapDao = new StageSqlMapDao(mock(JobInstanceSqlMapDao.class), new Cache(true, false, false), mock(TransactionTemplate.class), mock(SqlSessionFactory.class), goCache,
                mock(TransactionSynchronizationManager.class), mock(SystemEnvironment.class), null);
        stageSqlMapDao.setSqlMapClientTemplate(sqlMapClientTemplate);
        cloner = mock(Cloner.class);
        ReflectionUtil.setField(stageSqlMapDao, "cloner", cloner);
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) {
                return invocationOnMock.getArguments()[0];
            }
        }).when(cloner).deepClone(anyObject());
    }

    @Test
    void findLatestStageInstancesShouldCacheResults() {
        List<StageIdentity> latestStages = Arrays.asList(new StageIdentity("p1", "s1", 10L), new StageIdentity("p2", "s2", 100L));
        when(sqlMapClientTemplate.queryForList("latestStageInstances")).thenReturn((List) latestStages);

        List<StageIdentity> firstStageInstances = stageSqlMapDao.findLatestStageInstances();

        List<StageIdentity> latestStageInstances = stageSqlMapDao.findLatestStageInstances();

        assertThat(firstStageInstances, is(latestStages));
        assertThat(latestStageInstances, is(latestStages));
        verify(sqlMapClientTemplate, times(1)).queryForList("latestStageInstances");
    }

    @Test
    void shouldRemoveLatestStageInstancesFromCache_OnStageChange() {
        when(sqlMapClientTemplate.queryForList("latestStageInstances")).thenReturn(Arrays.asList(new StageIdentity("p1", "s1", 10L), new StageIdentity("p2", "s2", 100L)));
        String cacheKey = stageSqlMapDao.cacheKeyForLatestStageInstances();

        List<StageIdentity> latestStageInstances = stageSqlMapDao.findLatestStageInstances();
        assertThat(goCache.get(cacheKey), is(latestStageInstances));

        stageSqlMapDao.stageStatusChanged(StageMother.custom("stage"));
        assertThat(goCache.get(cacheKey), is(nullValue()));
    }

    @Test
    void shouldLoadStageHistoryEntryForAStageRunAfterTheLatestRunThatIsRetrievedForStageHistory() {
        String pipelineName = "some_pipeline_name";
        String stageName = "some_stage_name";
        Supplier function = mock(Supplier.class);
        Pagination pagination = mock(Pagination.class);
        when(pagination.getCurrentPage()).thenReturn(3);
        when(pagination.getPageSize()).thenReturn(10);
        when(function.get()).thenReturn(pagination);
        StageSqlMapDao spy = spy(stageSqlMapDao);
        List<StageHistoryEntry> expectedStageHistoryEntriesList = mock(ArrayList.class);
        StageHistoryEntry topOfThisPage = mock(StageHistoryEntry.class);
        when(expectedStageHistoryEntriesList.get(0)).thenReturn(topOfThisPage);
        StageHistoryEntry bottomOfLastPage = mock(StageHistoryEntry.class);
        doReturn(expectedStageHistoryEntriesList).when(spy).findStages(pagination, pipelineName, stageName);
        doReturn(bottomOfLastPage).when(spy).findImmediateChronologicallyForwardStageHistoryEntry(topOfThisPage);

        StageHistoryPage stageHistoryPage = spy.findStageHistoryPage(pipelineName, stageName, function);

        assertThat(stageHistoryPage.getStages(), is(expectedStageHistoryEntriesList));
        assertThat(stageHistoryPage.getImmediateChronologicallyForwardStageHistoryEntry(), is(bottomOfLastPage));

        verify(spy, times(1)).findStages(pagination, pipelineName, stageName);
        verify(spy, times(1)).findImmediateChronologicallyForwardStageHistoryEntry(expectedStageHistoryEntriesList.get(0));
    }

    @Test
    void shouldLoadTheStageHistoryEntryNextInTimeFromAGivenStageHistoryEntry() {
        StageIdentifier stageIdentifier = mock(StageIdentifier.class);
        String pipelineName = "some_pipeline_name";
        String stageName = "stage_name";
        long pipelineId = 41l;
        when(stageIdentifier.getPipelineName()).thenReturn(pipelineName);
        when(stageIdentifier.getStageName()).thenReturn(stageName);

        StageHistoryEntry topOfThisPage = mock(StageHistoryEntry.class);
        StageHistoryEntry bottomOfPreviousPage = mock(StageHistoryEntry.class);
        when(topOfThisPage.getId()).thenReturn(pipelineId);
        when(topOfThisPage.getIdentifier()).thenReturn(stageIdentifier);
        HashMap args = new HashMap();
        args.put("pipelineName", pipelineName);
        args.put("stageName", stageName);
        args.put("id", pipelineId);
        args.put("limit", 1);
        when(sqlMapClientTemplate.queryForObject("findStageHistoryEntryBefore", args)).thenReturn(bottomOfPreviousPage);

        StageHistoryEntry actual = stageSqlMapDao.findImmediateChronologicallyForwardStageHistoryEntry(topOfThisPage);
        assertThat(actual, is(bottomOfPreviousPage));

        verify(stageIdentifier).getPipelineName();
        verify(stageIdentifier).getStageName();
        verify(topOfThisPage).getId();
        verify(topOfThisPage).getIdentifier();
        verify(sqlMapClientTemplate).queryForObject("findStageHistoryEntryBefore", args);
    }

    @Nested
    class CacheKeyForPipelineAndStage {
        @Test
        void shouldGenerateCacheKey() {
            Assertions.assertThat(stageSqlMapDao.cacheKeyForPipelineAndStage("foo", "bar_baz"))
                    .isEqualTo("com.thoughtworks.go.server.dao.StageSqlMapDao.$isStageActive.$foo.$bar_baz");
        }

        @Test
        void shouldGenerateADifferentCacheKeyWhenPartOfPipelineIsInterchangedWithStageName() {
            Assertions.assertThat(stageSqlMapDao.cacheKeyForPipelineAndStage("foo", "bar_baz"))
                    .isNotEqualTo(stageSqlMapDao.cacheKeyForPipelineAndStage("foo_bar", "baz"));

            Assertions.assertThat(stageSqlMapDao.cacheKeyForPipelineAndStage("foo", "bar-baz"))
                    .isNotEqualTo(stageSqlMapDao.cacheKeyForPipelineAndStage("foo-bar", "baz"));
        }
    }

    @Nested
    class CacheKeyForStageCount {
        @Test
        void shouldGenerateCacheKey() {
            Assertions.assertThat(stageSqlMapDao.cacheKeyForStageCount("foo", "bar_baz"))
                    .isEqualTo("com.thoughtworks.go.server.dao.StageSqlMapDao.$numberOfStages.$foo.$bar_baz");
        }

        @Test
        void shouldGenerateADifferentCacheKeyWhenPartOfPipelineIsInterchangedWithStageName() {
            Assertions.assertThat(stageSqlMapDao.cacheKeyForStageCount("foo", "bar_baz"))
                    .isNotEqualTo(stageSqlMapDao.cacheKeyForStageCount("foo_bar", "baz"));

            Assertions.assertThat(stageSqlMapDao.cacheKeyForStageCount("foo", "bar-baz"))
                    .isNotEqualTo(stageSqlMapDao.cacheKeyForStageCount("foo-bar", "baz"));
        }
    }

    @Nested
    class CacheKeyForListOfStageIdentifiers {
        @Test
        void shouldGenerateCacheKey() {
            final StageIdentifier stageIdentifier = new StageIdentifier("foo", 1, "bar_baz", "1");
            Assertions.assertThat(stageSqlMapDao.cacheKeyForListOfStageIdentifiers(stageIdentifier))
                    .isEqualTo("com.thoughtworks.go.server.dao.StageSqlMapDao.$stageRunIdentifier.$foo.$1.$bar_baz");
        }

        @Test
        void shouldGenerateADifferentCacheKeyWhenPartOfPipelineIsInterchangedWithStageName() {
            StageIdentifier identifierOne = new StageIdentifier("foo", 1, "bar_baz", "1");
            StageIdentifier identifierTwo = new StageIdentifier("foo_bar", 1, "bar", "1");
            Assertions.assertThat(stageSqlMapDao.cacheKeyForListOfStageIdentifiers(identifierOne))
                    .isNotEqualTo(stageSqlMapDao.cacheKeyForListOfStageIdentifiers(identifierTwo));

            identifierOne = new StageIdentifier("foo", 1, "bar-baz", "1");
            identifierTwo = new StageIdentifier("foo-bar", 1, "bar", "1");
            Assertions.assertThat(stageSqlMapDao.cacheKeyForListOfStageIdentifiers(identifierOne))
                    .isNotEqualTo(stageSqlMapDao.cacheKeyForListOfStageIdentifiers(identifierTwo));
        }
    }

    @Nested
    class CacheKeyForStageIdentifier {
        @Test
        void shouldGenerateCacheKey() {
            final StageIdentifier stageIdentifier = new StageIdentifier("foo", 1, "bar_baz", "1");
            Assertions.assertThat(stageSqlMapDao.cacheKeyForStageIdentifier(stageIdentifier))
                    .isEqualTo("com.thoughtworks.go.server.dao.StageSqlMapDao.$stageIdentifier.$foo.$1.$bar_baz.$1");
        }

        @Test
        void shouldGenerateADifferentCacheKeyWhenPartOfPipelineIsInterchangedWithStageName() {
            StageIdentifier identifierOne = new StageIdentifier("foo", 1, "bar_baz", "1");
            StageIdentifier identifierTwo = new StageIdentifier("foo_bar", 1, "bar", "1");
            Assertions.assertThat(stageSqlMapDao.cacheKeyForStageIdentifier(identifierOne))
                    .isNotEqualTo(stageSqlMapDao.cacheKeyForStageIdentifier(identifierTwo));

            identifierOne = new StageIdentifier("foo", 1, "bar-baz", "1");
            identifierTwo = new StageIdentifier("foo-bar", 1, "bar", "1");
            Assertions.assertThat(stageSqlMapDao.cacheKeyForStageIdentifier(identifierOne))
                    .isNotEqualTo(stageSqlMapDao.cacheKeyForStageIdentifier(identifierTwo));
        }
    }

    @Nested
    class CacheKeyForDetailedStageHistories {
        @Test
        void shouldGenerateCacheKey() {
            Assertions.assertThat(stageSqlMapDao.cacheKeyForDetailedStageHistories("foo", "bar_baz"))
                    .isEqualTo("com.thoughtworks.go.server.dao.StageSqlMapDao.$detailedStageHistories.$foo.$bar_baz");
        }

        @Test
        void shouldGenerateADifferentCacheKeyWhenPartOfPipelineIsInterchangedWithStageName() {
            Assertions.assertThat(stageSqlMapDao.cacheKeyForDetailedStageHistories("foo", "bar_baz"))
                    .isNotEqualTo(stageSqlMapDao.cacheKeyForDetailedStageHistories("foo_bar", "baz"));

            Assertions.assertThat(stageSqlMapDao.cacheKeyForDetailedStageHistories("foo", "bar-baz"))
                    .isNotEqualTo(stageSqlMapDao.cacheKeyForDetailedStageHistories("foo-bar", "baz"));
        }
    }

    @Nested
    class CacheKeyForStageHistoryViaCursor {
        @Test
        void shouldGenerateCacheKey() {
            Assertions.assertThat(stageSqlMapDao.cacheKeyForStageHistoryViaCursor("foo", "bar_baz"))
                    .isEqualTo("com.thoughtworks.go.server.dao.StageSqlMapDao.$findDetailedStageHistoryViaCursor.$foo.$bar_baz");
        }

        @Test
        void shouldGenerateADifferentCacheKeyWhenPartOfPipelineIsInterchangedWithStageName() {
            Assertions.assertThat(stageSqlMapDao.cacheKeyForStageHistoryViaCursor("foo", "bar_baz"))
                    .isNotEqualTo(stageSqlMapDao.cacheKeyForStageHistoryViaCursor("foo_bar", "baz"));

            Assertions.assertThat(stageSqlMapDao.cacheKeyForStageHistoryViaCursor("foo", "bar-baz"))
                    .isNotEqualTo(stageSqlMapDao.cacheKeyForStageHistoryViaCursor("foo-bar", "baz"));
        }
    }

    @Nested
    class CacheKeyForStageOffset {

        @Test
        void shouldGenerateCacheKey() {
            final Stage stage = new Stage();
            stage.setIdentifier(new StageIdentifier("up42", 1, "1", "Foo", "1"));

            Assertions.assertThat(stageSqlMapDao.cacheKeyForStageOffset(stage))
                    .isEqualTo("com.thoughtworks.go.server.dao.StageSqlMapDao.$stageOffsetMap.$up42.$Foo");
        }

        @Test
        void shouldGenerateADifferentCacheKeyWhenDifferentPipelinesHaveStageWithSameName() {
            final Stage stageOne = new Stage();
            final Stage stageTwo = new Stage();

            stageOne.setIdentifier(new StageIdentifier("Foo", 1, "1", "Bar_Baz", "1"));
            stageTwo.setIdentifier(new StageIdentifier("Foo_Bar", 1, "1", "Baz", "1"));

            Assertions.assertThat(stageSqlMapDao.cacheKeyForStageOffset(stageOne))
                    .isNotEqualTo(stageSqlMapDao.cacheKeyForStageOffset(stageTwo));

            stageOne.setIdentifier(new StageIdentifier("Foo", 1, "1", "Bar-Baz", "1"));
            stageTwo.setIdentifier(new StageIdentifier("Foo-Bar", 1, "1", "Baz", "1"));

            Assertions.assertThat(stageSqlMapDao.cacheKeyForStageOffset(stageOne))
                    .isNotEqualTo(stageSqlMapDao.cacheKeyForStageOffset(stageTwo));
        }
    }

    @Nested
    class CacheKeyForPipelineAndCounter {
        @Test
        void shouldGenerateCacheKey() {
            Assertions.assertThat(stageSqlMapDao.cacheKeyForPipelineAndCounter("foo", 1))
                    .isEqualTo("com.thoughtworks.go.server.dao.StageSqlMapDao.$allStagesOfPipelineInstance.$foo.$1");
        }

        @Test
        void shouldGenerateADifferentCacheKeyForDifferentPipelineName() {
            Assertions.assertThat(stageSqlMapDao.cacheKeyForPipelineAndCounter("foo", 1))
                    .isNotEqualTo(stageSqlMapDao.cacheKeyForPipelineAndCounter("bar", 1));
        }
    }

    @Nested
    class CacheKeyForLatestStageInstances {
        @Test
        void shouldGenerateCacheKey() {
            Assertions.assertThat(stageSqlMapDao.cacheKeyForLatestStageInstances())
                    .isEqualTo("com.thoughtworks.go.server.dao.StageSqlMapDao.$latestStageInstances");
        }
    }

    @Nested
    class CacheKeyForStageCountForGraph {
        @Test
        void shouldGenerateCacheKey() {
            Assertions.assertThat(stageSqlMapDao.cacheKeyForStageCountForGraph("foo", "bar_baz"))
                    .isEqualTo("com.thoughtworks.go.server.dao.StageSqlMapDao.$totalStageCountForChart.$foo.$bar_baz");
        }

        @Test
        void shouldGenerateADifferentCacheKeyWhenPartOfPipelineIsInterchangedWithStageName() {
            Assertions.assertThat(stageSqlMapDao.cacheKeyForStageCountForGraph("foo", "bar_baz"))
                    .isNotEqualTo(stageSqlMapDao.cacheKeyForStageCountForGraph("foo_bar", "baz"));

            Assertions.assertThat(stageSqlMapDao.cacheKeyForStageCountForGraph("foo", "bar-baz"))
                    .isNotEqualTo(stageSqlMapDao.cacheKeyForStageCountForGraph("foo-bar", "baz"));
        }
    }

    @Nested
    class CacheKeyForMostRecentId {
        @Test
        void shouldGenerateCacheKey() {
            Assertions.assertThat(stageSqlMapDao.cacheKeyForMostRecentId("foo", "bar_baz"))
                    .isEqualTo("com.thoughtworks.go.server.dao.StageSqlMapDao.$mostRecentId.$foo.$bar_baz");
        }

        @Test
        void shouldGenerateADifferentCacheKeyWhenPartOfPipelineIsInterchangedWithStageName() {
            Assertions.assertThat(stageSqlMapDao.cacheKeyForMostRecentId("foo", "bar_baz"))
                    .isNotEqualTo(stageSqlMapDao.cacheKeyForMostRecentId("foo_bar", "baz"));

            Assertions.assertThat(stageSqlMapDao.cacheKeyForMostRecentId("foo", "bar-baz"))
                    .isNotEqualTo(stageSqlMapDao.cacheKeyForMostRecentId("foo-bar", "baz"));
        }
    }

    @Nested
    class CacheKeyForStageHistories {
        @Test
        void shouldGenerateCacheKey() {
            Assertions.assertThat(stageSqlMapDao.cacheKeyForStageHistories("foo", "bar_baz"))
                    .isEqualTo("com.thoughtworks.go.server.dao.StageSqlMapDao.$stageHistories.$foo.$bar_baz");
        }

        @Test
        void shouldGenerateSameCacheKeyEvenIfPipelineAndStageIsInDifferentLetterCase() {
            Assertions.assertThat(stageSqlMapDao.cacheKeyForStageHistories("foo", "bar_baz"))
                    .isEqualTo("com.thoughtworks.go.server.dao.StageSqlMapDao.$stageHistories.$foo.$bar_baz");

            Assertions.assertThat(stageSqlMapDao.cacheKeyForStageHistories("FOO", "BAR_baz"))
                    .isEqualTo("com.thoughtworks.go.server.dao.StageSqlMapDao.$stageHistories.$foo.$bar_baz");
        }

        @Test
        void shouldGenerateADifferentCacheKeyWhenPartOfPipelineIsInterchangedWithStageName() {
            Assertions.assertThat(stageSqlMapDao.cacheKeyForStageHistories("foo", "bar_baz"))
                    .isNotEqualTo(stageSqlMapDao.cacheKeyForStageHistories("foo_bar", "baz"));

            Assertions.assertThat(stageSqlMapDao.cacheKeyForStageHistories("foo", "bar-baz"))
                    .isNotEqualTo(stageSqlMapDao.cacheKeyForStageHistories("foo-bar", "baz"));
        }
    }

    @Nested
    class CacheKeyForAllStageOfPipeline {
        @Test
        void shouldGenerateCacheKey() {
            Assertions.assertThat(stageSqlMapDao.cacheKeyForAllStageOfPipeline("foo", 1, "bar"))
                    .isEqualTo("com.thoughtworks.go.server.dao.StageSqlMapDao.$allStageOfPipeline.$foo.$1.$bar");
        }

        @Test
        void shouldGenerateSameCacheKeyEvenIfPipelineAndStageIsInDifferentLetterCase() {
            Assertions.assertThat(stageSqlMapDao.cacheKeyForAllStageOfPipeline("foo", 1, "bar"))
                    .isEqualTo("com.thoughtworks.go.server.dao.StageSqlMapDao.$allStageOfPipeline.$foo.$1.$bar");

            Assertions.assertThat(stageSqlMapDao.cacheKeyForAllStageOfPipeline("FOO", 1, "BAR"))
                    .isEqualTo("com.thoughtworks.go.server.dao.StageSqlMapDao.$allStageOfPipeline.$foo.$1.$bar");
        }

        @Test
        void shouldGenerateADifferentCacheKeyWhenPartOfPipelineIsInterchangedWithStageName() {
            Assertions.assertThat(stageSqlMapDao.cacheKeyForAllStageOfPipeline("foo", 1, "1_bar"))
                    .isNotEqualTo(stageSqlMapDao.cacheKeyForAllStageOfPipeline("foo_1", 1, "bar"));

            Assertions.assertThat(stageSqlMapDao.cacheKeyForAllStageOfPipeline("foo", 1, "1-bar"))
                    .isNotEqualTo(stageSqlMapDao.cacheKeyForAllStageOfPipeline("foo-1", 1, "bar"));
        }
    }

    @Nested
    class CacheKeyForStageById {
        @Test
        void shouldGenerateCacheKey() {
            Assertions.assertThat(stageSqlMapDao.cacheKeyForStageById(1L))
                    .isEqualTo("com.thoughtworks.go.server.dao.StageSqlMapDao.$stageById.$1");
        }
    }

    @Nested
    class MutexForStageHistory {
        @Test
        void shouldUniqueMutexForGivenCombination() {
            Assertions.assertThat(stageSqlMapDao.mutexForStageHistory("foo", "bar_baz"))
                    .isEqualTo("com.thoughtworks.go.server.dao.StageSqlMapDao_stageHistoryMutex_foo_<>_bar_baz");
        }

        @Test
        void shouldGenerateADifferentMutexWhenPartOfPipelineIsInterchangedWithStageName() {
            Assertions.assertThat(stageSqlMapDao.mutexForStageHistory("foo", "bar_baz"))
                    .isNotEqualTo(stageSqlMapDao.mutexForStageHistory("foo_bar", "baz"));

            Assertions.assertThat(stageSqlMapDao.mutexForStageHistory("foo", "bar-baz"))
                    .isNotEqualTo(stageSqlMapDao.mutexForStageHistory("foo-bar", "baz"));
        }
    }
}
