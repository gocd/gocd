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
import java.util.Map;

import com.thoughtworks.go.server.domain.oauth.GadgetOauthAccessToken;
import com.thoughtworks.go.server.domain.oauth.GadgetOauthAuthorizationCode;
import com.thoughtworks.go.server.domain.oauth.GadgetOauthClient;
import com.thoughtworks.go.server.gadget.GadgetDataSource;
import com.thoughtworks.go.server.transaction.TransactionTemplate;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.hibernate3.support.HibernateDaoSupport;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;

@Service
public class GadgetRepository extends HibernateDaoSupport implements GadgetDataSource {
    private final OauthPersistenceHelper persistenceHelper;
    private TransactionTemplate txnTemplate;

    @Autowired
    public GadgetRepository(SessionFactory sessionFactory, TransactionTemplate txnTemplate) {
        this.txnTemplate = txnTemplate;
        setSessionFactory(sessionFactory);
        this.persistenceHelper = new OauthPersistenceHelper(getHibernateTemplate());
    }

    public void transaction(final Runnable txn) {
        txnTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override protected void doInTransactionWithoutResult(TransactionStatus status) {
                txn.run();
            }
        });
    }

    public GadgetOauthClientDTO findGadgetsOauthClientById(long id) {
        return persistenceHelper.loadDomainEntity(GadgetOauthClient.class, id).getDTO();
    }

    public Collection<GadgetOauthClientDTO> findAllGadgetsOauthClient() {
        return persistenceHelper.dtoFromDomain((List<GadgetOauthClient>) getHibernateTemplate().find("from GadgetOauthClient"));
    }

    public Collection<GadgetOauthAccessTokenDTO> findAllOauthAccessTokenByUserId(String userId) {
        return persistenceHelper.listByColumn(GadgetOauthAccessToken.class, "userId", userId);
    }

    public Collection<GadgetOauthAuthorizationCodeDTO> findAllOauthAuthorizationCodeByUserId(String userId) {
        return persistenceHelper.listByColumn(GadgetOauthAuthorizationCode.class, "userId", userId);
    }

    public GadgetOauthAccessTokenDTO findOauthAccessTokensForClientAndUserId(long gadgetsOauthClientId, String userId) {
        Collection<GadgetOauthAccessTokenDTO> tokenDTOs = persistenceHelper.listByColumn(GadgetOauthAccessToken.class, "gadgetsOauthClientId", gadgetsOauthClientId, "userId", userId);
        return tokenDTOs.isEmpty() ? null : new ArrayList<>(tokenDTOs).get(0);
    }

    public Collection<GadgetOauthAccessTokenDTO> findAllOauthAccessTokenByGadgetsOauthClientId(long gadgetOauthClientId) {
        return persistenceHelper.listByColumn(GadgetOauthAccessToken.class, "gadgetsOauthClientId", gadgetOauthClientId);
    }

    public Collection<GadgetOauthAuthorizationCodeDTO> findAllOauthAuthorizationCodeByGadgetsOauthClientId(long gadgetOauthClientId) {
        return persistenceHelper.listByColumn(GadgetOauthAuthorizationCode.class, "gadgetsOauthClientId", gadgetOauthClientId);
    }

    public GadgetOauthAuthorizationCodeDTO findAuthorizationCodesForClientAndUserId(long gadgetOauthClientId, String userId) {
        Collection<GadgetOauthAuthorizationCodeDTO> codeDTOs = persistenceHelper.listByColumn(GadgetOauthAuthorizationCode.class, "gadgetsOauthClientId", gadgetOauthClientId, "userId", userId);
        return codeDTOs.isEmpty() ? null : new ArrayList<>(codeDTOs).get(0);
    }

    public GadgetOauthClientDTO findGadgetsOauthClientByServiceName(String serviceName) {
        return persistenceHelper.entityByColumn(GadgetOauthClient.class, "serviceName", serviceName);
    }

    public GadgetOauthClientDTO findGadgetsOauthClientByOauthAuthorizeUrl(String oauthAuthorizeUrl) {
        return persistenceHelper.entityByColumn(GadgetOauthClient.class, "oauthAuthorizeUrl", oauthAuthorizeUrl);
    }

    public void deleteGadgetsOauthClient(long id) {
        persistenceHelper.deleteEntity(GadgetOauthClient.class, id);
    }

    public void deleteOauthAuthorizationCode(long id) {
        persistenceHelper.deleteEntity(GadgetOauthAuthorizationCode.class, id);
    }


    public void deleteOauthAccessToken(long id) {
        persistenceHelper.deleteEntity(GadgetOauthAccessToken.class, id);
    }

    public GadgetOauthClientDTO saveGadgetsOauthClient(Map attributes) {
        Long id = (Long) attributes.get("id");
        GadgetOauthClient client = new GadgetOauthClient();
        if ( id != null && id > 0) {
            client = persistenceHelper.loadDomainEntity(GadgetOauthClient.class, id);
        }
        client.setAttributes(attributes);
        return persistenceHelper.saveOrUpdateEntity(client);
    }

    public GadgetOauthAuthorizationCodeDTO saveOauthAuthorizationCode(Map attributes) {
        GadgetOauthAuthorizationCode auth = new GadgetOauthAuthorizationCode(attributes);
        return persistenceHelper.saveOrUpdateEntity(auth);
    }

    public GadgetOauthAccessTokenDTO saveOauthAccessToken(Map attributes) {
        GadgetOauthAccessToken token = new GadgetOauthAccessToken(attributes);
        return persistenceHelper.saveOrUpdateEntity(token);
    }
}
