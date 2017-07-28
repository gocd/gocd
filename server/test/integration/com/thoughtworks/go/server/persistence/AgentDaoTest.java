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

import java.sql.SQLException;

import com.thoughtworks.go.remote.AgentIdentifier;
import com.thoughtworks.go.server.cache.GoCache;
import com.thoughtworks.go.server.dao.DatabaseAccessHelper;
import com.thoughtworks.go.server.domain.Agent;
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

import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;


@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        "classpath:WEB-INF/applicationContext-global.xml",
        "classpath:WEB-INF/applicationContext-dataLocalAccess.xml",
        "classpath:WEB-INF/applicationContext-acegi-security.xml"
})
public class AgentDaoTest {
    @Autowired private AgentDao agentDao;
    @Autowired private DatabaseAccessHelper dbHelper;
    @Autowired private GoCache goCache;
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
    public void shouldReturnNullIfNoCookie() throws Exception {
        AgentIdentifier agentIdentifier = new AgentIdentifier("host", "127.0.0.1", "uuid");
        assertThat(agentDao.cookieFor(agentIdentifier), is(nullValue()));
    }

    @Test
    public void shouldAssociateInformationForAGivenAgent() throws Exception {
        AgentIdentifier agentIdentifier = new AgentIdentifier("host", "127.0.0.1", "uuid");
        agentDao.associateCookie(agentIdentifier, "cookie");
        assertThat(agentDao.cookieFor(agentIdentifier), is("cookie"));
        Agent agent = getAgentByUuid(agentIdentifier);
        assertThat(agent.getCookie(), is("cookie"));
        assertThat(agent.getHostname(), is(agentIdentifier.getHostName()));
        assertThat(agent.getIpaddress(), is(agentIdentifier.getIpAddress()));
    }

    @Test
    public void shouldUpdateExistingAgentMappingIfOneExists() throws Exception {
        AgentIdentifier agentIdentifier = new AgentIdentifier("host", "127.0.0.1", "uuid");
        agentDao.associateCookie(agentIdentifier, "cookie");
        agentDao.associateCookie(agentIdentifier, "cookie_updated");
        assertThat(agentDao.cookieFor(agentIdentifier), is("cookie_updated"));
        Agent agent = getAgentByUuid(agentIdentifier);
        assertThat(agent.getCookie(), is("cookie_updated"));
        assertThat(agent.getHostname(), is(agentIdentifier.getHostName()));
        assertThat(agent.getIpaddress(), is(agentIdentifier.getIpAddress()));
    }

    @Test
    public void shouldCacheCookieForAgent() throws Exception {
        AgentIdentifier agentIdentifier = new AgentIdentifier("host", "127.0.0.1", "uuid");
        agentDao.associateCookie(agentIdentifier, "cookie");
        assertThat(agentDao.cookieFor(agentIdentifier), is("cookie"));
        hibernateTemplate.execute(new HibernateCallback() {
            public Object doInHibernate(Session session) throws HibernateException, SQLException {
                Agent agent = (Agent) session.createQuery("from Agent where uuid = 'uuid'").uniqueResult();
                agent.update("updated_cookie", agentIdentifier.getHostName(), agentIdentifier.getIpAddress());
                session.update(agent);
                return null;
            }
        });
        Agent agent = getAgentByUuid(agentIdentifier);
        assertThat(agent.getCookie(), is("updated_cookie"));
        assertThat(agentDao.cookieFor(agentIdentifier), is("cookie"));
        goCache.clear();
        assertThat(agentDao.cookieFor(agentIdentifier), is("updated_cookie"));
    }

    @Test
    public void shouldNotClearCacheIfTransactionFails() throws Exception {
        HibernateTemplate originalTemplate = agentDao.getHibernateTemplate();
        AgentIdentifier agentIdentifier = new AgentIdentifier("host", "127.0.0.1", "uuid");
        agentDao.associateCookie(agentIdentifier, "cookie");
        assertThat(agentDao.cookieFor(agentIdentifier), is("cookie"));
        hibernateTemplate.execute(new HibernateCallback() {
            public Object doInHibernate(Session session) throws HibernateException, SQLException {
                Agent agent = (Agent) session.createQuery("from Agent where uuid = 'uuid'").uniqueResult();
                agent.update("updated_cookie", agentIdentifier.getHostName(), agentIdentifier.getIpAddress());
                session.update(agent);
                return null;
            }
        });
        Agent agent = getAgentByUuid(agentIdentifier);
        assertThat(agent.getCookie(), is("updated_cookie"));

        agentDao.setHibernateTemplate(mockHibernateTemplate);
        doThrow(new RuntimeException("holy smoke")).when(mockHibernateTemplate).saveOrUpdate(any(Agent.class));
        try {
            agentDao.associateCookie(agentIdentifier, "cookie");
            fail("should have propagated saveOrUpdate exception");
        } catch (Exception e) {
            assertThat(e.getMessage(), is("holy smoke"));
        }
        assertThat(agentDao.cookieFor(agentIdentifier), is("cookie"));
        agentDao.setHibernateTemplate(originalTemplate);
    }

    private Agent getAgentByUuid(AgentIdentifier agentIdentifier) {
        return (Agent) hibernateTemplate.execute(new HibernateCallback() {
            public Object doInHibernate(Session session) throws HibernateException, SQLException {
                return session.createSQLQuery("SELECT * from agents where uuid = '" + agentIdentifier.getUuid() + "'").addEntity(Agent.class).uniqueResult();
            }
        });
    }
}
