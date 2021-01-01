/*
 * Copyright 2021 ThoughtWorks, Inc.
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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionSynchronization;

import java.util.ArrayList;
import java.util.Arrays;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        "classpath:/applicationContext-global.xml",
        "classpath:/applicationContext-dataLocalAccess.xml",
        "classpath:/testPropertyConfigurer.xml",
        "classpath:/spring-all-servlet.xml",
})
public class TransactionSynchronizationManagerTest {
    @Autowired public TransactionTemplate transactionTemplate;

    @Test
    public void shouldRegisterSynchronization() {
        final TransactionSynchronizationManager synchronizationManager = new TransactionSynchronizationManager();

        final TransactionSynchronization synchronization = mock(TransactionSynchronization.class);
        transactionTemplate.execute(new org.springframework.transaction.support.TransactionCallbackWithoutResult() {
            @Override protected void doInTransactionWithoutResult(TransactionStatus status) {
                synchronizationManager.registerSynchronization(synchronization);
            }
        });

        verify(synchronization).afterCommit();
    }

    @Test
    public void shouldRegisterSynchronization_andNotCallItOnTransactionFailure() {
        final TransactionSynchronizationManager synchronizationManager = new TransactionSynchronizationManager();

        final TransactionSynchronization synchronization = mock(TransactionSynchronization.class);
        try {
            transactionTemplate.execute(new org.springframework.transaction.support.TransactionCallbackWithoutResult() {
                @Override protected void doInTransactionWithoutResult(TransactionStatus status) {
                    synchronizationManager.registerSynchronization(synchronization);
                    throw new RuntimeException();
                }
            });
        } catch (Exception e) {
            //ignore
        }
        verify(synchronization, never()).afterCommit();
    }

    @Test
    public void shouldUnderstandIfTransactionIsActive() {
        final TransactionSynchronizationManager synchronizationManager = new TransactionSynchronizationManager();
        final ArrayList<Boolean> transactionActivity = new ArrayList<>();

        transactionTemplate.execute(new org.springframework.transaction.support.TransactionCallbackWithoutResult() {
            @Override protected void doInTransactionWithoutResult(TransactionStatus status) {
                transactionActivity.add(synchronizationManager.isActualTransactionActive());
            }
        });
        assertThat(transactionActivity, is(Arrays.asList(true)));
        assertThat(synchronizationManager.isActualTransactionActive(), is(false));
    }

    @Test
    public void shouldUnderstandIfTransactionBodyExecuting() {
        final boolean[] inBody = new boolean[] {false};
        final TransactionSynchronizationManager synchronizationManager = new TransactionSynchronizationManager();

        transactionTemplate.execute(new TransactionCallback() {
            @Override
            public Object doInTransaction(TransactionStatus status) {
                inBody[0] = synchronizationManager.isTransactionBodyExecuting();
                return null;
            }
        });
        assertThat(inBody[0], is(true));
        assertThat(synchronizationManager.isTransactionBodyExecuting(), is(false));
    }
}
