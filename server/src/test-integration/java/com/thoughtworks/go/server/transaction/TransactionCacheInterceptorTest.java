/*
 * Copyright Thoughtworks, Inc.
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
package com.thoughtworks.go.server.transaction;

import com.thoughtworks.go.config.GoConfigDao;
import com.thoughtworks.go.config.materials.mercurial.HgMaterial;
import com.thoughtworks.go.domain.MaterialInstance;
import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.domain.materials.ModifiedAction;
import com.thoughtworks.go.domain.materials.ModifiedFile;
import com.thoughtworks.go.server.cache.GoCache;
import com.thoughtworks.go.server.dao.DatabaseAccessHelper;
import com.thoughtworks.go.util.GoConfigFileHelper;
import com.thoughtworks.go.util.ReflectionUtil;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.hibernate4.support.HibernateDaoSupport;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {
        "classpath:/applicationContext-global.xml",
        "classpath:/applicationContext-dataLocalAccess.xml",
        "classpath:/testPropertyConfigurer.xml",
        "classpath:/spring-all-servlet.xml",
})
public class TransactionCacheInterceptorTest {
    @Autowired private GoConfigDao goConfigDao;
    @Autowired private GoCache goCache;
    @Autowired private DatabaseAccessHelper dbHelper;
    @Autowired private TransactionTemplate transactionTemplate;
    @Autowired private SessionFactory sessionFactory;

    private GoConfigFileHelper configHelper = new GoConfigFileHelper();
    private HibernateDaoSupport hibernateDaoSupport;
    private TransactionCacheAssertionUtil assertionUtil;


    @BeforeEach
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

    @AfterEach
    public void tearDown() throws Exception {
        dbHelper.onTearDown();
        configHelper.onTearDown();
    }

    @Test
    public void shouldOptOutOfCacheServing_forInsert() {
        final MaterialInstance materialInstance = hgInstance();
        assertionUtil.assertCacheBehaviourInTxn(() -> hibernateDaoSupport.getHibernateTemplate().save(materialInstance));
        assertThat(materialInstance.getId()).isGreaterThan(0L);
    }

    @Test
    public void shouldOptOutOfCacheServing_forUpdate() {
        final MaterialInstance materialInstance = savedHg();

        ReflectionUtil.setField(materialInstance, "url", "loser-name");
        assertionUtil.assertCacheBehaviourInTxn(() -> {
            hibernateDaoSupport.getHibernateTemplate().update(materialInstance);
            hibernateDaoSupport.getHibernateTemplate().flush();
        });
        assertThat((Object) ReflectionUtil.getField(hibernateDaoSupport.getHibernateTemplate().load(MaterialInstance.class, materialInstance.getId()), "url")).isEqualTo("loser-name");
    }

    @Test
    public void shouldOptOutOfCacheServing_forDelete() {
        final MaterialInstance materialInstance = savedHg();

        assertionUtil.assertCacheBehaviourInTxn(() -> hibernateDaoSupport.getHibernateTemplate().delete(materialInstance));

        try {
            hibernateDaoSupport.getHibernateTemplate().load(MaterialInstance.class, materialInstance.getId());
            fail("should have deleted the entity successfully");
        } catch (Exception e) {
            assertThat(e.getMessage()).contains("No row with the given identifier exists");
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

        assertionUtil.assertCacheBehaviourInTxn(() -> hibernateDaoSupport.getHibernateTemplate().update(mod));

        assertThat(mod.getId()).isGreaterThan(0);
        assertThat(foo_c.getId()).isGreaterThan(0L);
        assertThat(baz_c.getId()).isGreaterThan(0L);
    }

    @Test
    public void shouldOptOutOfCacheServing_forUpdateOnItemsInCollection() {
        final MaterialInstance materialInstance = savedHg();
        final Modification mod = new Modification("loser", "loser commiting a winner stroke", "foo@bar.com", new Date(), "123");
        mod.setMaterialInstance(materialInstance);

        ModifiedFile foo_c = mod.createModifiedFile("foo.c", "src", ModifiedAction.added);

        hibernateDaoSupport.getHibernateTemplate().save(mod);

        ReflectionUtil.setField(foo_c, "fileName", "foo_generated.c");

        assertionUtil.assertCacheBehaviourInTxn(() -> {
            hibernateDaoSupport.getHibernateTemplate().update(mod);
            hibernateDaoSupport.getHibernateTemplate().flush();
        });

        assertThat(hibernateDaoSupport.getHibernateTemplate().load(ModifiedFile.class, foo_c.getId()).getFileName()).isEqualTo("foo_generated.c");
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
