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
package com.thoughtworks.go.server.dao;

import com.thoughtworks.go.domain.UsageStatisticsReporting;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.sql.Timestamp;
import java.util.Date;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        "classpath:/applicationContext-global.xml",
        "classpath:/applicationContext-dataLocalAccess.xml",
        "classpath:/testPropertyConfigurer.xml",
        "classpath:/spring-all-servlet.xml",
})
public class UsageStatisticsReportingSqlMapDaoIntegrationTest {
    @Autowired
    private UsageStatisticsReportingSqlMapDao usageStatisticsReportingSqlMapDao;
    @Autowired
    private DatabaseAccessHelper dbHelper;

    @Before
    public void setup() throws Exception {
        dbHelper.onSetUp();
    }

    @After
    public void teardown() throws Exception {
        dbHelper.onTearDown();
    }

    @Test
    public void shouldSaveServerInformation() throws Exception {
        String serverId = UUID.randomUUID().toString();
        Date statsUpdatedAt = new Date();
        UsageStatisticsReporting usageStatisticsReporting = new UsageStatisticsReporting()
                .setLastReportedAt(new Timestamp(statsUpdatedAt.getTime()))
                .setServerId(serverId);
        usageStatisticsReportingSqlMapDao.saveOrUpdate(usageStatisticsReporting);

        usageStatisticsReporting = usageStatisticsReportingSqlMapDao.load();
        assertEquals(usageStatisticsReporting.getServerId(), serverId);
        assertTrue(usageStatisticsReporting.persisted());
        assertThat(usageStatisticsReporting.getLastReportedAt().toInstant(), is(statsUpdatedAt.toInstant()));
    }

    @Test
    public void shouldUpdateServerInformation() throws Exception {
        String serverId = UUID.randomUUID().toString();
        UsageStatisticsReporting usageStatisticsReporting = new UsageStatisticsReporting()
                .setLastReportedAt(new Timestamp(new Date().getTime()))
                .setServerId(serverId);
        usageStatisticsReportingSqlMapDao.saveOrUpdate(usageStatisticsReporting);

        UsageStatisticsReporting toBeUpdated = usageStatisticsReportingSqlMapDao.load();
        toBeUpdated.setLastReportedAt(new Timestamp(new Date(DateTime.now().minusDays(2).getMillis()).getTime()));
        usageStatisticsReportingSqlMapDao.saveOrUpdate(toBeUpdated);

        UsageStatisticsReporting loaded = usageStatisticsReportingSqlMapDao.load();
        assertThat(loaded.getLastReportedAt().toInstant(), not(is(usageStatisticsReporting.getLastReportedAt().toInstant())));
        assertThat(loaded.getLastReportedAt().toInstant(), is(toBeUpdated.getLastReportedAt().toInstant()));
    }
}
