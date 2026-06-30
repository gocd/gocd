/*
 * Copyright Thoughtworks, Inc.
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
import com.thoughtworks.go.server.caching.GoCache;
import com.thoughtworks.go.server.database.Database;
import com.thoughtworks.go.server.service.MaterialConfigConverter;
import com.thoughtworks.go.server.service.MaterialExpansionService;
import com.thoughtworks.go.server.transaction.TransactionSynchronizationManager;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.orm.hibernate3.HibernateTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class MaterialRepositoryTest {

    private MaterialRepository materialRepository;
    private GoCache goCache;
    private HibernateTemplate mockHibernateTemplate;
    private Map<String, Object> ourCustomCache;

    @BeforeEach
    public void setUp() {
        Database databaseStrategy = mock(Database.class);
        SessionFactory sessionFactory = mock(SessionFactory.class);
        goCache = mock(GoCache.class);
        ourCustomCache = new HashMap<>();
        TransactionSynchronizationManager transactionSynchronizationManager = mock(TransactionSynchronizationManager.class);
        mockHibernateTemplate = mock(HibernateTemplate.class);
        MaterialConfigConverter materialConfigConverter = mock(MaterialConfigConverter.class);
        MaterialExpansionService materialExpansionService = mock(MaterialExpansionService.class);
        materialRepository = new MaterialRepository(sessionFactory, goCache, 4242, transactionSynchronizationManager, materialConfigConverter, materialExpansionService, databaseStrategy);
        materialRepository.setHibernateTemplate(mockHibernateTemplate);
        when(goCache.get(anyString()))
            .thenAnswer(invocation -> ourCustomCache.get(invocation.<String>getArgument(0)));
        doAnswer(invocation -> ourCustomCache.put(invocation.getArgument(0), invocation.<String>getArgument(1)))
            .when(goCache).put(anyString(), any());
        when(goCache.remove(anyString()))
            .thenAnswer(invocation -> ourCustomCache.remove(invocation.<String>getArgument(0)));
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
        assertThat(actualModification).isEqualTo(modification);

        modification = materialRepository.findModificationWithRevision(session, materialId, revision); //Fetch from cache, Cache get returns modification
        assertThat(actualModification).isEqualTo(modification);

        verify(query, times(1)).uniqueResult();
        verify(goCache, times(3)).get(key);
        verify(goCache, times(1)).put(key, modification);
    }

    @Test
    public void shouldNotSaveAndClearCacheWhenThereAreNoModifications() {
        GitMaterialInstance materialInstance = new GitMaterialInstance("url", null, "branch", null, UUID.randomUUID().toString());
        materialRepository.saveModifications(materialInstance, new ArrayList<>());
        verifyNoInteractions(mockHibernateTemplate);
        verifyNoInteractions(goCache);
    }

}
