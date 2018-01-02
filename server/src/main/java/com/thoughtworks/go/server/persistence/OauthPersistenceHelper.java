/*
 * Copyright 2016 ThoughtWorks, Inc.
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
 */

package com.thoughtworks.go.server.persistence;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.thoughtworks.go.server.domain.oauth.OauthDomainEntity;
import org.springframework.orm.hibernate3.HibernateTemplate;

/**
 * @understands
 */
public class OauthPersistenceHelper {
    private final HibernateTemplate template;

    public OauthPersistenceHelper(HibernateTemplate template) {
        this.template = template;
    }

    <T> T saveOrUpdateEntity(OauthDomainEntity<T> client) {
        this.template.saveOrUpdate(client);
        return client.getDTO();
    }

    void deleteEntity(Class<? extends OauthDomainEntity> type, long id) {
        List list = this.template.find(String.format("from %s where id = ?", type.getSimpleName()), id);
        if (list.size() > 0) {
            this.template.delete(list.get(0));
        }
    }

    <T> T entityByColumn(Class<? extends OauthDomainEntity<T>> type, String columnName, String columnValue) {
        List list = this.template.find(String.format("from %s where %s = ?", type.getSimpleName(), columnName), columnValue);
        return list.isEmpty() ? null : ((OauthDomainEntity<T>) list.get(0)).getDTO();
    }

    <T> Collection<T> listByColumn(Class<? extends OauthDomainEntity<T>> type, String columnName, Object columnValue) {
        List list = this.template.find(String.format("from %s where %s = ?", type.getSimpleName(), columnName), columnValue);
        return dtoFromDomain(list);
    }

    <T> Collection<T> listByColumn(Class<? extends OauthDomainEntity<T>> type, String columnName1, Object columnValue1, String columnName2, Object columnValue2) {
        List list = this.template.find(String.format("from %s where %s = ? and %s = ?", type.getSimpleName(), columnName1, columnName2), new Object[]{columnValue1, columnValue2});
        return dtoFromDomain(list);
    }

    <T> T loadDomainEntity(Class<T> entityClass, long id) {
        return this.template.load(entityClass, id);
    }

    Collection dtoFromDomain(List<? extends OauthDomainEntity> list) {
        ArrayList dtos = new ArrayList();
        for (OauthDomainEntity oauthClient : list) {
            dtos.add(oauthClient.getDTO());
        }
        return dtos;
    }

}
