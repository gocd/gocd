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

package com.thoughtworks.go.server.dao;

import com.ibatis.sqlmap.client.SqlMapClient;
import com.opensymphony.oscache.base.Cache;
import com.rits.cloning.Cloner;
import com.thoughtworks.go.domain.Stage;
import com.thoughtworks.go.domain.StageConfigIdentifier;
import com.thoughtworks.go.domain.StageIdentifier;
import com.thoughtworks.go.helper.StageMother;
import com.thoughtworks.go.presentation.pipelinehistory.StageHistoryEntry;
import com.thoughtworks.go.presentation.pipelinehistory.StageHistoryPage;
import com.thoughtworks.go.server.cache.GoCache;
import com.thoughtworks.go.server.domain.StageIdentity;
import com.thoughtworks.go.server.service.StubGoCache;
import com.thoughtworks.go.server.transaction.TestTransactionSynchronizationManager;
import com.thoughtworks.go.server.transaction.TransactionSynchronizationManager;
import com.thoughtworks.go.server.transaction.TransactionTemplate;
import com.thoughtworks.go.server.util.Pagination;
import com.thoughtworks.go.util.FuncVarArg;
import com.thoughtworks.go.util.ReflectionUtil;
import com.thoughtworks.go.util.SystemEnvironment;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.orm.ibatis.SqlMapClientTemplate;

import java.sql.SQLException;
import java.util.*;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.*;

/**
 * @understands: StageSqlMapDaoTest
 */
public class StageSqlMapDaoTest {

    private StageSqlMapDao stageSqlMapDao;
    private GoCache goCache;
    private SqlMapClientTemplate sqlMapClientTemplate;
    private Cloner cloner;

    @Before
    public void setUp() {
        goCache = new StubGoCache(new TestTransactionSynchronizationManager());
        sqlMapClientTemplate = mock(SqlMapClientTemplate.class);
        stageSqlMapDao = new StageSqlMapDao(mock(JobInstanceSqlMapDao.class), new Cache(true, false, false), mock(TransactionTemplate.class), mock(SqlMapClient.class), goCache,
                mock(TransactionSynchronizationManager.class), mock(SystemEnvironment.class), null);
        stageSqlMapDao.setSqlMapClientTemplate(sqlMapClientTemplate);
        cloner = mock(Cloner.class);
        ReflectionUtil.setField(stageSqlMapDao, "cloner", cloner);
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                return invocationOnMock.getArguments()[0];
            }
        }).when(cloner).deepClone(anyObject());
    }

    @Test
    public void findLatestStageInstancesShouldCacheResults() throws SQLException {
        List<StageIdentity> latestStages = Arrays.asList(new StageIdentity("p1", "s1", 10L), new StageIdentity("p2", "s2", 100L));
        when(sqlMapClientTemplate.queryForList("latestStageInstances")).thenReturn(latestStages);

        List<StageIdentity> firstStageInstances = stageSqlMapDao.findLatestStageInstances();

        List<StageIdentity> latestStageInstances = stageSqlMapDao.findLatestStageInstances();

        assertThat(firstStageInstances, is(latestStages));
        assertThat(latestStageInstances, is(latestStages));
        verify(sqlMapClientTemplate, times(1)).queryForList("latestStageInstances");
    }

    @Test
    public void shouldRemoveLatestStageInstancesFromCache_OnStageChange() {
        when(sqlMapClientTemplate.queryForList("latestStageInstances")).thenReturn(Arrays.asList(new StageIdentity("p1", "s1", 10L), new StageIdentity("p2", "s2", 100L)));
        String cacheKey = stageSqlMapDao.cacheKeyForLatestStageInstances();

        List<StageIdentity> latestStageInstances = stageSqlMapDao.findLatestStageInstances();
        assertThat((List<StageIdentity>) goCache.get(cacheKey), is(latestStageInstances));

        stageSqlMapDao.stageStatusChanged(StageMother.custom("stage"));
        assertThat(goCache.get(cacheKey), is(nullValue()));
    }

    @Test
    public void shouldLoadStageHistoryEntryForAStageRunAfterTheLatestRunThatIsRetrievedForStageHistory() throws Exception {
        String pipelineName = "some_pipeline_name";
        String stageName = "some_stage_name";
        FuncVarArg function = mock(FuncVarArg.class);
        Pagination pagination = mock(Pagination.class);
        when(pagination.getCurrentPage()).thenReturn(3);
        when(pagination.getPageSize()).thenReturn(10);
        when(function.call()).thenReturn(pagination);
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
    public void shouldLoadTheStageHistoryEntryNextInTimeFromAGivenStageHistoryEntry() throws Exception {
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

    @Test
    public void shouldGetAllStagesInstancesWithArtifactsForGivenStageNameAndPipelineNameInAscendingOrder() throws Exception {
        String pipelineName = "some_pipeline_name";
        String stageName = "stage_name";
        Long fromId = 100L;
        Long toId = 200L;
        boolean ascending = true;

        List<Stage> expectedResult = Arrays.asList(new Stage());
        when(sqlMapClientTemplate.queryForList(eq("getStagesWithArtifactsGivenPipelineAndStage"), anyObject())).thenReturn(expectedResult);

        List<Stage> result = stageSqlMapDao.getStagesWithArtifactsGivenPipelineAndStage(pipelineName, stageName, fromId, toId, ascending);
        assertThat(result, is(expectedResult));

        ArgumentCaptor<Map> args = ArgumentCaptor.forClass(Map.class);
        verify(sqlMapClientTemplate).queryForList(eq("getStagesWithArtifactsGivenPipelineAndStage"), args.capture());
        assertThat(((String) args.getValue().get("pipelineName")), is(pipelineName));
        assertThat(((String) args.getValue().get("stageName")), is(stageName));
        assertThat(((Long) args.getValue().get("fromId")), is(fromId));
        assertThat(((Long) args.getValue().get("toId")), is(toId));
        assertThat(((String) args.getValue().get("order")), is("ASC"));
    }

    @Test
    public void shouldGetAllStagesInstancesWithArtifactsForGivenStageNameAndPipelineNameInDescendingOrder() throws Exception {
        String pipelineName = "some_pipeline_name";
        String stageName = "stage_name";
        Long fromId = 100L;
        Long toId = 200L;
        boolean ascending = false;

        List<Stage> expectedResult = Arrays.asList(new Stage());
        when(sqlMapClientTemplate.queryForList(eq("getStagesWithArtifactsGivenPipelineAndStage"), anyObject())).thenReturn(expectedResult);

        List<Stage> result = stageSqlMapDao.getStagesWithArtifactsGivenPipelineAndStage(pipelineName, stageName, fromId, toId, ascending);
        assertThat(result, is(expectedResult));

        ArgumentCaptor<Map> args = ArgumentCaptor.forClass(Map.class);
        verify(sqlMapClientTemplate).queryForList(eq("getStagesWithArtifactsGivenPipelineAndStage"), args.capture());
        assertThat(((String) args.getValue().get("pipelineName")), is(pipelineName));
        assertThat(((String) args.getValue().get("stageName")), is(stageName));
        assertThat(((Long) args.getValue().get("fromId")), is(fromId));
        assertThat(((Long) args.getValue().get("toId")), is(toId));
        assertThat(((String) args.getValue().get("order")), is("DESC"));
    }

    @Test
    public void shouldGetAllDistinctStages() throws Exception {
        List<StageConfigIdentifier> expectedResult = Arrays.asList(new StageConfigIdentifier());
        when(sqlMapClientTemplate.queryForList("getDistinctStages")).thenReturn(expectedResult);

        List<StageConfigIdentifier> result = stageSqlMapDao.getAllDistinctStages();
        assertThat(result, is(expectedResult));
    }
}
