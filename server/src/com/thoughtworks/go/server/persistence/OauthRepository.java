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

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.sql.SQLException;

import com.thoughtworks.go.server.domain.oauth.OauthAuthorization;
import com.thoughtworks.go.server.domain.oauth.OauthClient;
import com.thoughtworks.go.server.domain.oauth.OauthDomainEntity;
import com.thoughtworks.go.server.domain.oauth.OauthToken;
import com.thoughtworks.go.server.oauth.OauthDataSource;
import com.thoughtworks.go.server.transaction.TransactionTemplate;
import org.hibernate.SessionFactory;
import org.hibernate.Session;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.hibernate3.support.HibernateDaoSupport;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.TransactionStatus;

/**
 * @understands persistence for data used for oauth support
 */
@Service
public class OauthRepository extends HibernateDaoSupport implements OauthDataSource {

    private TransactionTemplate txnTemplate;
    private final OauthPersistenceHelper persistenceHelper;

    @Autowired
    public OauthRepository(SessionFactory sessionFactory, TransactionTemplate txnTemplate) {
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

    public OauthClientDTO findOauthClientById(long id) {
        return persistenceHelper.loadDomainEntity(OauthClient.class, id).getDTO();
    }

    public Collection<OauthClientDTO> findAllOauthClient() {
        return persistenceHelper.dtoFromDomain((List<OauthClient>) getHibernateTemplate().find("from OauthClient"));
    }

    public OauthClientDTO findOauthClientByClientId(final String clientId) {
        return persistenceHelper.entityByColumn(OauthClient.class, "clientId", clientId);
    }

    public OauthClientDTO findOauthClientByName(String name) {
        return persistenceHelper.entityByColumn(OauthClient.class, "name", name);
    }

    public OauthClientDTO findOauthClientByRedirectUri(String redirectUri) {
        return persistenceHelper.entityByColumn(OauthClient.class, "redirectUri", redirectUri);
    }

    public OauthClientDTO saveOauthClient(Map attributes) {
        Long id = (Long) attributes.get("id");
        OauthClient oauthClient = new OauthClient();
        if ( id != null && id > 0) {
            oauthClient = persistenceHelper.loadDomainEntity(OauthClient.class, id);
        }
        oauthClient.setAttributes(attributes);
        return persistenceHelper.saveOrUpdateEntity(oauthClient);
    }

    public void deleteOauthClient(long id) {
        persistenceHelper.deleteEntity(OauthClient.class, id);
    }

    public Collection<OauthAuthorizationDTO> findAllOauthAuthorizationByOauthClientId(final String oauthClientId) {
        return persistenceHelper.listByColumn(OauthAuthorization.class, "oauthClient.clientId", oauthClientId);
    }

    public OauthAuthorizationDTO findOauthAuthorizationById(long id) {
        return loadAuthorization(id).getDTO();
    }

    public OauthAuthorizationDTO findOauthAuthorizationByCode(final String code) {
        return persistenceHelper.entityByColumn(OauthAuthorization.class, "code", code);
    }

    public OauthAuthorizationDTO saveOauthAuthorization(Map attributes) {
        OauthAuthorization authorization = new OauthAuthorization(attributes, persistenceHelper.loadDomainEntity(OauthClient.class, Long.parseLong((String) attributes.get("oauth_client_id"))));
        return persistenceHelper.saveOrUpdateEntity(authorization);
    }

    public void deleteOauthAuthorization(long id) {
        persistenceHelper.deleteEntity(OauthAuthorization.class, id);
    }

    public OauthTokenDTO findOauthTokenById(long id) {
        return ((OauthToken) getHibernateTemplate().load(OauthToken.class, id)).getDTO();
    }

    public Collection<OauthTokenDTO> findAllOauthTokenByOauthClientId(String oauthClientId) {
        return persistenceHelper.listByColumn(OauthToken.class, "oauthClient.clientId", oauthClientId);
    }

    public Collection<OauthTokenDTO> findAllOauthTokenByUserId(String userId) {
        return persistenceHelper.listByColumn(OauthToken.class, "userId", userId);
    }

    public OauthTokenDTO findOauthTokenByAccessToken(String accessToken) {
        return persistenceHelper.entityByColumn(OauthToken.class, "accessToken", accessToken);
    }

    public OauthTokenDTO findOauthTokenByRefreshToken(String refreshToken) {
        return persistenceHelper.entityByColumn(OauthToken.class, "refreshToken", refreshToken);
    }

    public OauthTokenDTO saveOauthToken(Map attributes) {
        OauthToken token = new OauthToken(attributes, persistenceHelper.loadDomainEntity(OauthClient.class, Long.parseLong((String) attributes.get("oauth_client_id"))));
        return persistenceHelper.saveOrUpdateEntity(token);
    }

    public void deleteUsersOauthGrants(final List<String> userIds) {
        txnTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override protected void doInTransactionWithoutResult(TransactionStatus status) {
                getHibernateTemplate().execute(new HibernateCallback() {
                    public Object doInHibernate(Session session) throws HibernateException, SQLException {
                        deleteEntitiesByUserIds(OauthAuthorization.class, session, userIds);
                        deleteEntitiesByUserIds(OauthToken.class, session, userIds);
                        return true;
                    }
                });
            }
        });
    }

    private <T> void deleteEntitiesByUserIds(Class<? extends OauthDomainEntity<T>> type, Session session, List<String> userIds) {
        Query query = session.createQuery(String.format("DELETE FROM %s WHERE userId IN (:userIds)", type.getSimpleName()));
        query.setParameterList("userIds", userIds);
        query.executeUpdate();
    }

    public void deleteOauthToken(long id) {
        persistenceHelper.deleteEntity(OauthToken.class, id);
    }

    public void deleteAllOauthGrants() {
        txnTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override protected void doInTransactionWithoutResult(TransactionStatus status) {
                getHibernateTemplate().bulkUpdate("DELETE OauthAuthorization");
                getHibernateTemplate().bulkUpdate("DELETE OauthToken");
            }
        });
    }

    private OauthAuthorization loadAuthorization(long id) {
        return persistenceHelper.loadDomainEntity(OauthAuthorization.class, id);
    }
}
