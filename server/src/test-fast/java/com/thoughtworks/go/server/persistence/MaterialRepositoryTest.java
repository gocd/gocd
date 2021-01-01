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
package com.thoughtworks.go.server.persistence;

import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.domain.materials.git.GitMaterialInstance;
import com.thoughtworks.go.server.cache.GoCache;
import com.thoughtworks.go.server.database.Database;
import com.thoughtworks.go.server.service.MaterialConfigConverter;
import com.thoughtworks.go.server.service.MaterialExpansionService;
import com.thoughtworks.go.server.transaction.TransactionSynchronizationManager;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.orm.hibernate3.HibernateTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

public class MaterialRepositoryTest {

    private MaterialRepository materialRepository;
    private SessionFactory sessionFactory;
    private GoCache goCache;
    private TransactionSynchronizationManager transactionSynchronizationManager;
    private HibernateTemplate mockHibernateTemplate;
    private HashMap<String, Object> ourCustomCache;
    private MaterialConfigConverter materialConfigConverter;
    private MaterialExpansionService materialExpansionService;
    private Database databaseStrategy;

    @Before
    public void setUp() {
        databaseStrategy = mock(Database.class);
        sessionFactory = mock(SessionFactory.class);
        goCache = mock(GoCache.class);
        ourCustomCache = new HashMap<>();
        transactionSynchronizationManager = mock(TransactionSynchronizationManager.class);
        mockHibernateTemplate = mock(HibernateTemplate.class);
        materialConfigConverter = mock(MaterialConfigConverter.class);
        materialExpansionService = mock(MaterialExpansionService.class);
        materialRepository = new MaterialRepository(sessionFactory, goCache, 4242, transactionSynchronizationManager, materialConfigConverter, materialExpansionService, databaseStrategy);
        materialRepository.setHibernateTemplate(mockHibernateTemplate);
        when(goCache.get(anyString())).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Object[] arguments = invocation.getArguments();
                return ourCustomCache.get(arguments[0]);
            }
        });
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Object[] arguments = invocation.getArguments();
                return ourCustomCache.put((String) arguments[0], arguments[1]);
            }
        }).when(goCache).put(anyString(), anyObject());
        when(goCache.remove(anyString())).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Object[] arguments = invocation.getArguments();
                return ourCustomCache.remove(arguments[0]);
            }
        });
    }

    @Test
    public void shouldCacheFindModificationWithRevision() {
        Modification modification = mock(Modification.class);
        Session session = mock(Session.class);
        Query query = mock(Query.class);

        int materialId = 111;
        String revision = "Rev -1";

        String key = materialRepository.cacheKeyForModificationWithRevision(materialId, revision);
        when(session.createQuery(anyString())).thenReturn(query);
        when(query.uniqueResult()).thenReturn(modification);

        Modification actualModification = materialRepository.findModificationWithRevision(session, materialId, revision);//Prime cache, Cache get returns null twice
        assertThat(actualModification, is(modification));

        modification = materialRepository.findModificationWithRevision(session, materialId, revision); //Fetch from cache, Cache get returns modification
        assertThat(actualModification, is(modification));

        verify(query, times(1)).uniqueResult();
        verify(goCache, times(3)).get(key);
        verify(goCache, times(1)).put(key, modification);
    }

    @Test
    public void shouldNotSaveAndClearCacheWhenThereAreNoModifications() {
        GitMaterialInstance materialInstance = new GitMaterialInstance("url", null, "branch", null, UUID.randomUUID().toString());
        materialRepository.saveModifications(materialInstance, new ArrayList<>());
        verifyZeroInteractions(mockHibernateTemplate);
        verifyZeroInteractions(goCache);
    }

}
