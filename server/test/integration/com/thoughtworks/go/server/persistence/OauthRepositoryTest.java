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

import java.lang.reflect.Field;
import java.util.*;

import com.thoughtworks.go.domain.PersistentObject;
import com.thoughtworks.go.server.dao.DatabaseAccessHelper;
import com.thoughtworks.go.server.domain.oauth.OauthAuthorization;
import com.thoughtworks.go.server.domain.oauth.OauthClient;
import com.thoughtworks.go.server.domain.oauth.OauthToken;
import com.thoughtworks.go.server.oauth.OauthDataSource;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.hibernate3.HibernateObjectRetrievalFailureException;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static com.thoughtworks.go.util.DataStructureUtils.m;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.junit.matchers.JUnitMatchers.hasItems;
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        "classpath:WEB-INF/applicationContext-global.xml",
        "classpath:WEB-INF/applicationContext-dataLocalAccess.xml",
        "classpath:WEB-INF/applicationContext-acegi-security.xml"
})
public class OauthRepositoryTest {
    @Autowired private OauthRepository repo;
    @Autowired private DatabaseAccessHelper dbHelper;
    private HibernateTemplate template;

    @Before public void setUp() throws Exception {
        template = repo.getHibernateTemplate();
        dbHelper.onSetUp();
    }

    @After
    public void tearDown() throws Exception {
        dbHelper.onTearDown();
    }

    @Test
    public void shouldHonorTransaction() throws Exception{
        final OauthClient client = new OauthClient("mingle", "client_id", "client_secret", "http://something");
        try {
            repo.transaction(new Runnable() {
                public void run() {
                    template.save(client);
                    throw new RuntimeException("ouch! it failed.");
                }
            });
            fail("should have bubbled up transaction failing exception");
        } catch (Exception e) {
            assertThat(e.getMessage(), is("ouch! it failed."));
        }
        try {
            template.load(OauthClient.class, client.getId());
            fail("should have failed because transaction was not successful");
        } catch (Exception e) {
            assertThat(e, is(Matchers.<Object>instanceOf(HibernateObjectRetrievalFailureException.class)));
        }
    }

    @Test
    public void shouldFindOauthClientById() {
        OauthClient client = new OauthClient("mingle", "client_id", "client_secret", "http://something");
        template.save(client);
        OauthDataSource.OauthClientDTO dto = repo.findOauthClientById(client.getId());
        assertThat(dto, is(client.getDTO()));
    }

    @Test
    public void shouldFindOauthClientByName() {
        OauthClient client = new OauthClient("mingle", "client_id", "client_secret", "http://something");
        template.save(client);
        OauthDataSource.OauthClientDTO dto = repo.findOauthClientByName("mingle");
        assertThat(dto, is(client.getDTO()));
    }

    @Test
    public void shouldFindOauthClientByRedirectUri() {
        OauthClient client = new OauthClient("mingle", "client_id", "client_secret", "http://something");
        template.save(client);
        OauthDataSource.OauthClientDTO dto = repo.findOauthClientByRedirectUri("http://something");
        assertThat(dto, is(client.getDTO()));
    }

    @Test
    public void shouldHaveUniqueClientNames() {
        OauthClient client = new OauthClient("mingle", "client_id", "client_secret", "http://something");
        template.save(client);
        try {
            template.save(new OauthClient("mingle", "another_id", "super_secret", "http://something/another"));
            fail("Should not be able to add 2 clients with the same name");
        } catch (Exception expected) {
        }
    }

    @Test
    public void shouldFindAllOauthClients() {
        OauthClient mingle1 = new OauthClient("mingle09", "client_id_9", "client_secret_9", "http://something");
        OauthClient mingle2 = new OauthClient("mingle05", "client_id_5", "client_secret_5", "http://something-else");
        OauthClient go01 = new OauthClient("go01", "client_id_1", "client_secret_1", "http://fake-go-server");
        template.save(mingle1);
        template.save(mingle2);
        template.save(go01);

        Collection<OauthDataSource.OauthClientDTO> clients = repo.findAllOauthClient();

        assertThat(clients.size(), is(3));
        assertThat(clients, hasItems(mingle1.getDTO(), mingle2.getDTO(), go01.getDTO()));
    }

    @Test
    public void shouldFindOauthClientByClientId() {
        OauthClient mingle09 = new OauthClient("mingle09", "client_id_9", "client_secret_9", "http://something");
        template.save(mingle09);
        OauthDataSource.OauthClientDTO dto = repo.findOauthClientByClientId(String.valueOf("client_id_9"));
        assertThat(dto, is(mingle09.getDTO()));
        dto = repo.findOauthClientByClientId(String.valueOf("client_id_non_existent"));
        assertThat(dto, is(nullValue()));
    }

    @Test
    public void shouldSaveOauthClient() throws IllegalAccessException, NoSuchFieldException {
        OauthClient mingle09 = new OauthClient("mingle09", "client_id", "client_secret", "http://something");
        Map<String, String> map = attrMap(mingle09.getDTO());
        repo.saveOauthClient(map);
        OauthClient client = (OauthClient) template.find("from OauthClient where clientSecret = 'client_secret'").get(0);
        OauthDataSource.OauthClientDTO dto = client.getDTO();
        assertHasIdAndMatches(dto, mingle09.getDTO());
    }

    @Test
    public void shouldUpdateOauthClient() throws IllegalAccessException {
        OauthClient mingle09 = new OauthClient("mingle09", "client_id", "client_secret", "http://something");
        final Map map = attrMap(mingle09.getDTO());
        template.save(mingle09);
        map.put("name", "a different name");
        map.put("id", mingle09.getId());
        repo.transaction(
        new Runnable() {
            public void run() {
                repo.saveOauthClient(map);
            }});   
        OauthClient client = (OauthClient) template.load(OauthClient.class, mingle09.getId());
        assertThat(client.getDTO().getName(), is("a different name"));
    }

    @Test
    public void shouldDeleteOauthClient() {
        OauthClient mingle09 = new OauthClient("mingle09", "client_id", "client_secret", "http://something");
        template.save(mingle09);
        assertThat(template.find("from OauthClient").size(), is(1));
        repo.deleteOauthClient(mingle09.getId());
        assertThat(template.find("from OauthClient").size(), is(0));
    }

    @Test
    public void shouldNotBombWhenNoOauthClientToDelete() {
        assertThat(template.find("from OauthClient").size(), is(0));
        repo.deleteOauthClient(10);
    }

    @Test
    public void shouldDeleteOauthClientAndAssociatedApprovalsAndTokens() {
        OauthClient mingle09 = new OauthClient("mingle09", "client_id", "client_secret", "http://something");
        template.save(mingle09);
        template.save(new OauthAuthorization("foo@bar.com", mingle09, "code", 332333));
        template.save(new OauthToken("foo@bar.com", mingle09, "foo-access", "foo-refresh", 12345));
        assertThat(template.find("from OauthClient").size(), is(1));
        assertThat(template.find("from OauthAuthorization").size(), is(1));
        assertThat(template.find("from OauthToken").size(), is(1));
        repo.deleteOauthClient(mingle09.getId());
        assertThat(template.find("from OauthClient").size(), is(0));
        assertThat(template.find("from OauthAuthorization").size(), is(0));
        assertThat(template.find("from OauthToken").size(), is(0));
    }

    @Test
    public void shouldFindAllAuthorizationsByClientId() {
        OauthClient mingle09 = new OauthClient("mingle09", "client_id", "client_secret", "http://something");
        template.save(mingle09);
        OauthAuthorization authFoo = new OauthAuthorization("foo@bar.com", mingle09, "code", 332333);
        template.save(authFoo);
        OauthAuthorization authBar = new OauthAuthorization("bar@bar.com", mingle09, "code112", 334337);
        template.save(authBar);

        OauthClient mingle05 = new OauthClient("mingle05", "client_id_2", "client_secret_2", "http://something-else");
        template.save(mingle05);
        OauthAuthorization authBaz = new OauthAuthorization("baz@bar.com", mingle05, "code115", 334337);
        template.save(authBaz);

        Collection<OauthDataSource.OauthAuthorizationDTO> authorizations = repo.findAllOauthAuthorizationByOauthClientId("client_id");

        assertThat(authorizations.size(), is(2));

        assertThat(authorizations, hasItems(authFoo.getDTO(), authBar.getDTO()));
    }

    @Test
    public void shouldFindOauthAuthorizationById() {
        OauthClient mingle09 = new OauthClient("mingle09", "client_id", "client_secret", "http://something");
        template.save(mingle09);
        OauthAuthorization authFoo = new OauthAuthorization("foo@bar.com", mingle09, "code", 332333);
        template.save(authFoo);
        OauthDataSource.OauthAuthorizationDTO dto = repo.findOauthAuthorizationById(authFoo.getId());
        assertThat(dto, is(authFoo.getDTO()));
    }

    @Test
    public void shouldFindOauthAuthorizationByCode() {
        OauthClient mingle09 = new OauthClient("mingle09", "client_id", "client_secret", "http://something");
        template.save(mingle09);
        OauthAuthorization authFoo = new OauthAuthorization("foo@bar.com", mingle09, "code", 332333);
        template.save(authFoo);
        OauthDataSource.OauthAuthorizationDTO dto = repo.findOauthAuthorizationByCode("code");
        assertThat(dto, is(authFoo.getDTO()));
    }

    @Test
    public void shouldReturnNullWhenNoAuthFoundByCode() {
        OauthDataSource.OauthAuthorizationDTO dto = repo.findOauthAuthorizationByCode("some-non-existing-code");
        assertThat(dto, is(nullValue()));
    }

    @Test
    public void shouldSaveAuthorization() throws IllegalAccessException, NoSuchFieldException {
        OauthClient mingle09 = new OauthClient("mingle09", "client_id", "client_secret", "http://something");
        template.save(mingle09);
        OauthAuthorization authFoo = new OauthAuthorization("foo@bar.com", mingle09, "code", 332333);
        repo.saveOauthAuthorization(attrMap(authFoo.getDTO()));
        OauthDataSource.OauthAuthorizationDTO dto = ((OauthAuthorization) template.find("from OauthAuthorization where code = ? ", "code").get(0)).getDTO();

        assertHasIdAndMatches(dto, authFoo.getDTO());
    }

    @Test
    public void shouldUpdateAuthorization() throws IllegalAccessException, NoSuchFieldException {
        OauthClient mingle09 = new OauthClient("mingle09", "client_id", "client_secret", "http://something");
        template.save(mingle09);
        OauthAuthorization authFoo = new OauthAuthorization("foo@bar.com", mingle09, "code", 332333);
        template.save(authFoo);
        Map map = attrMap(authFoo.getDTO());

        map.put("id", authFoo.getId());
        map.put("code", "hello_world");

        repo.saveOauthAuthorization(map);

        OauthDataSource.OauthAuthorizationDTO dto = ((OauthAuthorization) template.load(OauthAuthorization.class, authFoo.getId())).getDTO();

        assertThat(dto.getCode(), is("hello_world"));
    }

    @Test
    public void shouldDeleteAuthorization() {
        OauthClient mingle09 = new OauthClient("mingle09", "client_id", "client_secret", "http://something");
        template.save(mingle09);
        OauthAuthorization authFoo = new OauthAuthorization("foo@bar.com", mingle09, "code", 332333);
        template.save(authFoo);

        assertThat(template.find("from OauthAuthorization").size(), is(1));

        repo.deleteOauthAuthorization(authFoo.getId());

        assertThat(template.find("from OauthAuthorization").size(), is(0));
    }

    @Test
    public void shouldNotBombWhenNoAuthorizationToDelete() {
        assertThat(template.find("from OauthAuthorization").size(), is(0));
        repo.deleteOauthAuthorization(10);
    }

    @Test
    public void shouldFindOauthTokenById() {
        OauthClient mingle09 = new OauthClient("mingle09", "client_id", "client_secret", "http://something");
        template.save(mingle09);
        OauthToken token = new OauthToken("foo@bar.com", mingle09, "access-token", "refresh-token", 23324324);
        template.save(token);

        OauthDataSource.OauthTokenDTO tokenDto = repo.findOauthTokenById(token.getId());

        assertThat(tokenDto, is(token.getDTO()));
    }

    @Test
    public void shouldFindAllOauthTokenByOauthClientId() {
        OauthClient mingle09 = new OauthClient("mingle09", "client_id", "client_secret", "http://something");
        template.save(mingle09);
        OauthToken tokenFoo = new OauthToken("foo@bar.com", mingle09, "access-token", "refresh-token", 23324324);
        template.save(tokenFoo);
        OauthToken tokenBar = new OauthToken("bar@bar.com", mingle09, "access-token-bar", "refresh-token-bar", 23324324);
        template.save(tokenBar);

        OauthClient mingle05 = new OauthClient("mingle05", "client_id_5", "client_secret_5", "http://something");
        template.save(mingle05);
        OauthToken tokenBaz = new OauthToken("baz@bar.com", mingle05, "access-token-baz", "refresh-token-baz", 23324324);
        template.save(tokenBaz);

        Collection<OauthDataSource.OauthTokenDTO> tokens = repo.findAllOauthTokenByOauthClientId("client_id");

        assertThat(tokens.size(), is(2));

        assertThat(tokens, hasItems(tokenFoo.getDTO(), tokenBar.getDTO()));
    }

    @Test
    public void shouldFindAllOauthTokenByUserId() {
        OauthClient mingle09 = new OauthClient("mingle09", "client_id", "client_secret", "http://something");
        template.save(mingle09);
        OauthToken fooTokenFor09 = new OauthToken("foo@bar.com", mingle09, "access-token", "refresh-token", 23324324);
        template.save(fooTokenFor09);
        OauthToken barTokenFor09 = new OauthToken("bar@bar.com", mingle09, "access-token-bar", "refresh-token-bar", 23324324);
        template.save(barTokenFor09);

        OauthClient mingle05 = new OauthClient("mingle05", "client_id_5", "client_secret_5", "http://something");
        template.save(mingle05);
        OauthToken fooTokenFor05 = new OauthToken("foo@bar.com", mingle05, "access-token-baz", "refresh-token-baz", 23324324);
        template.save(fooTokenFor05);

        Collection<OauthDataSource.OauthTokenDTO> tokens = repo.findAllOauthTokenByUserId("foo@bar.com");

        assertThat(tokens.size(), is(2));

        assertThat(tokens, hasItems(fooTokenFor09.getDTO(), fooTokenFor05.getDTO()));
    }

    @Test
    public void shouldfindOauthTokenByAccessToken() {
        OauthClient mingle09 = new OauthClient("mingle09", "client_id", "client_secret", "http://something");
        template.save(mingle09);
        OauthToken fooTokenFor09 = new OauthToken("foo@bar.com", mingle09, "access-token", "refresh-token", 23324324);
        template.save(fooTokenFor09);
        OauthToken barTokenFor09 = new OauthToken("bar@bar.com", mingle09, "access-token-bar", "refresh-token-bar", 23324324);
        template.save(barTokenFor09);

        OauthDataSource.OauthTokenDTO dto = repo.findOauthTokenByAccessToken("access-token");

        assertThat(dto, is(fooTokenFor09.getDTO()));
    }

    @Test
    public void shouldfindOauthTokenByRefreshToken() {
        OauthClient mingle09 = new OauthClient("mingle09", "client_id", "client_secret", "http://something");
        template.save(mingle09);
        OauthToken fooTokenFor09 = new OauthToken("foo@bar.com", mingle09, "access-token", "refresh-token", 23324324);
        template.save(fooTokenFor09);
        OauthToken barTokenFor09 = new OauthToken("bar@bar.com", mingle09, "access-token-bar", "refresh-token-bar", 23324324);
        template.save(barTokenFor09);

        OauthDataSource.OauthTokenDTO dto = repo.findOauthTokenByRefreshToken("refresh-token");

        assertThat(dto, is(fooTokenFor09.getDTO()));
    }

    @Test
    public void shouldSaveTokens() throws IllegalAccessException, NoSuchFieldException {
        OauthClient mingle09 = new OauthClient("mingle09", "client_id", "client_secret", "http://something");
        template.save(mingle09);
        OauthToken fooTokenFor09 = new OauthToken("foo@bar.com", mingle09, "access-token", "refresh-token", 23324324);
        repo.saveOauthToken(attrMap(fooTokenFor09.getDTO()));
        OauthDataSource.OauthTokenDTO dto = ((OauthToken) template.find("from OauthToken where accessToken = ? ", "access-token").get(0)).getDTO();

        assertHasIdAndMatches(dto, fooTokenFor09.getDTO());
    }

    @Test
    public void shouldUpdateTokens() throws IllegalAccessException, NoSuchFieldException {
        OauthClient mingle09 = new OauthClient("mingle09", "client_id", "client_secret", "http://something");
        template.save(mingle09);
        OauthToken fooTokenFor09 = new OauthToken("foo@bar.com", mingle09, "access-token", "refresh-token", 23324324);
        template.save(fooTokenFor09);
        Map map = attrMap(fooTokenFor09.getDTO());

        map.put("id", fooTokenFor09.getId());
        map.put("access_token", "hello_world");

        repo.saveOauthToken(map);

        OauthDataSource.OauthTokenDTO dto = ((OauthToken) template.load(OauthToken.class, fooTokenFor09.getId())).getDTO();

        assertThat(dto.getAccessToken(), is("hello_world"));
    }

    @Test
    public void shouldDeleteOauthToken() {
        OauthClient mingle09 = new OauthClient("mingle09", "client_id", "client_secret", "http://something");
        template.save(mingle09);
          OauthToken fooTokenFor09 = new OauthToken("foo@bar.com", mingle09, "access-token", "refresh-token", 23324324);
        template.save(fooTokenFor09);

        assertThat(template.find("from OauthToken").size(), is(1));

        repo.deleteOauthToken(fooTokenFor09.getId());

        assertThat(template.find("from OauthToken").size(), is(0));
    }

    @Test
    public void shouldDeleteAllTokensAndCodes() {
        OauthClient mingle09 = new OauthClient("mingle09", "client_id", "client_secret", "http://something");
        template.save(mingle09);
        OauthAuthorization authFoo = new OauthAuthorization("foo@bar.com", mingle09, "code", 332333);
        template.save(authFoo);
        OauthToken fooTokenFor09 = new OauthToken("foo@bar.com", mingle09, "access-token", "refresh-token", 23324324);
        template.save(fooTokenFor09);

        repo.deleteAllOauthGrants();

        assertThat(template.find("from OauthAuthorization").size(), is(0));
        assertThat(template.find("from OauthToken").size(), is(0));
    }

    @Test
    public void shouldNotBombWhenNoTokenToDelete() {
        assertThat(template.find("from OauthToken").size(), is(0));
        repo.deleteOauthToken(10);
    }

    @Test
    public void shouldSaveClientIfIdIsMissing() throws Exception {
        String name = "oauth";
        String clientId = "client_id";
        String clientSecret = "client_secret";
        String redirectUri = "http://something";
        Map<String, String> map = m("id", "", "name", name, "client_id", clientId, "client_secret", clientSecret, "redirect_uri", redirectUri);
        repo.saveClient(map);
        OauthClient client = (OauthClient) template.find("from OauthClient where clientSecret = '" + clientSecret + "'").get(0);
        OauthDataSource.OauthClientDTO dto = client.getDTO();
        assertThat(dto.getName(), is(name));
        assertThat(dto.getClientId(), is(clientId));
        assertThat(dto.getClientSecret(), is(clientSecret));
        assertThat(dto.getRedirectUri(), is(redirectUri));
        assertThat(dto.getId(), is(not(PersistentObject.NOT_PERSISTED)));
    }

    @Test
    public void shouldSaveAuthorization_ForEngineFlow() throws Exception {
        OauthClient client = new OauthClient("mingle09", "client_id", "client_secret", "http://something");
        template.save(client);
        long expiresAt = new Date().getTime();
        Map<String, String> attributes = m("authenticity_token", "eJkmGwpHh045A/h5uhme+4Pqdr+E8b+jgRq1+vt/s6M=", "authorize", "Yes", "client_id", String.valueOf(client.getDTO().getId()),
                "redirect_uri", "https://mingle05.thoughtworks.com/gadgets/oauthcallback", "response_type", "code",
                "state", "eyJvYXV0aF9hdXRob3JpemVfdXJsIjoiaHR0cHM6Ly8xOTIuMTY4Ljk5LjU5\nOjgxNTQvZ28vYWRtaW4vb2F1dGgvYXV0aG9yaXplIn0=",
                "code", "ABCD", "expires_at", expiresAt, "user_id", "1");
        OauthDataSource.OauthAuthorizationDTO dto = repo.saveAuthorization(attributes);
        assertThat(dto.getId(), is(not(Matchers.nullValue())));
        assertThat(dto.getClientId(), is(String.valueOf(client.getDTO().getId())));
        assertThat(dto.getExpiresAt(), is(expiresAt));
        assertThat(dto.getCode(), is("ABCD"));
        assertThat(dto.getOauthClientId(), is(String.valueOf(client.getDTO().getId())));
        assertThat(dto.getUserId(), is("1"));
    }

    @Test
    public void shouldSaveToken_ForEngineFlow() throws Exception {
        OauthClient client = new OauthClient("mingle09", "client_id", "client_secret", "http://something");
        template.save(client);
        String accessToken = UUID.randomUUID().toString();
        String refreshToken = UUID.randomUUID().toString();
        long expiresAt = new Date().getTime();
        Map<String, String> attributes = m("user_id", "1", "client_id", String.valueOf(client.getDTO().getId()), "access_token", accessToken,
                "refresh_token", refreshToken, "expires_at", expiresAt);
        OauthDataSource.OauthTokenDTO dto = repo.saveToken(attributes);
        assertThat(dto.getUserId(), is("1"));
    }

    static void assertHasIdAndMatches(Object loaded, Object unpersistentExpected) throws NoSuchFieldException, IllegalAccessException {
        assertThat(loaded, is(Matchers.<Object>instanceOf(unpersistentExpected.getClass())));
        Field id = loaded.getClass().getDeclaredField("id");
        id.setAccessible(true);
        assertThat((Long) id.get(loaded), is(greaterThan(0l)));

        for (Field field : loaded.getClass().getDeclaredFields()) {
            if (field.getName().equals("id")) {
                continue;
            }
            field.setAccessible(true);
            assertThat(field.get(loaded), is(field.get(unpersistentExpected)));
        }
    }

    static Map attrMap(Object dto) throws IllegalAccessException {
        Map map = new HashMap();
        for (Field field : dto.getClass().getDeclaredFields()) {
            field.setAccessible(true);
            map.put(camelToSnakeCase(field.getName()), field.get(dto));
        }
        return map;
    }

    static String camelToSnakeCase(String camelCased) {
        StringBuilder builder = new StringBuilder();
        for (Character part : camelCased.toCharArray()) {
            if (part.toString().matches("[A-Z]")) {
                builder.append("_");
            }
            builder.append(part.toString().toLowerCase());
        }
        return builder.toString();
    }
}