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
import com.thoughtworks.go.server.security.providers.LdapAuthenticationProvider;
import com.thoughtworks.go.util.GoConfigFileHelper;
import com.unboundid.ldif.LDIFRecord;
import org.apache.commons.lang.ArrayUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.Authentication;
import org.springframework.security.BadCredentialsException;
import org.springframework.security.GrantedAuthority;
import org.springframework.security.providers.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static com.thoughtworks.go.server.security.GoAuthority.ROLE_SUPERVISOR;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.collection.IsArrayContaining.hasItemInArray;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        "classpath:WEB-INF/applicationContext-global.xml",
        "classpath:WEB-INF/applicationContext-dataLocalAccess.xml",
        "classpath:WEB-INF/applicationContext-acegi-security.xml"
        }
)
public class LdapAuthenticationTest {
    @Autowired private GoConfigDao goConfigDao;
    @Autowired private LdapAuthenticationProvider ldapAuthenticationProvider;
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

    @Test
    public void shouldSupportAuthenticationIfLdapConfigExist() {
        assertThat(ldapAuthenticationProvider.supports(UsernamePasswordAuthenticationToken.class), is(true));
    }

    @Test
    public void shouldNotSupportAuthenticationIfNoLdapConfig() throws IOException {
        CONFIG_HELPER.initializeConfigFile();
        assertThat(ldapAuthenticationProvider.supports(UsernamePasswordAuthenticationToken.class), is(false));
    }

    @Test
    public void shouldAuthenticateForInvalidUser() {
        assertFailedAuthentication("invalid_user", "");
    }

    @Test
    public void shouldAuthenticateValidUser() throws Exception {
        ldapServer.addUser(employeesOrgUnit, "foleys", "some-password", "Shilpa Foley", "foleys@somecompany.com");

        assertAuthenticationOfValidAdminUser("foleys", "some-password");
    }

    @Test
    public void shouldReturnAdministratorRoleForSpecifiedLdapUser() throws Exception {
        CONFIG_HELPER.initializeConfigFile();
        CONFIG_HELPER.addLdapSecurityWithAdmin(LDAP_URL, MANAGER_DN, MANAGER_PASSWORD, SEARCH_BASE, SEARCH_FILTER, "foleys");

        shouldAuthenticateValidUser();
    }

    @Test
    public void commonLdapUserShouldOnlyHaveAuthorityOfUserAndNotAdmin() throws Exception {
        ldapServer.addUser(employeesOrgUnit, "foleys", "some-password", "Shilpa Foley", "foleys@somecompany.com");

        CONFIG_HELPER.initializeConfigFile();
        CONFIG_HELPER.addLdapSecurityWithAdmin(LDAP_URL, MANAGER_DN, MANAGER_PASSWORD, SEARCH_BASE, SEARCH_FILTER, "another_admin");

        Authentication authentication = new UsernamePasswordAuthenticationToken("foleys", "some-password");
        Authentication result = ldapAuthenticationProvider.authenticate(authentication);
        assertThat(result.isAuthenticated(), is(true));

        GrantedAuthority[] authorities = result.getAuthorities();
        assertThat("foleys should have only user authority. Found: " + ArrayUtils.toString(authorities), authorities.length, is(1));
        assertThat(authorities[0].getAuthority(), is("ROLE_USER"));
    }

    @Test
    public void shouldAuthenticateConcurrently() throws Exception {
        ldapServer.addUser(employeesOrgUnit, "foleys", "some-password", "Shilpa Foley", "foleys@somecompany.com");

        ExecutorService pool = Executors.newFixedThreadPool(100);
        List<Callable<String>> allCallables = new ArrayList<Callable<String>>();

        for (int i = 0; i < 100; i++) {
            final boolean even = i % 2 == 0;

            allCallables.add(new Callable<String>() {
                @Override
                public String call() throws Exception {
                    if (even) {
                        assertAuthenticationOfValidAdminUser("foleys", "some-password");
                    } else {
                        assertFailedAuthentication("invalid_user", "");
                    }

                    return "";
                }
            });
        }

        List<Future<String>> futures = pool.invokeAll(allCallables);
        pool.shutdown();

        boolean finishedWithoutTimeout = pool.awaitTermination(10, TimeUnit.SECONDS);
        assertThat(finishedWithoutTimeout, is(true));

        // Assert no exceptions, by getting result.
        for (Future<String> future : futures) {
            future.get();
        }
    }

    private void assertAuthenticationOfValidAdminUser(String userName, String password) {
        Authentication authentication = new UsernamePasswordAuthenticationToken(userName, password);
        Authentication result = ldapAuthenticationProvider.authenticate(authentication);
        assertThat(result.isAuthenticated(), is(true));

        assertThat(userName + " should have " + ROLE_SUPERVISOR + " authority", result.getAuthorities(),
                hasItemInArray(ROLE_SUPERVISOR.asAuthority())); // by default, every user is administrator
    }

    private void assertFailedAuthentication(String userName, String password) {
        Authentication authentication = new UsernamePasswordAuthenticationToken(userName, password);
        try {
            ldapAuthenticationProvider.authenticate(authentication);
            fail("Expected authentication to fail for user: " + userName);
        } catch (BadCredentialsException e) {
        }
    }
}
