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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;
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
        List<StageIdentity> latestStages = asList(new StageIdentity("p1", "s1", 10L), new StageIdentity("p2", "s2", 100L));
        when(sqlMapClientTemplate.queryForList("latestStageInstances")).thenReturn(latestStages);

        List<StageIdentity> firstStageInstances = stageSqlMapDao.findLatestStageInstances();

        List<StageIdentity> latestStageInstances = stageSqlMapDao.findLatestStageInstances();

        assertThat(firstStageInstances, is(latestStages));
        assertThat(latestStageInstances, is(latestStages));
        verify(sqlMapClientTemplate, times(1)).queryForList("latestStageInstances");
    }

    @Test
    public void shouldRemoveLatestStageInstancesFromCache_OnStageChange() {
        when(sqlMapClientTemplate.queryForList("latestStageInstances")).thenReturn(asList(new StageIdentity("p1", "s1", 10L), new StageIdentity("p2", "s2", 100L)));
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
    public void shouldGetAllStagesInstancesWithArtifactsWithStagesFilter() throws Exception {
        String pipelineName = "some_pipeline_name";
        String stageOne = "stage_one";
        String stageTwo = "stage_two";
        String stageThree = "stage_three";
        String stageFour = "stage_four";
        List<StageConfigIdentifier> includeStages = asList(new StageConfigIdentifier(pipelineName, stageOne), new StageConfigIdentifier(pipelineName, stageTwo));
        List<StageConfigIdentifier> excludeStages = asList(new StageConfigIdentifier(pipelineName, stageThree), new StageConfigIdentifier(pipelineName, stageFour));
        Long fromId = 100L;
        Long toId = 200L;
        boolean ascending = true;

        List<Stage> expectedResult = asList(new Stage());
        when(sqlMapClientTemplate.queryForList(eq("getStagesWithArtifacts"), anyObject())).thenReturn(expectedResult);

        List<Stage> result = stageSqlMapDao.getStagesWithArtifacts(includeStages, excludeStages, fromId, toId, ascending);

        assertThat(result, is(expectedResult));

        ArgumentCaptor<Map> args = ArgumentCaptor.forClass(Map.class);
        verify(sqlMapClientTemplate).queryForList(eq("getStagesWithArtifacts"), args.capture());
        assertThat(((String) args.getValue().get("includeStages")), is("'some_pipeline_name/stage_one','some_pipeline_name/stage_two'"));
        assertThat(((String) args.getValue().get("excludeStages")), is("'some_pipeline_name/stage_three','some_pipeline_name/stage_four'"));
        assertThat(((Long) args.getValue().get("fromId")), is(fromId));
        assertThat(((Long) args.getValue().get("toId")), is(toId));
        assertThat(((String) args.getValue().get("orderBy")), is("ASC"));
    }

    @Test
    public void shouldGetAllStagesInstancesWithArtifactsWithoutStagesFilter() throws Exception {
        Long fromId = 100L;
        Long toId = 200L;
        boolean ascending = true;

        List<Stage> expectedResult = asList(new Stage());
        when(sqlMapClientTemplate.queryForList(eq("getStagesWithArtifacts"), anyObject())).thenReturn(expectedResult);

        List<Stage> result = stageSqlMapDao.getStagesWithArtifacts(new ArrayList<StageConfigIdentifier>(), null, fromId, toId, ascending);

        assertThat(result, is(expectedResult));

        ArgumentCaptor<Map> args = ArgumentCaptor.forClass(Map.class);
        verify(sqlMapClientTemplate).queryForList(eq("getStagesWithArtifacts"), args.capture());
        assertThat(args.getValue().get("includeStages"), nullValue());
        assertThat(args.getValue().get("excludeStages"), nullValue());
        assertThat(((Long) args.getValue().get("fromId")), is(fromId));
        assertThat(((Long) args.getValue().get("toId")), is(toId));
        assertThat(((String) args.getValue().get("orderBy")), is("ASC"));
    }

    @Test
    public void shouldGetAllStagesInstancesWithArtifactsInDescendingOrder() throws Exception {
        Long fromId = 100L;
        Long toId = 200L;
        boolean ascending = false;

        List<Stage> expectedResult = asList(new Stage());
        when(sqlMapClientTemplate.queryForList(eq("getStagesWithArtifacts"), anyObject())).thenReturn(expectedResult);

        List<Stage> result = stageSqlMapDao.getStagesWithArtifacts(new ArrayList<StageConfigIdentifier>(), null, fromId, toId, ascending);

        assertThat(result, is(expectedResult));

        ArgumentCaptor<Map> args = ArgumentCaptor.forClass(Map.class);
        verify(sqlMapClientTemplate).queryForList(eq("getStagesWithArtifacts"), args.capture());
        assertThat(args.getValue().get("includeStages"), nullValue());
        assertThat(args.getValue().get("excludeStages"), nullValue());
        assertThat(((Long) args.getValue().get("fromId")), is(fromId));
        assertThat(((Long) args.getValue().get("toId")), is(toId));
        assertThat(((String) args.getValue().get("orderBy")), is("DESC"));
    }

    @Test
    public void shouldGetAllDistinctStages() throws Exception {
        List<StageConfigIdentifier> expectedResult = asList(new StageConfigIdentifier("pipeline", "stage"));
        when(sqlMapClientTemplate.queryForList("getDistinctStages")).thenReturn(expectedResult);

        List<StageConfigIdentifier> result = stageSqlMapDao.getAllDistinctStages();
        assertThat(result, is(expectedResult));
    }
}
