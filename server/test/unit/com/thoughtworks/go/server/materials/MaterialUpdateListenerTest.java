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

package com.thoughtworks.go.server.materials;

import com.thoughtworks.go.config.materials.svn.SvnMaterial;
import com.thoughtworks.go.helper.MaterialsMother;
import com.thoughtworks.go.server.cronjob.GoDiskSpaceMonitor;
import com.thoughtworks.go.server.perf.MDUPerformanceLogger;
import com.thoughtworks.go.server.transaction.TransactionCallback;
import com.thoughtworks.go.server.transaction.TransactionTemplate;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.internal.verification.Times;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class MaterialUpdateListenerTest {
    private MaterialUpdateListener materialUpdateListener;
    private MaterialUpdateCompletedTopic topic;
    private MaterialDatabaseUpdater updater;
    private static final SvnMaterial MATERIAL = MaterialsMother.svnMaterial();
    private GoDiskSpaceMonitor diskSpaceMonitor;
    private TransactionTemplate transactionTemplate;
    private MDUPerformanceLogger mduPerformanceLogger;

    @Before public void setUp() throws Exception {
        topic = mock(MaterialUpdateCompletedTopic.class);
        updater = mock(MaterialDatabaseUpdater.class);
        diskSpaceMonitor = mock(GoDiskSpaceMonitor.class);
        transactionTemplate = mock(TransactionTemplate.class);
        mduPerformanceLogger = mock(MDUPerformanceLogger.class);
        materialUpdateListener = new MaterialUpdateListener(topic, updater, mduPerformanceLogger, diskSpaceMonitor);
    }

    @Test
    public void shouldNotUpdateOnMessageWhenLowOnDisk() throws Exception {
        when(diskSpaceMonitor.isLowOnDisk()).thenReturn(true);
        materialUpdateListener.onMessage(new MaterialUpdateMessage(MATERIAL, 0));
        verify(updater, new Times(0)).updateMaterial(MATERIAL);
    }

    @Test
    public void shouldUpdatematerialOnMessage() throws Exception {
        setupTransactionTemplateStub();
        materialUpdateListener.onMessage(new MaterialUpdateMessage(MATERIAL, 0));
        verify(updater).updateMaterial(MATERIAL);
    }

    private void setupTransactionTemplateStub() throws Exception {
        when(transactionTemplate.executeWithExceptionHandling(Mockito.any(TransactionCallback.class))).thenAnswer(invocationOnMock -> {
            TransactionCallback callback = (TransactionCallback) invocationOnMock.getArguments()[0];
            callback.doInTransaction(null);
            return null;
        });
    }

    @Test
    public void shouldPostOnCompleteMessage() throws Exception {
        materialUpdateListener.onMessage(new MaterialUpdateMessage(MATERIAL, 20));
        verify(topic).post(new MaterialUpdateSuccessfulMessage(MATERIAL, 20));
    }

    @Test
    public void shouldPostUpdateFailedMessageOnException() throws Exception {
        setupTransactionTemplateStub();
        Exception exception = new Exception();
        doThrow(exception).when(updater).updateMaterial(MATERIAL);
        materialUpdateListener.onMessage(new MaterialUpdateMessage(MATERIAL, 10));
        verify(topic).post(new MaterialUpdateFailedMessage(MATERIAL, 10, exception));
    }
}
