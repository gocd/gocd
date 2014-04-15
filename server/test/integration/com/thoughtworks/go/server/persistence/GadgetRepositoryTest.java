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
import java.util.Collection;
import java.util.List;

import com.thoughtworks.go.server.dao.DatabaseAccessHelper;
import com.thoughtworks.go.server.domain.oauth.GadgetOauthAccessToken;
import com.thoughtworks.go.server.domain.oauth.GadgetOauthAuthorizationCode;
import com.thoughtworks.go.server.domain.oauth.GadgetOauthClient;
import com.thoughtworks.go.server.gadget.GadgetDataSource;
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

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasItem;
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
public class GadgetRepositoryTest  {
    @Autowired private GadgetRepository repo;
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
    public void shouldHonorTransaction() throws Exception {
        final GadgetOauthClient client = new GadgetOauthClient("google.com", "google", "client_id", "client_secret");
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
            template.load(GadgetOauthClient.class, client.getId());
            fail("should have failed because transaction was not successful");
        } catch (Exception e) {
            assertThat(e, is(Matchers.<Object>instanceOf(HibernateObjectRetrievalFailureException.class)));
        }
    }

    @Test
    public void shouldFindGadgetOauthClientById() {
        GadgetOauthClient saved = new GadgetOauthClient("google.com", "google", "client_id", "client_secret");
        template.save(saved);
        GadgetDataSource.GadgetOauthClientDTO dto = repo.findGadgetsOauthClientById(saved.getId());
        assertThat(dto, not(nullValue()));
        assertThat(dto, is(saved.getDTO()));
    }

    @Test
    public void shouldFindAllGadgetsOauthClients() {
        GadgetOauthClient saved1 = new GadgetOauthClient("google.com", "google", "client_id", "client_secret");
        template.save(saved1);
        GadgetOauthClient saved2 = new GadgetOauthClient("yahoo.com", "yahoo", "client_id", "client_secret");
        template.save(saved2);
        GadgetOauthClient saved3 = new GadgetOauthClient("thoughtworks.com", "thoughtworks", "client_id", "client_secret");
        template.save(saved3);
        Collection<GadgetDataSource.GadgetOauthClientDTO> allDtos = repo.findAllGadgetsOauthClient();
        assertThat(allDtos.size(), is(3));
        assertThat(allDtos, hasItems(saved1.getDTO(), saved2.getDTO(), saved3.getDTO()));
    }

    @Test
    public void shouldFindAllOauthAccessTokenByUserId() {
        GadgetOauthClient google = new GadgetOauthClient("google.com", "google", "client_id", "client_secret");
        template.save(google);
        GadgetOauthAccessToken token1 = new GadgetOauthAccessToken("fooUser", google, "access_token", "refresh_token", 1000L);
        template.save(token1);

        GadgetOauthClient yahoo = new GadgetOauthClient("yahoo.com", "yahoo", "client_id", "client_secret");
        template.save(yahoo);
        GadgetOauthAccessToken token2 = new GadgetOauthAccessToken("fooUser", yahoo, "access_token", "refresh_token", 1000L);
        template.save(token2);

        GadgetOauthAccessToken token3 = new GadgetOauthAccessToken("barUser", google, "access_token", "refresh_token", 1000L);
        template.save(token3);

        List<GadgetDataSource.GadgetOauthAccessTokenDTO> foosTokens = new ArrayList<GadgetDataSource.GadgetOauthAccessTokenDTO>(repo.findAllOauthAccessTokenByUserId("fooUser"));
        assertThat(foosTokens.size(), is(2));
        assertThat(foosTokens, hasItems(token1.getDTO(), token2.getDTO()));

        Collection<GadgetDataSource.GadgetOauthAccessTokenDTO> barsTokens = repo.findAllOauthAccessTokenByUserId("barUser");
        assertThat(barsTokens.size(), is(1));
        assertThat(barsTokens, hasItem(token3.getDTO()));

        assertThat(repo.findAllOauthAccessTokenByUserId("bazUser").size(), is(0));
    }

    @Test
    public void shouldfindOauthAccessTokensForClientAndUserId() {
        GadgetOauthClient google = new GadgetOauthClient("google.com", "google", "client_id", "client_secret");
        template.save(google);
        GadgetOauthAccessToken token1 = new GadgetOauthAccessToken("fooUser", google, "access_token", "refresh_token", 1000L);
        template.save(token1);

        GadgetOauthClient yahoo = new GadgetOauthClient("yahoo.com", "yahoo", "client_id", "client_secret");
        template.save(yahoo);
        GadgetOauthAccessToken token2 = new GadgetOauthAccessToken("fooUser", yahoo, "access_token", "refresh_token", 1000L);
        template.save(token2);

        GadgetDataSource.GadgetOauthAccessTokenDTO tokenDTO1 = repo.findOauthAccessTokensForClientAndUserId(google.getId(), "fooUser");
        assertThat(tokenDTO1, is(token1.getDTO()));

        GadgetDataSource.GadgetOauthAccessTokenDTO tokenDTO2 = repo.findOauthAccessTokensForClientAndUserId(yahoo.getId(), "fooUser");
        assertThat(tokenDTO2, is(token2.getDTO()));


        assertThat(repo.findOauthAccessTokensForClientAndUserId(google.getId(), "cryBaby"), is(nullValue()));
    }

//    @Test
//    public void shouldFindOauthAccessTokenByUserId() {
//        GadgetOauthClient google = new GadgetOauthClient("google.com", "google", "client_id", "client_secret");
//        template.save(google);
//        GadgetOauthAccessToken token = new GadgetOauthAccessToken("fooUser", google, "access_token", "refresh_token", 1000);
//        template.save(token);
//
//        GadgetOauthClient yahoo = new GadgetOauthClient("yahoo.com", "yahoo", "client_id", "client_secret");
//        template.save(yahoo);
//        GadgetOauthAccessToken token2 = new GadgetOauthAccessToken("fooUser", yahoo, "access_token", "refresh_token", 1000);
//        template.save(token2);
//
//        GadgetOauthAccessToken token3 = new GadgetOauthAccessToken("barUser", yahoo, "access_token", "refresh_token", 1000);
//        template.save(token3);
//
//        Collection<GadgetDataSource.GadgetOauthAccessTokenDTO> tokens = repo.findOauthAccessTokenByUserId("fooUser");
//
//        assertThat(tokens.size(), is(2));
//        assertThat(tokens, hasItems(token.getDTO(), token2.getDTO()));
//
//        tokens = repo.findOauthAccessTokenByUserId("barUser");
//        assertThat(tokens.size(), is(1));
//        assertThat(tokens, hasItem(token3.getDTO()));
//
//        tokens = repo.findOauthAccessTokenByUserId("bazUser");
//        assertThat(tokens.size(), is(0));
//    }

    @Test
    public void shouldFindGadgetsOauthClientByServiceName() {
        GadgetOauthClient saved1 = new GadgetOauthClient("google.com", "google", "client_id", "client_secret");
        template.save(saved1);
        GadgetDataSource.GadgetOauthClientDTO dto = repo.findGadgetsOauthClientByServiceName("froogle");
        assertThat(dto, nullValue());
        dto = repo.findGadgetsOauthClientByServiceName("google");
        assertThat(dto, not(nullValue()));
        assertThat(dto, is(saved1.getDTO()));
    }

    @Test
    public void shouldFindGadgetsOauthClientByOauthAuthorizeUrl() {
        GadgetOauthClient saved1 = new GadgetOauthClient("google.com", "google", "client_id", "client_secret");
        template.save(saved1);
        GadgetDataSource.GadgetOauthClientDTO dto = repo.findGadgetsOauthClientByOauthAuthorizeUrl("google.com");
        assertThat(dto, not(nullValue()));
        assertThat(dto, is(saved1.getDTO()));
    }

    @Test
    public void shouldSaveGadgetsOauthClient() throws IllegalAccessException {
        GadgetOauthClient client = new GadgetOauthClient("google.com", "google", "client_id", "client_secret");
        repo.saveGadgetsOauthClient(OauthRepositoryTest.attrMap(client.getDTO()));
        List allClients = template.find("from GadgetOauthClient");
        assertThat(allClients.size(), is(1));
        GadgetOauthClient latestClient = (GadgetOauthClient) allClients.get(0);
        assertThat(latestClient, not(nullValue()));
        assertThat(latestClient.getId(), greaterThan(0l));
        client.setId(latestClient.getId());
        assertThat(latestClient.getDTO(), is(client.getDTO()));
    }

    @Test
    public void shouldFindAllOauthAccessTokenByGadgetsOauthClientId() {
        GadgetOauthClient google = new GadgetOauthClient("google.com", "google", "client_id", "client_secret");
        template.save(google);
        GadgetOauthAccessToken token1 = new GadgetOauthAccessToken("fooUser", google, "access_token", "refresh_token", 1000L);
        template.save(token1);

        GadgetOauthClient yahoo = new GadgetOauthClient("yahoo.com", "yahoo", "client_id", "client_secret");
        template.save(yahoo);
        GadgetOauthAccessToken token2 = new GadgetOauthAccessToken("fooUser", yahoo, "access_token", "refresh_token", 1000L);
        template.save(token2);

        GadgetOauthAccessToken token3 = new GadgetOauthAccessToken("barUser", yahoo, "access_token", "refresh_token", 1000L);
        template.save(token3);

        Collection<GadgetDataSource.GadgetOauthAccessTokenDTO> googleTokens = repo.findAllOauthAccessTokenByGadgetsOauthClientId(google.getId());
        assertThat(googleTokens.size(), is(1));
        assertThat(new ArrayList(googleTokens).get(0), not(nullValue()));
        assertThat(googleTokens, hasItem(token1.getDTO()));

        Collection<GadgetDataSource.GadgetOauthAccessTokenDTO> yahooTokens = repo.findAllOauthAccessTokenByGadgetsOauthClientId(yahoo.getId());
        assertThat(yahooTokens.size(), is(2));
        assertThat(new ArrayList(yahooTokens).get(0), not(nullValue()));
        assertThat(new ArrayList(yahooTokens).get(1), not(nullValue()));
        assertThat(yahooTokens, hasItem(token2.getDTO()));
        assertThat(yahooTokens, hasItem(token3.getDTO()));
    }

    @Test
    public void shouldFindAllOauthAuthorizationCodeByUserId() {
        GadgetOauthClient google = new GadgetOauthClient("google.com", "google", "client_id", "client_secret");
        template.save(google);
        GadgetOauthAuthorizationCode authCode1 = new GadgetOauthAuthorizationCode("fooUser", google, "code", 1000l);
        template.save(authCode1);

        GadgetOauthClient yahoo = new GadgetOauthClient("yahoo.com", "yahoo", "client_id", "client_secret");
        template.save(yahoo);
        GadgetOauthAuthorizationCode authCode2 = new GadgetOauthAuthorizationCode("fooUser", yahoo, "code", 1000l);
        template.save(authCode2);
        GadgetOauthAuthorizationCode authCode3 = new GadgetOauthAuthorizationCode("barUser", yahoo, "code", 1000l);
        template.save(authCode3);

        Collection<GadgetDataSource.GadgetOauthAuthorizationCodeDTO> fooAuthCodes = repo.findAllOauthAuthorizationCodeByUserId("fooUser");

        assertThat(fooAuthCodes.size(), is(2));
        assertThat(new ArrayList(fooAuthCodes).get(0), not(nullValue()));
        assertThat(new ArrayList(fooAuthCodes).get(1), not(nullValue()));
        assertThat(fooAuthCodes, hasItems(authCode1.getDTO(), authCode2.getDTO()));

        Collection<GadgetDataSource.GadgetOauthAuthorizationCodeDTO> yahooAuthCodes = repo.findAllOauthAuthorizationCodeByUserId("barUser");
        assertThat(yahooAuthCodes.size(), is(1));
        assertThat(new ArrayList(yahooAuthCodes).get(0), not(nullValue()));
        assertThat(yahooAuthCodes, hasItem(authCode3.getDTO()));
    }

    @Test
    public void shouldFindAllOauthAuthorizationCodeByGadgetsOauthClientId() {
        GadgetOauthClient google = new GadgetOauthClient("google.com", "google", "client_id", "client_secret");
        template.save(google);
        GadgetOauthAuthorizationCode authCode1 = new GadgetOauthAuthorizationCode("fooUser", google, "code", 1000l);
        template.save(authCode1);

        GadgetOauthClient yahoo = new GadgetOauthClient("yahoo.com", "yahoo", "client_id", "client_secret");
        template.save(yahoo);
        GadgetOauthAuthorizationCode authCode2 = new GadgetOauthAuthorizationCode("fooUser", yahoo, "code", 1000l);
        template.save(authCode2);
        GadgetOauthAuthorizationCode authCode3 = new GadgetOauthAuthorizationCode("barUser", yahoo, "code", 1000l);
        template.save(authCode3);

        Collection<GadgetDataSource.GadgetOauthAuthorizationCodeDTO> googleAuthCodes = repo.findAllOauthAuthorizationCodeByGadgetsOauthClientId(google.getId());
        assertThat(googleAuthCodes.size(), is(1));
        assertThat(new ArrayList(googleAuthCodes).get(0), not(nullValue()));
        assertThat(googleAuthCodes, hasItem(authCode1.getDTO()));

        Collection<GadgetDataSource.GadgetOauthAuthorizationCodeDTO> yahooAuthCodes = repo.findAllOauthAuthorizationCodeByGadgetsOauthClientId(yahoo.getId());
        assertThat(new ArrayList(yahooAuthCodes).get(0), not(nullValue()));
        assertThat(new ArrayList(yahooAuthCodes).get(1), not(nullValue()));
        assertThat(yahooAuthCodes, hasItems(authCode2.getDTO(), authCode3.getDTO()));
    }

    @Test
    public void shouldFindAuthorizationCodesForClientAndUserId() {
        GadgetOauthClient google = new GadgetOauthClient("google.com", "google", "client_id", "client_secret");
        template.save(google);
        GadgetOauthAuthorizationCode authCode1 = new GadgetOauthAuthorizationCode("fooUser", google, "code", 1000l);
        template.save(authCode1);

        GadgetOauthClient yahoo = new GadgetOauthClient("yahoo.com", "yahoo", "client_id", "client_secret");
        template.save(yahoo);
        GadgetOauthAuthorizationCode authCode2 = new GadgetOauthAuthorizationCode("fooUser", yahoo, "code", 1000l);
        template.save(authCode2);
        GadgetOauthAuthorizationCode authCode3 = new GadgetOauthAuthorizationCode("barUser", yahoo, "code", 1000l);
        template.save(authCode3);

        GadgetDataSource.GadgetOauthAuthorizationCodeDTO fooGoogleAuthCode = repo.findAuthorizationCodesForClientAndUserId(google.getId(), "fooUser");
        assertThat(fooGoogleAuthCode, is(authCode1.getDTO()));

        GadgetDataSource.GadgetOauthAuthorizationCodeDTO fooYahooAuthCode = repo.findAuthorizationCodesForClientAndUserId(yahoo.getId(), "fooUser");
        assertThat(fooYahooAuthCode, is(authCode2.getDTO()));
        GadgetDataSource.GadgetOauthAuthorizationCodeDTO barYahooAuthCode = repo.findAuthorizationCodesForClientAndUserId(yahoo.getId(), "barUser");
        assertThat(barYahooAuthCode, is(authCode3.getDTO()));

        GadgetDataSource.GadgetOauthAuthorizationCodeDTO noAuthCode = repo.findAuthorizationCodesForClientAndUserId(google.getId(), "nonExistantUser");
        assertThat(noAuthCode, is(nullValue()));
    }

    @Test
    public void shouldSaveGadgetOauthAccessToken() throws IllegalAccessException {
        GadgetOauthClient google = new GadgetOauthClient("google.com", "google", "client_id", "client_secret");
        template.save(google);
        GadgetOauthAccessToken token = new GadgetOauthAccessToken("fooUser", google, "access_token", "refresh_token", 1000L);
        GadgetDataSource.GadgetOauthAccessTokenDTO tokenDto = repo.saveOauthAccessToken(OauthRepositoryTest.attrMap(token));
        GadgetOauthAccessToken accessToken = (GadgetOauthAccessToken) template.load(GadgetOauthAccessToken.class, tokenDto.getId());
        assertThat(accessToken.getId(), greaterThan(0l));
        assertThat(accessToken.getDTO(), is(tokenDto));
    }

    @Test
    public void shouldSaveGadgetOauthAuthorizationCode() throws IllegalAccessException {
        GadgetOauthClient google = new GadgetOauthClient("google.com", "google", "client_id", "client_secret");
        template.save(google);
        GadgetOauthAuthorizationCode authCode = new GadgetOauthAuthorizationCode("fooUser", google, "code", 1000L);
        GadgetDataSource.GadgetOauthAuthorizationCodeDTO authCodeDTO = repo.saveOauthAuthorizationCode(OauthRepositoryTest.attrMap(authCode));
        GadgetOauthAuthorizationCode loadedAuthCode = (GadgetOauthAuthorizationCode) template.load(GadgetOauthAuthorizationCode.class, authCodeDTO.getId());
        assertThat(loadedAuthCode.getId(), greaterThan(0l));
        assertThat(loadedAuthCode.getDTO(), is(authCodeDTO));
    }

    @Test
    public void shouldDeleteGadgetOauthClient() throws IllegalAccessException {
        GadgetOauthClient google = new GadgetOauthClient("google.com", "google", "client_id", "client_secret");
        template.save(google);

        GadgetOauthClient yahoo = new GadgetOauthClient("yahoo.com", "yahoo", "client_id", "client_secret");
        template.save(yahoo);

        repo.deleteGadgetsOauthClient(google.getId());
        Collection<GadgetDataSource.GadgetOauthClientDTO> oauthClients = repo.findAllGadgetsOauthClient();
        assertThat(oauthClients.size(), is(1));
        assertThat(oauthClients, hasItem(yahoo.getDTO()));
    }

    @Test
    public void shouldDeleteGadgetOauthAccessTokens() throws IllegalAccessException {
        GadgetOauthClient google = new GadgetOauthClient("google.com", "google", "client_id", "client_secret");
        template.save(google);

        GadgetOauthAccessToken token1 = new GadgetOauthAccessToken("fooUser", google, "access_token", "refresh_token", 1000L);
        GadgetDataSource.GadgetOauthAccessTokenDTO fooTokenDto = repo.saveOauthAccessToken(OauthRepositoryTest.attrMap(token1));


        GadgetOauthAccessToken token2 = new GadgetOauthAccessToken("barUser", google, "access_token", "refresh_token", 1000L);
        GadgetDataSource.GadgetOauthAccessTokenDTO barTokenDto = repo.saveOauthAccessToken(OauthRepositoryTest.attrMap(token2));

        repo.deleteOauthAccessToken(fooTokenDto.getId());

        Collection<GadgetDataSource.GadgetOauthAccessTokenDTO> tokens = repo.findAllOauthAccessTokenByGadgetsOauthClientId(google.getId());
        assertThat(tokens.size(), is(1));
        assertThat(tokens, hasItem(barTokenDto));
    }

    @Test
    public void shouldDeleteGadgetOauthAuthorizationCodes() throws IllegalAccessException {
        GadgetOauthClient google = new GadgetOauthClient("google.com", "google", "client_id", "client_secret");
        template.save(google);

        GadgetOauthAuthorizationCode authCode1 = new GadgetOauthAuthorizationCode("fooUser", google, "code", 1000l);
        template.save(authCode1);

        GadgetOauthAuthorizationCode authCode2 = new GadgetOauthAuthorizationCode("barUser", google, "code", 1000l);
        template.save(authCode2);

        repo.deleteOauthAuthorizationCode(authCode1.getId());

        Collection<GadgetDataSource.GadgetOauthAuthorizationCodeDTO> authorizationCodes = repo.findAllOauthAuthorizationCodeByGadgetsOauthClientId(google.getId());
        assertThat(authorizationCodes.size(), is(1));
        assertThat(authorizationCodes, hasItem(authCode2.getDTO()));
    }

}
