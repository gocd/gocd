/*
 * Copyright 2017 ThoughtWorks, Inc.
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

package com.thoughtworks.go.server.dao;

import com.thoughtworks.go.domain.NullPlugin;
import com.thoughtworks.go.domain.Plugin;
import com.thoughtworks.go.server.cache.GoCache;
import com.thoughtworks.go.server.transaction.TransactionTemplate;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Restrictions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;

import java.util.List;

@Component
public class PluginSqlMapDao implements PluginDao {
    private SessionFactory sessionFactory;
    private TransactionTemplate transactionTemplate;
    private GoCache goCache;

    @Autowired
    public PluginSqlMapDao(SessionFactory sessionFactory, TransactionTemplate transactionTemplate, GoCache goCache) {
        this.sessionFactory = sessionFactory;
        this.transactionTemplate = transactionTemplate;
        this.goCache = goCache;
    }

    @Override
    public void saveOrUpdate(final Plugin plugin) {
        String cacheKey = cacheKeyForPluginSettings(plugin.getPluginId());
        synchronized (cacheKey) {
            transactionTemplate.execute(new TransactionCallbackWithoutResult() {
                @Override
                protected void doInTransactionWithoutResult(TransactionStatus status) {
                    sessionFactory.getCurrentSession().saveOrUpdate(plugin);
                    goCache.remove(cacheKey);
                }
            });
        }
    }

    @Override
    public Plugin findPlugin(final String pluginId) {
        String cacheKey = cacheKeyForPluginSettings(pluginId);
        Plugin plugin = (Plugin) goCache.get(cacheKey);
        if (plugin != null) {
            return plugin;
        }
        synchronized (cacheKey) {
            plugin = (Plugin) goCache.get(cacheKey);
            if (plugin != null) {
                return plugin;
            }

            plugin = (Plugin) transactionTemplate.execute(new TransactionCallback() {
                @Override
                public Object doInTransaction(TransactionStatus transactionStatus) {
                    return sessionFactory.getCurrentSession()
                            .createCriteria(Plugin.class)
                            .add(Restrictions.eq("pluginId", pluginId))
                            .setCacheable(true).uniqueResult();
                }
            });

            if (plugin != null) {
                goCache.put(cacheKey, plugin);
                return plugin;
            }
            goCache.remove(cacheKey);
            return new NullPlugin();
        }
    }

    String cacheKeyForPluginSettings(String pluginId) {
        return (getClass().getName() + "_plugin_settings_" + pluginId).intern();
    }

    // used in tests
    @Override
    public List<Plugin> getAllPlugins() {
        return (List<Plugin>) transactionTemplate.execute(new TransactionCallback() {
            @Override
            public Object doInTransaction(TransactionStatus transactionStatus) {
                return sessionFactory.getCurrentSession()
                        .createQuery("FROM Plugin")
                        .list();
            }
        });
    }

    // used in tests
    @Override
    public void deleteAllPlugins() {
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus status) {
                sessionFactory.getCurrentSession()
                        .createQuery("DELETE FROM Plugin")
                        .executeUpdate();
            }
        });
    }
}
