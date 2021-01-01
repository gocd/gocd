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
package com.thoughtworks.go.server.dao;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.GoConfigDao;
import com.thoughtworks.go.domain.Pipeline;
import com.thoughtworks.go.domain.buildcause.BuildCause;
import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.helper.GoConfigMother;
import com.thoughtworks.go.helper.ModificationsMother;
import com.thoughtworks.go.presentation.pipelinehistory.*;
import com.thoughtworks.go.server.cache.GoCache;
import com.thoughtworks.go.server.database.Database;
import com.thoughtworks.go.server.persistence.MaterialRepository;
import com.thoughtworks.go.server.transaction.SqlMapClientTemplate;
import com.thoughtworks.go.util.SystemEnvironment;
import com.thoughtworks.go.util.TimeProvider;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.Random;

import static com.thoughtworks.go.helper.ModificationsMother.*;
import static com.thoughtworks.go.util.DataStructureUtils.m;
import static com.thoughtworks.go.util.IBatisUtil.arguments;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class PipelineSqlMapDaoTest {
    private PipelineSqlMapDao pipelineSqlMapDao;
    private GoCache goCache;
    private SqlMapClientTemplate sqlMapClientTemplate;
    private MaterialRepository materialRepository;
    private GoConfigDao configFileDao;
    private TimeProvider timeProvider;

    @BeforeEach
    void setUp() throws Exception {
        goCache = mock(GoCache.class);
        sqlMapClientTemplate = mock(SqlMapClientTemplate.class);
        materialRepository = mock(MaterialRepository.class);
        configFileDao = mock(GoConfigDao.class);
        timeProvider = mock(TimeProvider.class);
        pipelineSqlMapDao = new PipelineSqlMapDao(null, materialRepository, goCache, null, null, null, null, null, configFileDao, null, timeProvider);
        pipelineSqlMapDao.setSqlMapClientTemplate(sqlMapClientTemplate);
    }

    @Test
    void shouldLoadPipelineHistoryFromCacheWhenQueriedViaNameAndCounter() throws Exception {
        String pipelineName = "wholetthedogsout";
        int pipelineCounter = 42;
        PipelineInstanceModel expected = mock(PipelineInstanceModel.class);
        when(goCache.get(anyString())).thenReturn(expected);
        when(expected.getBuildCause()).thenReturn(mock(BuildCause.class));
        when(expected.getApprovedBy()).thenReturn("some-user");

        PipelineInstanceModel reFetch = pipelineSqlMapDao.findPipelineHistoryByNameAndCounter(pipelineName, pipelineCounter); //returned from cache

        assertThat(reFetch).isEqualTo(expected);
        verify(goCache).get(anyString());
    }

    @Test
    void shouldPrimePipelineHistoryToCacheWhenQueriedViaNameAndCounter() throws Exception {
        String pipelineName = "wholetthedogsout";
        int pipelineCounter = 42;
        Map<String, Object> map = arguments("pipelineName", pipelineName).and("pipelineCounter", pipelineCounter).asMap();
        PipelineInstanceModel expected = mock(PipelineInstanceModel.class);
        when(sqlMapClientTemplate.queryForObject("getPipelineHistoryByNameAndCounter", map)).thenReturn(expected);
        when(expected.getId()).thenReturn(1111L);
        when(expected.getBuildCause()).thenReturn(mock(BuildCause.class));
        when(expected.getApprovedBy()).thenReturn("some-user");
        when(materialRepository.findMaterialRevisionsForPipeline(expected.getId())).thenReturn(null);

        PipelineInstanceModel primed = pipelineSqlMapDao.findPipelineHistoryByNameAndCounter(pipelineName, pipelineCounter);//prime cache

        assertThat(primed).isEqualTo(expected);

        verify(sqlMapClientTemplate, times(1)).queryForObject("getPipelineHistoryByNameAndCounter", map);
        verify(goCache, times(1)).put(anyString(), eq(expected));
        verify(goCache, times(2)).get(anyString());
    }

    @Test
    void shouldUpdateCommentAndRemoveItFromPipelineHistoryCache() throws Exception {
        String pipelineName = "wholetthedogsout";
        int pipelineCounter = 42;
        String comment = "This song is from the 90s.";
        Map<String, Object> args = arguments("pipelineName", pipelineName).and("pipelineCounter", pipelineCounter).and("comment", comment).asMap();

        Pipeline expected = mock(Pipeline.class);
        when(sqlMapClientTemplate.queryForObject("findPipelineByNameAndCounter", arguments("name", pipelineName).and("counter", pipelineCounter).asMap())).thenReturn(expected);
        when(expected.getId()).thenReturn(102413L);

        pipelineSqlMapDao.updateComment(pipelineName, pipelineCounter, comment);

        verify(sqlMapClientTemplate, times(1)).update("updatePipelineComment", args);
        verify(goCache, times(1)).remove("com.thoughtworks.go.server.dao.PipelineSqlMapDao.$pipelineHistory.$102413");
    }

    @Test
    void shouldGetLatestRevisionFromOrderedLists() {
        PipelineSqlMapDao pipelineSqlMapDao = new PipelineSqlMapDao(null, null, null, null, null, null, null, new SystemEnvironment(), mock(GoConfigDao.class), mock(Database.class), timeProvider);
        ArrayList list1 = new ArrayList();
        ArrayList list2 = new ArrayList();
        assertThat(pipelineSqlMapDao.getLatestRevisionFromOrderedLists(list1, list2)).isEqualTo((String) null);
        Modification modification1 = new Modification(MOD_USER, MOD_COMMENT, EMAIL_ADDRESS,
                YESTERDAY_CHECKIN, ModificationsMother.nextRevision());
        list1.add(modification1);
        assertThat(pipelineSqlMapDao.getLatestRevisionFromOrderedLists(list1, list2)).isEqualTo(ModificationsMother.currentRevision());
        Modification modification2 = new Modification(MOD_USER_COMMITTER, MOD_COMMENT_2, EMAIL_ADDRESS,
                TODAY_CHECKIN, ModificationsMother.nextRevision());
        list2.add(modification2);
        assertThat(pipelineSqlMapDao.getLatestRevisionFromOrderedLists(list1, list2)).isEqualTo(ModificationsMother.currentRevision());
    }

    @Test
    void loadHistoryByIds_shouldLoadHistoryByIdWhenOnlyASingleIdIsNeedeSoThatItUsesTheExistingCacheForEnvironmentsPage() throws Exception {
        SqlMapClientTemplate mockTemplate = mock(SqlMapClientTemplate.class);
        when(mockTemplate.queryForList(eq("getPipelineRange"), any())).thenReturn(Arrays.asList(2L));
        pipelineSqlMapDao.setSqlMapClientTemplate(mockTemplate);
        pipelineSqlMapDao.loadHistory("pipelineName", 1, 0);
        verify(mockTemplate, never()).queryForList(eq("getPipelineHistoryByName"), any());
        verify(mockTemplate, times(1)).queryForList(eq("getPipelineRange"), any());
    }

    @Test
    void shouldGetAnEmptyListOfPIMsWhenActivePipelinesListDoesNotHavePIMsForRequestedPipeline() throws Exception {
        String pipelineName = "pipeline-with-no-active-instances";

        when(configFileDao.load()).thenReturn(GoConfigMother.configWithPipelines(pipelineName));
        when(sqlMapClientTemplate.queryForList("allActivePipelines")).thenReturn(new ArrayList<>());

        PipelineInstanceModels models = pipelineSqlMapDao.loadActivePipelineInstancesFor(new CaseInsensitiveString(pipelineName));

        assertThat(models.isEmpty()).isTrue();
    }

    @Test
    void shouldGetAnListOfPIMsForPipelineWhenActivePipelinesListHasPIMsForRequestedPipeline() throws Exception {
        String p1 = "pipeline-with-active-instances";
        String p2 = "pipeline-with-no-active-instances";

        PipelineInstanceModel pimForP1_1 = pimFor(p1, 1);
        PipelineInstanceModel pimForP1_2 = pimFor(p1, 2);

        when(configFileDao.load()).thenReturn(GoConfigMother.configWithPipelines(p1, p2));
        when(sqlMapClientTemplate.queryForList("allActivePipelines")).thenReturn(asList(pimForP1_1, pimForP1_2, pimFor(p2, 1), pimFor(p2, 2)));
        when(sqlMapClientTemplate.queryForObject("getPipelineHistoryById", m("id", pimForP1_1.getId()))).thenReturn(pimForP1_1);
        when(sqlMapClientTemplate.queryForObject("getPipelineHistoryById", m("id", pimForP1_2.getId()))).thenReturn(pimForP1_2);

        PipelineInstanceModels models = pipelineSqlMapDao.loadActivePipelineInstancesFor(new CaseInsensitiveString(p1));

        assertThat(models.size()).isEqualTo(2);

        assertThat(pimForP1_1.getName()).isEqualTo(p1);
        assertThat(pimForP1_1.getCounter()).isEqualTo(1);

        assertThat(pimForP1_2.getName()).isEqualTo(p1);
        assertThat(pimForP1_2.getCounter()).isEqualTo(2);

        verify(sqlMapClientTemplate).queryForList("allActivePipelines");
        verify(sqlMapClientTemplate).queryForObject("getPipelineHistoryById", m("id", pimForP1_1.getId()));
        verify(sqlMapClientTemplate).queryForObject("getPipelineHistoryById", m("id", pimForP1_2.getId()));
        verifyNoMoreInteractions(sqlMapClientTemplate); /* Should not have loaded history for the other pipeline. */
    }

    private PipelineInstanceModel pimFor(String p1, int counter) {
        StageInstanceModels models = new StageInstanceModels();
        models.add(new StageInstanceModel("stage", "1", new JobHistory()));
        PipelineInstanceModel model = new PipelineInstanceModel(p1, counter, String.valueOf(counter), BuildCause.createManualForced(), models);
        model.setId(new Random().nextLong());
        return model;
    }

    @Nested
    class CacheKeyForBuildCauseByNameAndCounter {
        @Test
        void shouldGenerateCacheKey() {
            Assertions.assertThat(pipelineSqlMapDao.cacheKeyForBuildCauseByNameAndCounter("foo", 1))
                    .isEqualTo("com.thoughtworks.go.server.dao.PipelineSqlMapDao.$buildCauseByNameAndCounter.$foo.$1");
        }
    }

    @Nested
    class CacheKeyForPipelineHistoryByNameAndCounter {
        @Test
        void shouldGenerateCacheKey() {
            Assertions.assertThat(pipelineSqlMapDao.cacheKeyForPipelineHistoryByNameAndCounter("foo", 1))
                    .isEqualTo("com.thoughtworks.go.server.dao.PipelineSqlMapDao.$cacheKeyForPipelineHistoryByName.$foo.$AndCounter.$1");
        }
    }

    @Nested
    class CacheKeyForLatestPipelineIdByPipelineName {
        @Test
        void shouldGenerateCacheKey() {
            Assertions.assertThat(pipelineSqlMapDao.cacheKeyForLatestPipelineIdByPipelineName("foo"))
                    .isEqualTo("com.thoughtworks.go.server.dao.PipelineSqlMapDao.$latestPipelineIdByPipelineName.$foo");
        }
    }

    @Nested
    class CacheKeyForPauseState {
        @Test
        void shouldGenerateCacheKey() {
            Assertions.assertThat(pipelineSqlMapDao.cacheKeyForPauseState("foo"))
                    .isEqualTo("com.thoughtworks.go.server.dao.PipelineSqlMapDao.$cacheKeyForPauseState.$foo");
        }
    }

    @Nested
    class cacheKeyForLatestPassedStage {
        @Test
        void shouldGenerateCacheKey() {
            Assertions.assertThat(pipelineSqlMapDao.cacheKeyForLatestPassedStage(1, "stage_1"))
                    .isEqualTo("com.thoughtworks.go.server.dao.PipelineSqlMapDao.$cacheKeyForlatestPassedStage.$1.$stage_1");
        }
    }

    @Nested
    class CacheKeyForPipelineInstancesTriggeredWithDependencyMaterial {
        @Test
        void shouldGenerateCacheKey() {
            Assertions.assertThat(pipelineSqlMapDao.cacheKeyForPipelineInstancesTriggeredWithDependencyMaterial("Foo", "Bar", 1))
                    .isEqualTo("com.thoughtworks.go.server.dao.PipelineSqlMapDao.$cacheKeyForPipelineInstancesWithDependencyMaterial.$foo.$bar.$1");
        }

        @Test
        void shouldGenerateADifferentCacheKeyWhenPartOfPipelineIsInterchangedWithStageName() {
            Assertions.assertThat(pipelineSqlMapDao.cacheKeyForPipelineInstancesTriggeredWithDependencyMaterial("foo", "bar_baz", 1))
                    .isNotEqualTo(pipelineSqlMapDao.cacheKeyForPipelineInstancesTriggeredWithDependencyMaterial("foo_bar", "baz", 1));

            Assertions.assertThat(pipelineSqlMapDao.cacheKeyForPipelineInstancesTriggeredWithDependencyMaterial("foo", "bar-baz", 1))
                    .isNotEqualTo(pipelineSqlMapDao.cacheKeyForPipelineInstancesTriggeredWithDependencyMaterial("foo-bar", "baz", 1));
        }
    }

    @Nested
    class CacheKeyForPipelineInstancesTriggeredWithDependencyMaterialWithRevision {
        @Test
        void shouldGenerateCacheKey() {
            Assertions.assertThat(pipelineSqlMapDao.cacheKeyForPipelineInstancesTriggeredWithDependencyMaterial("Foo", "finger-print", "1c71670ed"))
                    .isEqualTo("com.thoughtworks.go.server.dao.PipelineSqlMapDao.$cacheKeyForPipelineInstancesWithDependencyMaterial.$foo.$finger-print.$1c71670ed");
        }
    }

    @Nested
    class LatestSuccessfulStageCacheKey {
        @Test
        void shouldGenerateCacheKey() {
            Assertions.assertThat(pipelineSqlMapDao.latestSuccessfulStageCacheKey("Foo", "Bar"))
                    .isEqualTo("com.thoughtworks.go.server.dao.PipelineSqlMapDao.$latestSuccessfulStage.$Foo.$Bar");
        }

        @Test
        void shouldGenerateADifferentCacheKeyWhenPartOfPipelineIsInterchangedWithStageName() {
            Assertions.assertThat(pipelineSqlMapDao.latestSuccessfulStageCacheKey("foo", "bar-baz"))
                    .isNotEqualTo(pipelineSqlMapDao.latestSuccessfulStageCacheKey("foo-bar", "baz"));

            Assertions.assertThat(pipelineSqlMapDao.latestSuccessfulStageCacheKey("foo", "bar_baz"))
                    .isNotEqualTo(pipelineSqlMapDao.latestSuccessfulStageCacheKey("foo_bar", "baz"));
        }
    }

    @Nested
    class ActivePipelinesCacheKey {
        @Test
        void shouldGenerateCacheKey() {
            Assertions.assertThat(pipelineSqlMapDao.activePipelinesCacheKey())
                    .isEqualTo("com.thoughtworks.go.server.dao.PipelineSqlMapDao.$activePipelines");
        }
    }

    @Nested
    class PipelineHistoryCacheKey {
        @Test
        void shouldGenerateCacheKey() {
            Assertions.assertThat(pipelineSqlMapDao.pipelineHistoryCacheKey(1L))
                    .isEqualTo("com.thoughtworks.go.server.dao.PipelineSqlMapDao.$pipelineHistory.$1");
        }
    }
}
