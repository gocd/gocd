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
package com.thoughtworks.go.server.persistence;

import com.thoughtworks.go.config.Agent;
import com.thoughtworks.go.domain.AgentConfigStatus;
import com.thoughtworks.go.domain.AgentInstance;
import com.thoughtworks.go.domain.exception.UnregisteredAgentException;
import com.thoughtworks.go.listener.DatabaseEntityChangeListener;
import com.thoughtworks.go.remote.AgentIdentifier;
import com.thoughtworks.go.server.cache.GoCache;
import com.thoughtworks.go.server.dao.DatabaseAccessHelper;
import com.thoughtworks.go.util.SystemEnvironment;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.HashMap;
import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Fail.fail;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {
        "classpath:/applicationContext-global.xml",
        "classpath:/applicationContext-dataLocalAccess.xml",
        "classpath:/testPropertyConfigurer.xml",
        "classpath:/spring-all-servlet.xml",
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

    @BeforeEach
    public void setUp() throws Exception {
        dbHelper.onSetUp();
        hibernateTemplate = agentDao.getHibernateTemplate();
        mockHibernateTemplate = mock(HibernateTemplate.class);
    }

    @AfterEach
    public void tearDown() throws Exception {
        dbHelper.onTearDown();
        goCache.clear();
    }

    @Nested
    @ContextConfiguration(locations = {
            "classpath:/applicationContext-global.xml",
            "classpath:/applicationContext-dataLocalAccess.xml",
            "classpath:/testPropertyConfigurer.xml",
            "classpath:/spring-all-servlet.xml",
    })
    class GetAgents {
        @Test
        void shouldGetAgentByUUID() {
            Agent agent = new Agent("uuid", "localhost", "127.0.0.1", "cookie");
            agentDao.saveOrUpdate(agent);
            Agent agentFromDB = agentDao.getAgentByUUIDFromCacheOrDB(agent.getUuid());
            assertThat(agent, is(agentFromDB));
        }

        @Test
        void shouldFetchAgentFromDBByUUID() {
            Agent agent = new Agent("uuid", "localhost", "127.0.0.1", "cookie");
            agentDao.saveOrUpdate(agent);
            Agent agentFromDB = agentDao.fetchAgentFromDBByUUID(agent.getUuid());
            assertThat(agent, is(agentFromDB));
        }

        @Test
        void shouldFetchAgentFromDBIncludingDeletedAgent() {
            String uuid = "uuid";
            Agent agent = new Agent(uuid, "localhost", "127.0.0.1", "cookie");
            agentDao.saveOrUpdate(agent);
            agentDao.bulkSoftDelete(singletonList(uuid));

            Agent agentFromDB = agentDao.fetchAgentFromDBByUUID(uuid);
            assertThat(agentFromDB, is(nullValue()));

            agentFromDB = agentDao.fetchAgentFromDBByUUIDIncludingDeleted(uuid);
            assertThat(agentFromDB, is(agent));
        }

        @Test
        void shouldGetAgentsByUUIDsExcludingSoftDeletedAgents() {
            String uuid1 = "uuid1";
            String uuid2 = "uuid2";
            String uuid3 = "uuid3";

            Agent agent1 = new Agent(uuid1, "localhost1", "127.0.0.1", "cookie1");
            Agent agent2 = new Agent(uuid2, "localhost2", "127.0.0.2", "cookie2");
            Agent agent3 = new Agent(uuid3, "localhost3", "127.0.0.3", "cookie3");

            agent2.setDeleted(true);

            agentDao.saveOrUpdate(agent1);
            agentDao.saveOrUpdate(agent2);
            agentDao.saveOrUpdate(agent3);

            List<Agent> agents = agentDao.getAgentsByUUIDs(asList(uuid1, uuid2, uuid3));

            assertThat(agents.size(), is(2));
            assertThat(agents.get(0).getUuid(), is(uuid1));
            assertThat(agents.get(1).getUuid(), is(uuid3));
        }

        @Test
        void shouldGetAllAgentsExcludingSoftDeletedAgents() {
            String uuid1 = "uuid1";
            String uuid2 = "uuid2";
            String uuid3 = "uuid3";

            Agent agent1 = new Agent(uuid1, "localhost1", "127.0.0.1", "cookie1");
            Agent agent2 = new Agent(uuid2, "localhost2", "127.0.0.2", "cookie2");
            Agent agent3 = new Agent(uuid3, "localhost3", "127.0.0.3", "cookie3");

            agent2.setDeleted(true);

            agentDao.saveOrUpdate(agent1);
            agentDao.saveOrUpdate(agent2);
            agentDao.saveOrUpdate(agent3);

            List<Agent> allAgents = agentDao.getAllAgents();

            assertThat(allAgents.size(), is(2));
            assertThat(allAgents.get(0).getUuid(), is(uuid1));
            assertThat(allAgents.get(1).getUuid(), is(uuid3));
        }

        @Test
        void shouldReturnNullWhenUnknownUUIDIsSpecifiedInGetAgentByUUID() {
            Agent agent = agentDao.getAgentByUUIDFromCacheOrDB("uuid-that-does-not-exist");
            assertThat(agent, is(nullValue()));
        }

        @Test
        void shouldReturnNullWhenUnknownUUIDIsSpecifiedInFetchAgentFromDBByUUID() {
            Agent agent = agentDao.fetchAgentFromDBByUUID("uuid-that-does-not-exist");
            assertThat(agent, is(nullValue()));
        }

        @Test
        void shouldReturnEmptyListWhenUnknownUUIDsAreSpecifiedInGetAgentsByUUIDs() {
            List<Agent> agents = agentDao.getAgentsByUUIDs(singletonList("uuid-that-does-not-exist"));
            assertThat(agents, is(emptyList()));
        }

        @Test
        void shouldReturnEmptyListByGetAllAgentsWhenNoAgentExistsInDB() {
            List<Agent> agents = agentDao.getAllAgents();
            assertThat(agents, is(emptyList()));
        }
    }

    @Nested
    @ContextConfiguration(locations = {
            "classpath:/applicationContext-global.xml",
            "classpath:/applicationContext-dataLocalAccess.xml",
            "classpath:/testPropertyConfigurer.xml",
            "classpath:/spring-all-servlet.xml",
    })
    class Cookie {
        @Test
        void shouldThrowExceptionIfTheAgentIsNotPresentInDBWhileAssociatingCookie() {
            AgentIdentifier unregisteredAgentIdentifier = new AgentIdentifier("host", "127.0.0.1", "uuid2");
            assertThatCode(() -> agentDao.associateCookie(unregisteredAgentIdentifier, "cookie"))
                    .isInstanceOf(UnregisteredAgentException.class)
                    .hasMessage("Agent [uuid2] is not registered.");
        }

        @Test
        public void shouldAssociateCookieAndNotifyListeners() {
            DatabaseEntityChangeListener<Agent> mockListener1 = registerMockListener();
            DatabaseEntityChangeListener<Agent> mockListener2 = registerMockListener();

            AgentIdentifier agentIdentifier = new AgentIdentifier("host", "127.0.0.1", "uuid1");
            associateCookieAndVerifyThatCookieIsAssociated(agentIdentifier, "cookie");

            verify(mockListener1, times(2)).entityChanged(any(Agent.class));
            verify(mockListener2, times(2)).entityChanged(any(Agent.class));
        }

        @Test
        public void shouldUpdateCookieHostnameAndIpAddressOnCookieAssociation() {
            AgentIdentifier agentIdentifier = new AgentIdentifier("host", "127.0.0.1", "uuid");
            associateCookieAndVerifyFromDBThatCookieIsAssociated(agentIdentifier, "cookie");

            String updatedCookie = "updated_cookie";
            associateCookieAndVerifyThatCookieIsAssociated(agentIdentifier, updatedCookie);

            Agent agent = fetchAgentFromDBByUUID(agentIdentifier);
            assertThat(agent.getCookie(), is(updatedCookie));
            assertThat(agent.getHostname(), is(agentIdentifier.getHostName()));
            assertThat(agent.getIpaddress(), is(agentIdentifier.getIpAddress()));
        }

        @Test
        public void shouldCacheCookieForAgent() {
            AgentIdentifier agentIdentifier = new AgentIdentifier("host", "127.0.0.1", "uuid2");
            associateCookieAndVerifyThatCookieIsAssociated(agentIdentifier, "cookie");
            updateCookieAndVerifyThatCookieIsUpdated(agentIdentifier, "updated_cookie");

            assertThat(agentDao.cookieFor(agentIdentifier), is("cookie"));

            goCache.clear();
            assertThat(agentDao.cookieFor(agentIdentifier), is("updated_cookie"));
        }

        @Test
        public void shouldReturnNullWhenNoCookieIsAssociatedWithAgent() {
            AgentIdentifier agentIdentifier = new AgentIdentifier("host", "127.0.0.1", "uuid1");
            assertThat(agentDao.cookieFor(agentIdentifier), is(nullValue()));
        }

        @Test
        public void shouldNotClearCacheAndCallListenersIfTransactionFails() {
            HibernateTemplate originalTemplate = agentDao.getHibernateTemplate();
            AgentIdentifier agentIdentifier = new AgentIdentifier("host", "127.0.0.1", "uuid1");
            associateCookieAndVerifyThatCookieIsAssociated(agentIdentifier, "cookie");

            hibernateTemplate.execute((HibernateCallback) session -> {
                Agent agent = (Agent) session.createQuery("from Agent where uuid = 'uuid1'").uniqueResult();
                agent.setCookie("updated_cookie");
                session.update(agent);
                return null;
            });
            Agent agent = fetchAgentFromDBByUUID(agentIdentifier);
            assertThat(agent.getCookie(), is("updated_cookie"));

            agentDao.setHibernateTemplate(mockHibernateTemplate);
            doThrow(new RuntimeException("holy smoke")).when(mockHibernateTemplate).saveOrUpdate(any(Agent.class));
            DatabaseEntityChangeListener<Agent> mockListener = registerMockListener();
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

        private void updateCookieAndVerifyThatCookieIsUpdated(AgentIdentifier agentIdentifier, String updatedCookie) {
            hibernateTemplate.execute(session -> {
                Agent agent = (Agent) session.createQuery("from Agent where uuid = 'uuid2'").uniqueResult();
                agent.setCookie(updatedCookie);
                session.update(agent);
                return null;
            });
            Agent agent = fetchAgentFromDBByUUID(agentIdentifier);
            assertThat(agent.getCookie(), is(updatedCookie));
        }

        private void associateCookieAndVerifyFromDBThatCookieIsAssociated(AgentIdentifier agentIdentifier, String cookie) {
            Agent agent = new Agent(agentIdentifier.getUuid(), agentIdentifier.getHostName(), agentIdentifier.getIpAddress());
            agent.setCookie("dummy-cookie");
            agentDao.saveOrUpdate(agent);
            agentDao.associateCookie(agentIdentifier, "cookie");
            Agent agentFromDB = fetchAgentFromDBByUUID(agentIdentifier);
            assertThat(agentFromDB.getCookie(), is(cookie));
        }

        private void associateCookieAndVerifyThatCookieIsAssociated(AgentIdentifier agentIdentifier, String cookie) {
            Agent agent = new Agent(agentIdentifier.getUuid(), agentIdentifier.getHostName(), agentIdentifier.getIpAddress());
            agent.setCookie("dummy-cookie");
            agentDao.saveOrUpdate(agent);
            agentDao.associateCookie(agentIdentifier, cookie);
            assertThat(agentDao.cookieFor(agentIdentifier), is(cookie));
        }
    }

    @Nested
    @ContextConfiguration(locations = {
            "classpath:/applicationContext-global.xml",
            "classpath:/applicationContext-dataLocalAccess.xml",
            "classpath:/testPropertyConfigurer.xml",
            "classpath:/spring-all-servlet.xml",
    })
    class Bulk {
        @Test
        public void shouldBulkUpdateAttributes() {
            Agent agent1 = new Agent("uuid", "localhost", "127.0.0.1", "cookie");
            agent1.setResources("r1,r2");
            agent1.setEnvironments("e1,e2,e3");

            AgentInstance agentInstance1 = AgentInstance.createFromAgent(agent1, new SystemEnvironment(), null);

            Agent agent2 = new Agent("uuid2", "localhost2", "127.0.0.2", "cookie2");
            agent2.setResources("r1");

            Agent agent3 = new Agent("uuid3", "localhost3", "127.0.0.3", "cookie3");
            agent3.setResources("r1");
            agent3.setEnvironments("e1,e3");
            AgentInstance agentInstance3 = AgentInstance.createFromAgent(agent3, new SystemEnvironment(), null);

            agentDao.saveOrUpdate(agent1);
            agentDao.saveOrUpdate(agent2);
            agentDao.saveOrUpdate(agent3);

            HashMap<String, AgentConfigStatus> agentToStatusMap = new HashMap<>();
            agentToStatusMap.put(agent1.getUuid(), agentInstance1.getStatus().getConfigStatus());
            agentToStatusMap.put(agent3.getUuid(), agentInstance3.getStatus().getConfigStatus());

            agent1.addResources(asList("r3", "r4"));
            agent3.addResources(asList("r3", "r4"));

            agent1.removeResources(asList("r1", "r2"));
            agent3.removeResources(asList("r1", "r2"));

            agent1.addEnvironments(asList("e2", "e4"));
            agent3.addEnvironments(asList("e2", "e4"));

            agent1.removeEnvironments(asList("e1", "e3"));
            agent3.removeEnvironments(asList("e1", "e3"));

            agentDao.bulkUpdateAgents(asList(agent1, agent3));

            assertThat(agentDao.getAgentByUUIDFromCacheOrDB(agent1.getUuid()).getResources(), is("r3,r4"));
            assertThat(agentDao.getAgentByUUIDFromCacheOrDB(agent2.getUuid()).getResources(), is("r1"));
            assertThat(agentDao.getAgentByUUIDFromCacheOrDB(agent3.getUuid()).getResources(), is("r3,r4"));

            assertThat(agentDao.getAgentByUUIDFromCacheOrDB(agent1.getUuid()).getEnvironments(), is("e2,e4"));
            assertThat(agentDao.getAgentByUUIDFromCacheOrDB(agent2.getUuid()).getEnvironments(), isEmptyString());
            assertThat(agentDao.getAgentByUUIDFromCacheOrDB(agent3.getUuid()).getEnvironments(), is("e2,e4"));
        }

        @Test
        public void shouldBulkDeleteAgents() {
            String uuid1 = "uuid1";
            String uuid2 = "uuid2";

            Agent agent1 = new Agent(uuid1, "localhost", "127.0.0.1", "cookie");
            Agent agent2 = new Agent(uuid2, "localhost2", "127.0.0.2", "cookie2");

            agentDao.saveOrUpdate(agent1);
            agentDao.saveOrUpdate(agent2);

            agentDao.bulkSoftDelete(asList(uuid1, uuid2));

            assertThat(agentDao.getAgentsByUUIDs(asList(uuid1, uuid2)), is(emptyList()));
        }

        @Test
        public void shouldBulkDisableAgents() {
            String disabledUUID1 = "uuid1";
            String disabledUUID2 = "uuid2";

            Agent agent1 = new Agent(disabledUUID1, "localhost1", "127.0.0.1", "cookie");
            Agent agent2 = new Agent(disabledUUID2, "localhost2", "127.0.0.2", "cookie3");

            agentDao.saveOrUpdate(agent1);
            agentDao.saveOrUpdate(agent2);

            List<String> disabledUuids = asList(disabledUUID1, disabledUUID2);
            agentDao.disableAgents(disabledUuids);

            disabledUuids.forEach(uuid -> assertThat(agentDao.getAgentByUUIDFromCacheOrDB(uuid).isDisabled(), is(true)));
        }
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

    private Agent fetchAgentFromDBByUUID(AgentIdentifier agentIdentifier) {
        return (Agent) hibernateTemplate.execute(session -> session.createSQLQuery("SELECT * from Agents where uuid = '" + agentIdentifier.getUuid() + "'")
                .addEntity(Agent.class).uniqueResult());
    }

    private DatabaseEntityChangeListener<Agent> registerMockListener() {
        DatabaseEntityChangeListener<Agent> mockListener = mock(DatabaseEntityChangeListener.class);
        agentDao.registerDatabaseAgentEntityChangeListener(mockListener);
        return mockListener;
    }
}
