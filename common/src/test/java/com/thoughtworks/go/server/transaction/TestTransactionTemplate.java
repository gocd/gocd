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

import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.TransactionException;

public class TestTransactionTemplate extends TransactionTemplate {


    private static class ActualTransactionTemplate extends org.springframework.transaction.support.TransactionTemplate {
        private TestTransactionSynchronizationManager synchronizationManager;

        private ActualTransactionTemplate(TestTransactionSynchronizationManager synchronizationManager) {
            this.synchronizationManager = synchronizationManager;
        }

        @Override public Object execute(TransactionCallback action) throws TransactionException {
            Object o = null;
            try {
                o = action.doInTransaction(null);
                synchronizationManager.executeAfterCompletion(TransactionSynchronization.STATUS_COMMITTED);
                synchronizationManager.executeAfterCommit();
            } catch (RuntimeException e) {
                synchronizationManager.executeAfterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);
                throw e;
            } catch (Error e) {
                synchronizationManager.executeAfterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);
                throw e;
            } finally {
                synchronizationManager.clearSynchronizations();
            }
            return o;
        }
    }

    public TestTransactionTemplate(TestTransactionSynchronizationManager synchronizationManager) {
        super(new ActualTransactionTemplate(synchronizationManager));
    }

    @Override public Object execute(TransactionCallback action) {
        return super.execute(action);
    }

    @Override public Object executeWithExceptionHandling(com.thoughtworks.go.server.transaction.TransactionCallback action) throws Exception {
        return super.executeWithExceptionHandling(action);
    }
}
