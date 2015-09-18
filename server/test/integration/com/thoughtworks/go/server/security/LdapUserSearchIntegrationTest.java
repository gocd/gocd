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

package com.thoughtworks.go.server.security;

import com.thoughtworks.go.config.GoConfigDao;
import com.thoughtworks.go.domain.User;
import com.thoughtworks.go.util.GoConfigFileHelper;
import com.unboundid.ldif.LDIFRecord;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.List;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        "classpath:WEB-INF/applicationContext-global.xml",
        "classpath:WEB-INF/applicationContext-dataLocalAccess.xml",
        "classpath:WEB-INF/applicationContext-acegi-security.xml"
})
public class LdapUserSearchIntegrationTest  {
    @Autowired private LdapUserSearch ldapUserSearch;
    @Autowired private GoConfigDao goConfigDao;

    private static final GoConfigFileHelper CONFIG_HELPER = new GoConfigFileHelper();
    private InMemoryLdapServerForTests ldapServer;
    private LDIFRecord employeesOrgUnit;

    private static final int PORT = 12389;
    private static final String LDAP_URL = "ldap://localhost:" + PORT;
    private static final String BASE_DN = "dc=corp,dc=somecompany,dc=com";
    private static final String MANAGER_DN = "cn=Active Directory Ldap User,ou=SomeSystems,ou=Accounts,ou=Principal," + BASE_DN;
    private static final String MANAGER_PASSWORD = "some-password";
    private static final String SEARCH_BASE = "ou=Employees,ou=Company,ou=Principal," + BASE_DN;
    private static final String SEARCH_FILTER = "(sAMAccountName={0})";

    @Before
    public void setUp() throws Exception {
        CONFIG_HELPER.usingCruiseConfigDao(goConfigDao);
        CONFIG_HELPER.initializeConfigFile();
        CONFIG_HELPER.addLdapSecurity(LDAP_URL, MANAGER_DN, MANAGER_PASSWORD, SEARCH_BASE, SEARCH_FILTER);

        ldapServer = new InMemoryLdapServerForTests(BASE_DN, MANAGER_DN, MANAGER_PASSWORD).start(PORT);
        ldapServer.addOrganizationalUnit("Principal", "ou=Principal," + BASE_DN);
        ldapServer.addOrganizationalUnit("Company", "ou=Company,ou=Principal," + BASE_DN);
        employeesOrgUnit = ldapServer.addOrganizationalUnit("Employees", "ou=Employees,ou=Company,ou=Principal," + BASE_DN);
    }

    @After
    public void tearDown() throws Exception {
        ldapServer.stop();
    }

    @Test(timeout = 30 * 1000)
    public void shouldSearchUserByLastname() throws Exception {
        ldapServer.addUser(employeesOrgUnit, "foleys", "some-password", "Shilpa Foley", "foleys@somecompany.com");
        ldapServer.addUser(employeesOrgUnit, "fdas", "some-password", "First Das", "fdas@somecompany.com");
        ldapServer.addUser(employeesOrgUnit, "sdas", "some-password", "Second Das", "sdas@somecompany.com");

        List<User> users = ldapUserSearch.search("Foley");

        assertThat(users.size(), is(1));
        assertThat(users.get(0).getName(), is("foleys"));
        assertThat(users.get(0).getEmail(), is("foleys@somecompany.com"));
        assertThat(users.get(0).getDisplayName(), is("Shilpa Foley"));

        users = ldapUserSearch.search("Das");
        assertThat(users.size(), equalTo(2));
    }

    @Test(timeout = 30 * 1000)
    public void shouldSearchUserByFullName() throws Exception {
        ldapServer.addUser(employeesOrgUnit, "foleys", "some-password", "Shilpa Foley", "foleys@somecompany.com");

        List<User> users = ldapUserSearch.search("Shilpa Foley");
        assertThat(users.size(), is(1));
        assertThat(users.get(0).getName(), is("foleys"));
        assertThat(users.get(0).getEmail(), is("foleys@somecompany.com"));
        assertThat(users.get(0).getDisplayName(), is("Shilpa Foley"));
    }

    @Test(timeout = 30 * 1000)
    public void shouldSearchUserByLoginId() throws Exception {
        ldapServer.addUser(employeesOrgUnit, "foleys", "some-password", "Shilpa Foley", "foleys@somecompany.com");

        List users = ldapUserSearch.search("foleys");
        assertThat(users.size(), is(1));
    }

    @Test(timeout = 30 * 1000)
    public void shouldSearchUserByEmailAddress() throws Exception {
        ldapServer.addUser(employeesOrgUnit, "foleys", "some-password", "Shilpa Foley", "foleys@somecompany.com");

        List<User> users = ldapUserSearch.search("foleys@");

        assertThat(users.size(), is(1));
        assertThat(users.get(0).getName(), is("foleys"));
        assertThat(users.get(0).getEmail(), is("foleys@somecompany.com"));
    }

    @Test(timeout = 30 * 1000)
    public void shouldSearchUserByWildCard() throws Exception {
        ldapServer.addUser(employeesOrgUnit, "foleys", "some-password", "Shilpa Foley", "foleys@somecompany.com");
        ldapServer.addUser(employeesOrgUnit, "foleyt", "some-password", "Thilpa Foley", "foleyt@somecompany.com");
        ldapServer.addUser(employeesOrgUnit, "foleyj", "some-password", "Julpa Foley", "foleyj@somecompany.com");

        List users = ldapUserSearch.search("hilpa");
        assertThat(users.size(), equalTo(2));
    }

    @Test(timeout = 30 * 1000)
    public void shouldLimitUserSearchResults() throws Exception {
        addManyUsers(200);

        try {
            ldapUserSearch.search("somecompany");
            fail("Should have failed as number of results higher than allowed limit.");
        } catch (LdapUserSearch.NotAllResultsShownException e) {
            assertThat(e.getUsers().size(), is(100));
        }
    }

    private void addManyUsers(int numberOfUsersToAdd) throws Exception {
        for (int i = 0; i < numberOfUsersToAdd; i++) {
            ldapServer.addUser(employeesOrgUnit, "employee" + i, "some-password", "Employee Number " + i, "employee" + i + "@somecompany.com");
        }
    }
}
