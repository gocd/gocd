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

package com.thoughtworks.go.server.transaction;

import java.util.Date;

import com.thoughtworks.go.config.GoConfigDao;
import com.thoughtworks.go.config.materials.mercurial.HgMaterial;
import com.thoughtworks.go.domain.MaterialInstance;
import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.domain.materials.ModifiedAction;
import com.thoughtworks.go.domain.materials.ModifiedFile;
import com.thoughtworks.go.server.cache.GoCache;
import com.thoughtworks.go.server.dao.DatabaseAccessHelper;
import com.thoughtworks.go.server.persistence.MaterialRepository;
import com.thoughtworks.go.util.GoConfigFileHelper;
import com.thoughtworks.go.util.ReflectionUtil;
import org.hibernate.SessionFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.hibernate3.support.HibernateDaoSupport;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        "classpath:WEB-INF/applicationContext-global.xml",
        "classpath:WEB-INF/applicationContext-dataLocalAccess.xml",
        "classpath:WEB-INF/applicationContext-acegi-security.xml"
})
public class TransactionCacheInterceptorTest {
    @Autowired private GoConfigDao goConfigDao;
    @Autowired private GoCache goCache;
    @Autowired private DatabaseAccessHelper dbHelper;
    @Autowired private TransactionTemplate transactionTemplate;
    @Autowired private TransactionSynchronizationManager synchronizationManager;
    @Autowired private SessionFactory sessionFactory;
    @Autowired private MaterialRepository materialRepository;

    private GoConfigFileHelper configHelper = new GoConfigFileHelper();
    private HibernateDaoSupport hibernateDaoSupport;
    private TransactionCacheAssertionUtil assertionUtil;


    @Before
    public void setUp() throws Exception {
        configHelper.usingCruiseConfigDao(goConfigDao);
        configHelper.onSetUp();
        dbHelper.onSetUp();
        goCache.clear();
        hibernateDaoSupport = new HibernateDaoSupport() {
        };
        hibernateDaoSupport.setSessionFactory(sessionFactory);
        assertionUtil = new TransactionCacheAssertionUtil(goCache, transactionTemplate);
    }

    @After
    public void tearDown() throws Exception {
        dbHelper.onTearDown();
        configHelper.onTearDown();
    }

    @Test
    public void shouldOptOutOfCacheServing_forInsert() {
        final MaterialInstance materialInstance = hgInstance();
        assertionUtil.assertCacheBehaviourInTxn(new TransactionCacheAssertionUtil.DoInTxn() {
            public void invoke() {
                hibernateDaoSupport.getHibernateTemplate().save(materialInstance);
            }
        });
        assertThat(materialInstance.getId(), greaterThan(0l));
    }

    @Test
    public void shouldOptOutOfCacheServing_forUpdate() {
        final MaterialInstance materialInstance = savedHg();

        ReflectionUtil.setField(materialInstance, "url", "loser-name");
        assertionUtil.assertCacheBehaviourInTxn(new TransactionCacheAssertionUtil.DoInTxn() {
            public void invoke() {
                hibernateDaoSupport.getHibernateTemplate().update(materialInstance);
                hibernateDaoSupport.getHibernateTemplate().flush();
            }
        });
        assertThat((String) ReflectionUtil.getField(hibernateDaoSupport.getHibernateTemplate().load(MaterialInstance.class, materialInstance.getId()), "url"), is("loser-name"));
    }

    @Test
    public void shouldOptOutOfCacheServing_forDelete() {
        final MaterialInstance materialInstance = savedHg();

        assertionUtil.assertCacheBehaviourInTxn(new TransactionCacheAssertionUtil.DoInTxn() {
            public void invoke() {
                hibernateDaoSupport.getHibernateTemplate().delete(materialInstance);
            }
        });

        try {
            hibernateDaoSupport.getHibernateTemplate().load(MaterialInstance.class, materialInstance.getId());
            fail("should have deleted the entity successfully");
        } catch (Exception e) {
            assertThat(e.getMessage(), containsString("No row with the given identifier exists"));
        }
    }

    @Test
    public void shouldOptOutOfCacheServing_forNewItemsInCollection() {
        final MaterialInstance materialInstance = savedHg();
        final Modification mod = new Modification("loser", "loser commiting a winner stroke", "foo@bar.com", new Date(), "123");
        mod.setMaterialInstance(materialInstance);
        hibernateDaoSupport.getHibernateTemplate().save(mod);

        ModifiedFile foo_c = mod.createModifiedFile("foo.c", "src", ModifiedAction.added);
        ModifiedFile bar_c = mod.createModifiedFile("bar.c", "src", ModifiedAction.deleted);
        ModifiedFile baz_c = mod.createModifiedFile("baz.c", "src", ModifiedAction.modified);

        assertionUtil.assertCacheBehaviourInTxn(new TransactionCacheAssertionUtil.DoInTxn() {
            public void invoke() {
                hibernateDaoSupport.getHibernateTemplate().update(mod);
            }
        });

        assertThat(mod.getId(), greaterThan(0l));
        assertThat(foo_c.getId(), greaterThan(0l));
        assertThat(baz_c.getId(), greaterThan(0l));
    }

    @Test
    public void shouldOptOutOfCacheServing_forUpdateOnItemsInCollection() {
        final MaterialInstance materialInstance = savedHg();
        final Modification mod = new Modification("loser", "loser commiting a winner stroke", "foo@bar.com", new Date(), "123");
        mod.setMaterialInstance(materialInstance);

        ModifiedFile foo_c = mod.createModifiedFile("foo.c", "src", ModifiedAction.added);

        hibernateDaoSupport.getHibernateTemplate().save(mod);

        ReflectionUtil.setField(foo_c, "fileName", "foo_generated.c");

        assertionUtil.assertCacheBehaviourInTxn(new TransactionCacheAssertionUtil.DoInTxn() {
            public void invoke() {
                hibernateDaoSupport.getHibernateTemplate().update(mod);
                hibernateDaoSupport.getHibernateTemplate().flush();
            }
        });

        assertThat(((ModifiedFile) hibernateDaoSupport.getHibernateTemplate().load(ModifiedFile.class, foo_c.getId())).getFileName(), is("foo_generated.c"));
    }

    private MaterialInstance savedHg() {
        MaterialInstance materialInstance = hgInstance();
        hibernateDaoSupport.getHibernateTemplate().save(materialInstance);
        return materialInstance;
    }

    private MaterialInstance hgInstance() {
        HgMaterial hgMaterial = new HgMaterial("http://google.com", null);
        return hgMaterial.createMaterialInstance();
    }
}
