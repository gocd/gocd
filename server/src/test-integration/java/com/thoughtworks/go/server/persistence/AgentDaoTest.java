/*
 * Copyright 2019 ThoughtWorks, Inc.
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

import com.thoughtworks.go.config.Agent;
import com.thoughtworks.go.config.ResourceConfigs;
import com.thoughtworks.go.domain.AgentConfigStatus;
import com.thoughtworks.go.domain.AgentInstance;
import com.thoughtworks.go.listener.DatabaseEntityChangeListener;
import com.thoughtworks.go.remote.AgentIdentifier;
import com.thoughtworks.go.server.cache.GoCache;
import com.thoughtworks.go.server.dao.DatabaseAccessHelper;
import com.thoughtworks.go.util.SystemEnvironment;
import com.thoughtworks.go.util.TriState;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        "classpath:WEB-INF/applicationContext-global.xml",
        "classpath:WEB-INF/applicationContext-dataLocalAccess.xml",
        "classpath:testPropertyConfigurer.xml",
        "classpath:WEB-INF/spring-all-servlet.xml",
})
public class AgentDaoTest {
    @Autowired
    private AgentDao agentDao;
    @Autowired
    private DatabaseAccessHelper dbHelper;
    @Autowired
    private GoCache goCache;
    private HibernateTemplate hibernateTemplate;
    private HibernateTemplate mockHibernateTemplate;

    @Before
    public void setUp() throws Exception {
        dbHelper.onSetUp();
        hibernateTemplate = agentDao.getHibernateTemplate();
        mockHibernateTemplate = mock(HibernateTemplate.class);
    }

    @After
    public void tearDown() throws Exception {
        dbHelper.onTearDown();
        goCache.clear();
    }

    @Test
    public void shouldGetAgentByUUID() {
        Agent agent = new Agent("uuid", "localhost", "127.0.0.1", "cookie");
        agentDao.saveOrUpdate(agent);

        Agent agentFromDB = agentDao.agentByUuid(agent.getUuid());
        assertThat(agent, is(agentFromDB));
    }

    @Test
    public void shouldAssociateCookieWithAnAgent() {
        AgentIdentifier agentIdentifier = new AgentIdentifier("host", "127.0.0.1", "uuid1");
        DatabaseEntityChangeListener<Agent> mockListener = mock(DatabaseEntityChangeListener.class);
        agentDao.registerListener(mockListener);
        agentDao.associateCookie(agentIdentifier, "cookie");
        assertThat(agentDao.cookieFor(agentIdentifier), is("cookie"));
        verify(mockListener).entityChanged(any(Agent.class));
    }

    @Test
    public void shouldReturnNullIfNoCookieIsAssociatedWithAnAgent() {
        AgentIdentifier agentIdentifier = new AgentIdentifier("host", "127.0.0.1", "uuid1");
        assertThat(agentDao.cookieFor(agentIdentifier), is(nullValue()));
    }

    @Test
    public void shouldUpdateExistingAgentMappingIfOneExists() {
        AgentIdentifier agentIdentifier = new AgentIdentifier("host", "127.0.0.1", "uuid");
        agentDao.associateCookie(agentIdentifier, "cookie");
        Agent agent = getAgentByUuid(agentIdentifier);
        assertThat(agent.getCookie(), is("cookie"));

        agentDao.associateCookie(agentIdentifier, "cookie_updated");
        assertThat(agentDao.cookieFor(agentIdentifier), is("cookie_updated"));

        agent = getAgentByUuid(agentIdentifier);
        assertThat(agent.getCookie(), is("cookie_updated"));
        assertThat(agent.getHostname(), is(agentIdentifier.getHostName()));
        assertThat(agent.getIpaddress(), is(agentIdentifier.getIpAddress()));
    }

    @Test
    public void shouldNotClearCacheAndCallListenersIfTransactionFails() {
        HibernateTemplate originalTemplate = agentDao.getHibernateTemplate();
        AgentIdentifier agentIdentifier = new AgentIdentifier("host", "127.0.0.1", "uuid1");
        agentDao.associateCookie(agentIdentifier, "cookie");
        assertThat(agentDao.cookieFor(agentIdentifier), is("cookie"));
        hibernateTemplate.execute(new HibernateCallback() {
            @Override
            public Object doInHibernate(Session session) throws HibernateException {
                Agent agent = (Agent) session.createQuery("from Agent where uuid = 'uuid1'").uniqueResult();
                agent.setFieldValues("updated_cookie", agentIdentifier.getHostName(), agentIdentifier.getIpAddress());
                session.update(agent);
                return null;
            }
        });
        Agent agent = getAgentByUuid(agentIdentifier);
        assertThat(agent.getCookie(), is("updated_cookie"));

        agentDao.setHibernateTemplate(mockHibernateTemplate);
        doThrow(new RuntimeException("holy smoke")).when(mockHibernateTemplate).saveOrUpdate(any(Agent.class));
        DatabaseEntityChangeListener<Agent> mockListener = mock(DatabaseEntityChangeListener.class);
        agentDao.registerListener(mockListener);
        try {
            agentDao.associateCookie(agentIdentifier, "cookie");
            fail("should have propagated saveOrUpdate exception");
        } catch (Exception e) {
            assertThat(e.getMessage(), is("holy smoke"));
        }
        assertThat(agentDao.cookieFor(agentIdentifier), is("cookie"));
        verify(mockListener, never()).entityChanged(any(Agent.class));
        agentDao.setHibernateTemplate(originalTemplate);
    }

    @Test
    public void shouldGetAgentsForGivenUuidsExcludingSoftDeletedAgents() {
        Agent agent1 = new Agent("uuid", "localhost", "127.0.0.1", "cookie");
        Agent agent2 = new Agent("uuid2", "localhost2", "127.0.0.2", "cookie2");
        agent2.setDeleted(true);

        agentDao.saveOrUpdate(agent1);
        agentDao.saveOrUpdate(agent2);

        List<String> uuids = Arrays.asList("uuid", "uuid2");

        List<Agent> allAgents = agentDao.agentsByUUIds(uuids);

        assertThat(allAgents.size(), is(1));
        assertThat(allAgents.get(0).getUuid(), is("uuid"));
    }

    @Test
    public void shouldReturnSameCacheKeyForDifferentStringsHoldingSameValue() {
        String uuid1 = "uuid";
        String uuid2 = new String("uuid");
        String uuid3 = String.valueOf("uuid");
        String uuid4 = "u" + "u" + "i" + "d";

        String key1 = agentDao.agentCacheKey(uuid1);
        String key2 = agentDao.agentCacheKey(uuid2);
        String key3 = agentDao.agentCacheKey(uuid3);
        String key4 = agentDao.agentCacheKey(uuid4);

        assertEquals(key1, key2);
        assertEquals(key2, key3);
        assertEquals(key1, key3);
        assertEquals(key1, key4);
    }

    @Test
    public void shouldGetAllAgentsExcludingSoftDeletedAgents() {
        Agent agent1 = new Agent("uuid", "localhost", "127.0.0.1", "cookie");
        Agent agent2 = new Agent("uuid2", "localhost2", "127.0.0.2", "cookie2");
        agent2.setDeleted(true);

        agentDao.saveOrUpdate(agent1);
        agentDao.saveOrUpdate(agent2);

        List<Agent> allAgents = agentDao.allAgents();

        assertThat(allAgents.size(), is(1));
        assertThat(allAgents.get(0).getUuid(), is("uuid"));
    }

    @Test
    public void shouldChangeAgentDisabledFlag() {
        Agent agent1 = new Agent("uuid", "localhost", "127.0.0.1", "cookie");
        Agent agent2 = new Agent("uuid2", "localhost2", "127.0.0.2", "cookie2");
        Agent agent3 = new Agent("uuid3", "localhost3", "127.0.0.3", "cookie3");

        agentDao.saveOrUpdate(agent1);
        agentDao.saveOrUpdate(agent2);
        agentDao.saveOrUpdate(agent3);

        List<String> disabledUuids = Arrays.asList("uuid", "uuid3");
        List<String> enabledUuids = Arrays.asList("uuid2");

        agentDao.changeDisabled(disabledUuids, true);
        agentDao.changeDisabled(enabledUuids, false);

        assertThat(agentDao.agentByUuid(disabledUuids.get(0)).isDisabled(), is(true));
        assertThat(agentDao.agentByUuid(disabledUuids.get(1)).isDisabled(), is(true));

        assertThat(agentDao.agentByUuid(enabledUuids.get(0)).isDisabled(), is(false));
    }

    @Test
    public void shouldBulkUpdateAttributes() {
        Agent agent = new Agent("uuid", "localhost", "127.0.0.1", "cookie");
        agent.setResourceConfigs(new ResourceConfigs("resource1,resource2"));
        AgentInstance agentInstance1 = AgentInstance.createFromAgent(agent, new SystemEnvironment(), null);
        agent.setEnvironments("env1,env2,env3");

        Agent agent1 = new Agent("uuid2", "localhost2", "127.0.0.2", "cookie2");
        agent1.setResourceConfigs(new ResourceConfigs("resource1"));
        AgentInstance agentInstance2 = AgentInstance.createFromAgent(agent1, new SystemEnvironment(), null);

        Agent agent2 = new Agent("uuid3", "localhost3", "127.0.0.3", "cookie3");
        agent2.setResourceConfigs(new ResourceConfigs("resource1"));
        agent2.setEnvironments("env1,env3");
        AgentInstance agentInstance3 = AgentInstance.createFromAgent(agent2, new SystemEnvironment(), null);

        agentDao.saveOrUpdate(agent);
        agentDao.saveOrUpdate(agent1);
        agentDao.saveOrUpdate(agent2);

        HashMap<String, AgentConfigStatus> agentToStatusMap = new HashMap<>();
        agentToStatusMap.put(agent.getUuid(), agentInstance1.getStatus().getConfigStatus());
        agentToStatusMap.put(agent2.getUuid(), agentInstance3.getStatus().getConfigStatus());

        agent.addResources(Arrays.asList("resource3", "resource4"));
        agent2.addResources(Arrays.asList("resource3", "resource4"));

        agent.removeResources(Arrays.asList("resource1", "resource2"));
        agent2.removeResources(Arrays.asList("resource1", "resource2"));

        agent.addEnvironments(Arrays.asList("env2", "env4"));
        agent2.addEnvironments(Arrays.asList("env2", "env4"));

        agent.removeEnvironments(Arrays.asList("env1", "env3"));
        agent2.removeEnvironments(Arrays.asList("env1", "env3"));

        agentDao.bulkUpdateAttributes(Arrays.asList(agent, agent2), agentToStatusMap, TriState.UNSET);

        assertThat(agentDao.agentByUuid(agent.getUuid()).getResources().resourceNames(), is(Arrays.asList("resource3", "resource4")));
        assertThat(agentDao.agentByUuid(agent1.getUuid()).getResources().resourceNames(), is(Arrays.asList("resource1")));
        assertThat(agentDao.agentByUuid(agent2.getUuid()).getResources().resourceNames(), is(Arrays.asList("resource3", "resource4")));

        assertThat(agentDao.agentByUuid(agent.getUuid()).getEnvironments(), is("env2,env4"));
        assertNull(agentDao.agentByUuid(agent1.getUuid()).getEnvironments());
        assertThat(agentDao.agentByUuid(agent2.getUuid()).getEnvironments(), is("env2,env4"));
    }

    @Test
    public void shouldBulkDeleteAgent() {
        Agent agent1 = new Agent("uuid", "localhost", "127.0.0.1", "cookie");
        Agent agent2 = new Agent("uuid2", "localhost2", "127.0.0.2", "cookie2");
        agentDao.saveOrUpdate(agent1);
        agentDao.saveOrUpdate(agent2);

        agentDao.bulkSoftDelete(Arrays.asList("uuid", "uuid2"));

        assertNull(agentDao.agentByUuid("uuid"));
        assertNull(agentDao.agentByUuid("uuid2"));
    }


    private Agent getAgentByUuid(AgentIdentifier agentIdentifier) {
        return (Agent) hibernateTemplate.execute(session -> session.createSQLQuery("SELECT * from Agents where uuid = '" + agentIdentifier.getUuid() + "'")
                .addEntity(Agent.class).uniqueResult());
    }

    private Agent getAgentByUuid(String uuid) {
        return (Agent) hibernateTemplate.execute(session -> session.createSQLQuery("SELECT * from Agents where uuid = '" + uuid + "'")
                .addEntity(Agent.class).uniqueResult());
    }

    @Test
    public void shouldCacheCookieForAgent() {
        AgentIdentifier agentIdentifier = new AgentIdentifier("host", "127.0.0.1", "uuid2");
        agentDao.associateCookie(agentIdentifier, "cookie");
        assertThat(agentDao.cookieFor(agentIdentifier), is("cookie"));
        hibernateTemplate.execute(session -> {
            Agent agent = (Agent) session.createQuery("from Agent where uuid = 'uuid2'").uniqueResult();
            agent.setFieldValues("updated_cookie", agentIdentifier.getHostName(), agentIdentifier.getIpAddress());
            session.update(agent);
            return null;
        });
        Agent agent = getAgentByUuid(agentIdentifier);
        assertThat(agent.getCookie(), is("updated_cookie"));
        assertThat(agentDao.cookieFor(agentIdentifier), is("cookie"));
        goCache.clear();
        assertThat(agentDao.cookieFor(agentIdentifier), is("updated_cookie"));
    }
}
