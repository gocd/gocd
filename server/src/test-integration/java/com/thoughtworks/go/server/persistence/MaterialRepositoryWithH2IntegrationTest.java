/*
 * Copyright 2020 ThoughtWorks, Inc.
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
package com.thoughtworks.go.server.persistence;

import java.sql.SQLException;

import com.googlecode.junit.ext.RunIf;
import com.thoughtworks.go.config.materials.AbstractMaterial;
import com.thoughtworks.go.config.materials.mercurial.HgMaterial;
import com.thoughtworks.go.junitext.DatabaseChecker;
import com.thoughtworks.go.junitext.GoJUnitExtSpringRunner;
import com.thoughtworks.go.server.cache.GoCache;
import com.thoughtworks.go.server.dao.DatabaseAccessHelper;
import org.apache.commons.codec.binary.Hex;
import org.hibernate.HibernateException;
import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.test.context.ContextConfiguration;

import static java.lang.String.format;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

@RunWith(GoJUnitExtSpringRunner.class)
@ContextConfiguration(locations = {
        "classpath:/applicationContext-global.xml",
        "classpath:/applicationContext-dataLocalAccess.xml",
        "classpath:/testPropertyConfigurer.xml",
        "classpath:/spring-all-servlet.xml",
})
public class MaterialRepositoryWithH2IntegrationTest {
    @Autowired MaterialRepository repo;
    @Autowired
    GoCache goCache;
    @Autowired DatabaseAccessHelper dbHelper;
    private HibernateTemplate originalTemplate;

    @Before
    public void setUp() throws Exception {
        originalTemplate = repo.getHibernateTemplate();
        dbHelper.onSetUp();
        goCache.clear();
    }

    @After
    public void tearDown() throws Exception {
        repo.setHibernateTemplate(originalTemplate);
        dbHelper.onTearDown();
    }

    @Test
    @RunIf(value = DatabaseChecker.class, arguments = {DatabaseChecker.H2})
    public void materialFingerprintShouldUseTheHashAlgoritmInMigration47() throws Exception {
        final HgMaterial material = new HgMaterial("url", null);
        byte[] fingerprint = (byte[]) repo.getHibernateTemplate().execute(new HibernateCallback() {
            @Override
            public Object doInHibernate(Session session) throws HibernateException, SQLException {
                String pattern = format("'type=%s%surl=%s'", material.getType(), AbstractMaterial.FINGERPRINT_DELIMITER, material.getUrl());
                SQLQuery query = session.createSQLQuery(format("CALL HASH('SHA256', STRINGTOUTF8(%s), 1)", pattern));
                return query.uniqueResult();
            }
        });
        assertThat(Hex.encodeHexString(fingerprint), is(material.getFingerprint()));
    }

    @Test
    @RunIf(value = DatabaseChecker.class, arguments = {DatabaseChecker.H2})
    public void shouldPreventInsertionOfMaterialsWithSameFingerprint() throws Exception {
        try {
            dbHelper.execute("INSERT into Materials(type, flyweightName, fingerprint) VALUES ('DependencyMaterial', 'bar', 'foo')");
            dbHelper.execute("INSERT into Materials(type, flyweightName, fingerprint) VALUES ('SvnMaterial', 'baz', 'foo')");
            fail("should have failed");
        }
        catch (SQLException passed) {
            assertThat(passed.getMessage(), containsString("Unique index or primary key violation"));
            assertThat(passed.getMessage(), containsString("PUBLIC.MATERIALS(FINGERPRINT)"));
        }
    }

}
