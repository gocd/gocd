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
package com.thoughtworks.go.domain.materials;

import java.util.Date;
import java.util.HashMap;

import com.thoughtworks.go.config.materials.git.GitMaterial;
import com.thoughtworks.go.domain.MaterialInstance;
import com.thoughtworks.go.server.dao.DatabaseAccessHelper;
import com.thoughtworks.go.server.persistence.MaterialRepository;
import com.thoughtworks.go.server.transaction.TransactionCallback;
import com.thoughtworks.go.server.transaction.TransactionTemplate;
import com.thoughtworks.go.util.json.JsonHelper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.TransactionStatus;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        "classpath:/applicationContext-global.xml",
        "classpath:/applicationContext-dataLocalAccess.xml",
        "classpath:/testPropertyConfigurer.xml",
        "classpath:/spring-all-servlet.xml",
})
public class ModificationIntegrationTest {

    @Autowired private MaterialRepository materialRepository;
    @Autowired private TransactionTemplate transactionTemplate;

    @Autowired private DatabaseAccessHelper dbHelper;

    @Before
    public void setUp() throws Exception {
        dbHelper.onSetUp();
    }

    @After
    public void tearDown() throws Exception {
        dbHelper.onTearDown();
    }

    @Test
    public void shouldPersistAdditionalDataIntoModificationsTable() throws Exception {
        String revision = "revision";
        HashMap<String, String> additionalDataMap = new HashMap<>();
        additionalDataMap.put("key", "value");
        String additionalData = JsonHelper.toJsonString(additionalDataMap);
        final Modification modification = new Modification("user", "comment", "foo@bar.fooadsf", new Date(), revision, additionalData);
        final GitMaterial git = new GitMaterial("url", "branch");
        transactionTemplate.executeWithExceptionHandling(new TransactionCallback() {
            @Override
            public Object doInTransaction(TransactionStatus status) throws Exception {
                MaterialInstance gitInstance = materialRepository.findOrCreateFrom(git);
                materialRepository.saveModification(gitInstance, modification);
                return null;
            }
        });
        Modification actual = materialRepository.findModificationWithRevision(git, revision);
        /*
            Checking if time are the same, because actual has SQLTime whereas we send in java.util.Date. No idea how we were testing this ever
            because Modification#equals fails instance check (ShilpaG & Sachin)
         */
        assertThat(actual.getModifiedTime().getTime(), is(modification.getModifiedTime().getTime()));
        assertThat(actual.getAdditionalData(), is(additionalData));
        assertThat(actual.getAdditionalDataMap(), is(additionalDataMap));
    }
}
