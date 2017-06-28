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

package com.thoughtworks.go.server.dao;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

import com.ibatis.sqlmap.client.SqlMapClient;
import com.thoughtworks.go.config.GoConfigDao;
import com.thoughtworks.go.database.Database;
import com.thoughtworks.go.domain.Pipeline;
import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.helper.ModificationsMother;
import com.thoughtworks.go.presentation.pipelinehistory.PipelineInstanceModel;
import com.thoughtworks.go.presentation.pipelinehistory.PipelineInstanceModels;
import com.thoughtworks.go.server.cache.GoCache;
import com.thoughtworks.go.server.persistence.MaterialRepository;
import com.thoughtworks.go.util.SystemEnvironment;
import org.hibernate.SessionFactory;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.orm.ibatis.SqlMapClientTemplate;

import static com.thoughtworks.go.helper.ModificationsMother.*;
import static com.thoughtworks.go.util.IBatisUtil.arguments;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.*;

public class PipelineSqlMapDaoTest {

    PipelineSqlMapDao pipelineSqlMapDao;
    private GoCache goCache;
    private SqlMapClientTemplate sqlMapClientTemplate;
    private SqlMapClient sqlMapClient;
    private MaterialRepository materialRepository;

    @Before
    public void setUp() throws Exception {
        goCache = mock(GoCache.class);
        sqlMapClientTemplate = mock(SqlMapClientTemplate.class);
        sqlMapClient = mock(SqlMapClient.class);
        materialRepository = mock(MaterialRepository.class);
        pipelineSqlMapDao = new PipelineSqlMapDao(null, materialRepository, goCache, null, null, sqlMapClient, null, null, null, null, null);
        pipelineSqlMapDao.setSqlMapClientTemplate(sqlMapClientTemplate);
    }

    @Test
    public void shouldLoadPipelineHistoryFromCacheWhenQueriedViaNameAndCounter() throws Exception {
        String pipelineName = "wholetthedogsout";
        int pipelineCounter = 42;
        PipelineInstanceModel expected = mock(PipelineInstanceModel.class);
        when(goCache.get(anyString())).thenReturn(expected);

        PipelineInstanceModel reFetch = pipelineSqlMapDao.findPipelineHistoryByNameAndCounter(pipelineName, pipelineCounter); //returned from cache

        assertThat(reFetch, is(expected));
        verify(goCache).get(anyString());
    }

    @Test
    public void shouldPrimePipelineHistoryToCacheWhenQueriedViaNameAndCounter() throws Exception {
        String pipelineName = "wholetthedogsout";
        int pipelineCounter = 42;
        Map<String, Object> map = arguments("pipelineName", pipelineName).and("pipelineCounter", pipelineCounter).asMap();
        PipelineInstanceModel expected = mock(PipelineInstanceModel.class);
        when(sqlMapClientTemplate.queryForObject("getPipelineHistoryByNameAndCounter", map)).thenReturn(expected);
        when(expected.getId()).thenReturn(1111l);
        when(materialRepository.findMaterialRevisionsForPipeline(expected.getId())).thenReturn(null);

        PipelineInstanceModel primed = pipelineSqlMapDao.findPipelineHistoryByNameAndCounter(pipelineName, pipelineCounter);//prime cache

        assertThat(primed, is(expected));

        verify(sqlMapClientTemplate, times(1)).queryForObject("getPipelineHistoryByNameAndCounter", map);
        verify(goCache, times(1)).put(anyString(), eq(expected));
        verify(goCache, times(2)).get(anyString());
    }

    @Test
    public void shouldUpdateCommentAndRemoveItFromPipelineHistoryCache() throws Exception {
        String pipelineName = "wholetthedogsout";
        int pipelineCounter = 42;
        String comment = "This song is from the 90s.";
        Map<String, Object> args = arguments("pipelineName", pipelineName).and("pipelineCounter", pipelineCounter).and("comment", comment).asMap();

        Pipeline expected = mock(Pipeline.class);
        when(sqlMapClientTemplate.queryForObject("findPipelineByNameAndCounter", arguments("name", pipelineName).and("counter", pipelineCounter).asMap())).thenReturn(expected);
        when(expected.getId()).thenReturn(102413L);

        pipelineSqlMapDao.updateComment(pipelineName, pipelineCounter, comment);

        verify(sqlMapClientTemplate, times(1)).update("updatePipelineComment", args);
        verify(goCache, times(1)).remove("com.thoughtworks.go.server.dao.PipelineSqlMapDao_pipelineHistory_102413");
    }

    @Test
    public void shouldGetLatestRevisionFromOrderedLists() {
        PipelineSqlMapDao pipelineSqlMapDao = new PipelineSqlMapDao(null, null, null, null, null, null, null, new SystemEnvironment(), mock(GoConfigDao.class), mock(Database.class), mock(SessionFactory.class));
        ArrayList list1 = new ArrayList();
        ArrayList list2 = new ArrayList();
        Assert.assertThat(pipelineSqlMapDao.getLatestRevisionFromOrderedLists(list1, list2), is((String) null));
        Modification modification1 = new Modification(MOD_USER, MOD_COMMENT, EMAIL_ADDRESS,
                YESTERDAY_CHECKIN, ModificationsMother.nextRevision());
        list1.add(modification1);
        Assert.assertThat(pipelineSqlMapDao.getLatestRevisionFromOrderedLists(list1, list2), is(ModificationsMother.currentRevision()));
        Modification modification2 = new Modification(MOD_USER_COMMITTER, MOD_COMMENT_2, EMAIL_ADDRESS,
                TODAY_CHECKIN, ModificationsMother.nextRevision());
        list2.add(modification2);
        Assert.assertThat(pipelineSqlMapDao.getLatestRevisionFromOrderedLists(list1, list2), is(ModificationsMother.currentRevision()));
    }

    @Test
    public void loadHistoryByIds_shouldLoadHistoryByIdWhenOnlyASingleIdIsNeedeSoThatItUsesTheExistingCacheForEnvironmentsPage() throws Exception {
        SqlMapClientTemplate mockTemplate = mock(SqlMapClientTemplate.class);
        when(mockTemplate.queryForList(eq("getPipelineRange"), any())).thenReturn(Arrays.asList(2L));
        pipelineSqlMapDao.setSqlMapClientTemplate(mockTemplate);
        PipelineInstanceModels pipelineHistories = pipelineSqlMapDao.loadHistory("pipelineName", 1, 0);
        verify(mockTemplate, never()).queryForList(eq("getPipelineHistoryByName"), any());
        verify(mockTemplate, times(1)).queryForList(eq("getPipelineRange"), any());
    }

}
