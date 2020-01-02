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
package com.thoughtworks.go.server.service.datasharing;

import com.thoughtworks.go.server.dao.DataSharingSettingsSqlMapDao;
import com.thoughtworks.go.server.domain.DataSharingSettings;
import com.thoughtworks.go.server.service.EntityHashingService;
import com.thoughtworks.go.server.transaction.TransactionSynchronizationManager;
import com.thoughtworks.go.server.transaction.TransactionTemplate;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import java.util.Date;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class DataSharingSettingsServiceTest {
    private DataSharingSettingsService sharingSettingsService;
    @Mock
    private DataSharingSettingsSqlMapDao dataSharingSettingsSqlMapDao;
    @Mock
    private EntityHashingService entityHashingService;
    @Mock
    private TransactionTemplate transactionTemplate;
    @Mock
    private TransactionSynchronizationManager transactionSynchronizationManager;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        sharingSettingsService = new DataSharingSettingsService(dataSharingSettingsSqlMapDao, transactionTemplate,
                transactionSynchronizationManager, entityHashingService);

        when(transactionTemplate.execute(any(TransactionCallback.class))).then(invocation -> {
            ((TransactionCallback) invocation.getArguments()[0]).doInTransaction(new SimpleTransactionStatus());
            return null;
        });
    }

    @Test
    public void shouldUpdateDataSharingSettings() throws Exception {
        boolean newConsent = false;
        String consentedBy = "Bob";
        ArgumentCaptor<DataSharingSettings> argumentCaptor = ArgumentCaptor.forClass(DataSharingSettings.class);

        sharingSettingsService.createOrUpdate(new DataSharingSettings(newConsent, consentedBy, new Date()));

        verify(dataSharingSettingsSqlMapDao).saveOrUpdate(argumentCaptor.capture());
        DataSharingSettings updatedDataSharingSettings = argumentCaptor.getValue();

        assertThat(updatedDataSharingSettings.allowSharing(), is(newConsent));
        assertThat(updatedDataSharingSettings.updatedBy(), is(consentedBy));
    }

    @Test
    public void shouldInvokeListenersAfterSettingsAreUpdated() {
        final boolean[] isListenerInvoked = {false};
        sharingSettingsService.register(updatedSettings -> isListenerInvoked[0] = true);

        assertFalse(isListenerInvoked[0]);

        sharingSettingsService.createOrUpdate(new DataSharingSettings(true, "Bob", new Date()));

        assertTrue(isListenerInvoked[0]);
    }
}
