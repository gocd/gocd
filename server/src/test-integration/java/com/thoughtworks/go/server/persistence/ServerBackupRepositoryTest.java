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

import java.util.Date;

import com.thoughtworks.go.server.dao.DatabaseAccessHelper;
import com.thoughtworks.go.server.domain.ServerBackup;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        "classpath:WEB-INF/applicationContext-global.xml",
        "classpath:WEB-INF/applicationContext-dataLocalAccess.xml",
        "classpath:testPropertyConfigurer.xml"
})
public class ServerBackupRepositoryTest {

    @Autowired ServerBackupRepository repository;
    @Autowired
    DatabaseAccessHelper dbHelper;

    @Before
    public void setup() throws Exception {
        dbHelper.onSetUp();
    }

    @After
    public void teardown() throws Exception {
        dbHelper.onTearDown();
    }

    @Test
    public void shouldReturnTheLastBackupTime() {
        Date time = new Date();
        repository.save(new ServerBackup("file_path", time, "user"));
        Date latest = new Date(time.getTime() + 10000);
        repository.save(new ServerBackup("file_path", latest, "user"));

        ServerBackup serverBackup = repository.lastBackup();

        assertEquals(new ServerBackup("file_path", latest, "user"), serverBackup);
    }

    @Test
    public void shouldReturnTheUserNameWhichTriggeredTheLastBackup() {
        Date time = new Date();
        repository.save(new ServerBackup("file_path", time, "loser"));
        repository.save(new ServerBackup("file_path", time, "new_loser"));

        assertEquals(new ServerBackup("file_path", time, "new_loser"), repository.lastBackup());
    }

    @Test
    public void shouldReturnNullWhenThereAreNoEntries() {
        assertThat(repository.lastBackup(), is(nullValue()));
    }
}
