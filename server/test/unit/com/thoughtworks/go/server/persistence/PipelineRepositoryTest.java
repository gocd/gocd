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

package com.thoughtworks.go.server.persistence;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

import com.thoughtworks.go.server.cache.GoCache;
import com.thoughtworks.go.server.database.DatabaseStrategy;
import com.thoughtworks.go.server.domain.user.PipelineSelections;
import com.thoughtworks.go.util.SystemEnvironment;
import org.hibernate.SessionFactory;
import org.junit.Before;
import org.junit.Test;
import org.springframework.orm.hibernate3.HibernateTemplate;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PipelineRepositoryTest {

    private SessionFactory sessionFactory;
    private GoCache goCache;
    private HibernateTemplate hibernateTemplate;
    private PipelineRepository pipelineRepository;
    private SystemEnvironment systemEnvironment;
    private DatabaseStrategy databaseStrategy;

    @Before
    public void setup() {
        sessionFactory = mock(SessionFactory.class);
        hibernateTemplate = mock(HibernateTemplate.class);
        goCache = mock(GoCache.class);
        systemEnvironment = mock(SystemEnvironment.class);
        databaseStrategy = mock(DatabaseStrategy.class);
        pipelineRepository = new PipelineRepository(sessionFactory, goCache,systemEnvironment, databaseStrategy);
        pipelineRepository.setHibernateTemplate(hibernateTemplate);
    }

    @Test
    public void shouldCachePipelineSelectionForGivenUserId() throws Exception {
        String queryString = "FROM PipelineSelections WHERE userId = ?";
        PipelineSelections pipelineSelections = new PipelineSelections();
        long userId = 1L;

        when(hibernateTemplate.find(queryString, new Object[]{userId})).thenReturn(Arrays.asList(pipelineSelections));
        //return false for first 2 calls and return true for next call
        when(goCache.isKeyInCache(pipelineRepository.pipelineSelectionForUserIdKey(userId))).thenReturn(false).thenReturn(false).thenReturn(true);

        pipelineRepository.findPipelineSelectionsByUserId(userId);
        pipelineRepository.findPipelineSelectionsByUserId(userId);

        verify(hibernateTemplate).find(queryString, new Object[]{userId});
        verify(goCache).put(pipelineRepository.pipelineSelectionForUserIdKey(userId), pipelineSelections);
    }

    @Test
    public void shouldCachePipelineSelectionForGivenId() throws Exception {
        PipelineSelections pipelineSelections = new PipelineSelections();
        long id = 1L;

        when(hibernateTemplate.get(PipelineSelections.class, id)).thenReturn(pipelineSelections);
        //return false for first 2 calls and return true for next call
        when(goCache.isKeyInCache(pipelineRepository.pipelineSelectionForCookieKey(id))).thenReturn(false).thenReturn(false).thenReturn(true);

        pipelineRepository.findPipelineSelectionsById(id);
        pipelineRepository.findPipelineSelectionsById(id);

        verify(hibernateTemplate).get(PipelineSelections.class, id);
        verify(goCache).put(pipelineRepository.pipelineSelectionForCookieKey(id), pipelineSelections);
    }

    @Test
    public void shouldInvalidatePipelineSelectionCacheOnSaveOrUpdate() throws Exception {


        PipelineSelections pipelineSelections = new PipelineSelections();
        pipelineSelections.setId(1);
        pipelineSelections.update(new ArrayList<String>(), new Date(), 2L, true);
        pipelineRepository.saveSelectedPipelines(pipelineSelections);


        verify(goCache).remove(pipelineRepository.pipelineSelectionForCookieKey(1L));
        verify(goCache).remove(pipelineRepository.pipelineSelectionForUserIdKey(2L));
    }
}
