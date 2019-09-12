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
import com.thoughtworks.go.server.transaction.TransactionTemplate;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.sql.Timestamp;
import java.util.Date;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

class UsageStatisticsReportingSqlMapDaoTest {
    @Mock
    private SessionFactory sessionFactory;
    @Mock
    private TransactionTemplate transactionTemplate;

    private UsageStatisticsReportingSqlMapDao reportingSqlMapDao;

    @BeforeEach
    void setUp() {
        initMocks(this);
        reportingSqlMapDao = new UsageStatisticsReportingSqlMapDao(sessionFactory, transactionTemplate);
    }

    @Test
    void shouldLoadUsageStatisticsReportingFromDBOnFirstCall() {
        UsageStatisticsReporting usageStatisticsReporting = new UsageStatisticsReporting()
                .setLastReportedAt(new Timestamp(new Date().getTime()))
                .setServerId("server-id");
        when(transactionTemplate.execute(any(org.springframework.transaction.support.TransactionCallback.class))).thenReturn(usageStatisticsReporting);

        UsageStatisticsReporting loaded = reportingSqlMapDao.load();

        assertThat(loaded, is(usageStatisticsReporting));
        verify(transactionTemplate, times(1)).execute(any(org.springframework.transaction.support.TransactionCallback.class));
    }

}
