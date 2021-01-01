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

import java.util.ArrayList;
import java.util.List;

import org.springframework.transaction.support.TransactionSynchronization;

public class TestTransactionSynchronizationManager extends TransactionSynchronizationManager {
    private List<TransactionSynchronization> synchronizations;

    public TestTransactionSynchronizationManager() {
        this.synchronizations = new ArrayList<>();
    }

    @Override public synchronized void registerSynchronization(TransactionSynchronization synchronization) {
        synchronizations.add(synchronization);
    }

    @Override public boolean isTransactionBodyExecuting() {
        return false;//assuming no one cares about this
    }

    public synchronized void executeAfterCommit() {
        for (TransactionSynchronization synchronization : synchronizations) {
            synchronization.afterCommit();
        }
    }

    public synchronized void clearSynchronizations() {
        synchronizations.clear();
    }

    public void executeAfterCompletion(int status) {
        for (TransactionSynchronization synchronization : synchronizations) {
            synchronization.afterCompletion(status);
        }
    }
}
