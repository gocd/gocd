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

package com.thoughtworks.go.server.transaction;

import java.util.HashSet;
import java.util.Set;

import org.springframework.transaction.support.TransactionSynchronization;

import static java.lang.Integer.MAX_VALUE;
import static java.lang.Integer.MIN_VALUE;

public final class TransactionContext {
    private static final String TOO_BIG_STACK_MESSAGE = "This should never have happened, as we shouldn't have MAX_INT deep stack of transactions(and this is thread local, so no wait)";
    private static final String TOO_MANY_RELEASES = "This semaphore has been released more times than acquired.";
    private static final String TOO_MANY_TXNS_IN_SURROUNDING = "Multiple independent transactions are not permitted inside single transaction surrounding.";

    private int txnActive;
    private int txnSurrounding;
    private Set<TransactionSynchronization> futureSynchronizations;
    private boolean txnFinished;

    TransactionContext() {
        txnActive = MAX_VALUE;
        txnSurrounding = MAX_VALUE;
        futureSynchronizations = new HashSet<>();
        txnFinished = false;
    }

    public void transactionPushed() {
        if (! isTransactionBodyExecuting()) {
            if (isInTransactionSurrounding() && txnFinished) {
                throw new RuntimeException(TOO_MANY_TXNS_IN_SURROUNDING);
            }
            txnFinished = false;
        }

        ensureCanPush(txnActive);
        txnActive--;

        if (! futureSynchronizations.isEmpty()) {
            for (TransactionSynchronization futureSynchronization : futureSynchronizations) {
                doRegisterSynchronization(futureSynchronization);
            }
            clearFutureSynchronizations();
        }
    }

    public void transactionPopped() {
        ensureCanPop(txnActive);
        txnActive++;
        if (! isTransactionBodyExecuting()) {
            txnFinished = true;
        }
    }

    private void ensureCanPush(int sem) {
        if (sem == MIN_VALUE) {
            throw new RuntimeException(TOO_BIG_STACK_MESSAGE);
        }
    }

    private void ensureCanPop(int txnActive) {
        if (txnActive == MAX_VALUE) {
            throw new RuntimeException(TOO_MANY_RELEASES);
        }
    }

    public void surroundingPushed() {
        if (! isInTransactionSurrounding()) {
            txnFinished = false;
        }
        ensureCanPush(txnSurrounding);
        txnSurrounding--;
    }

    public void surroundingPopped() {
        ensureCanPop(txnSurrounding);
        txnSurrounding++;
        if (! isInTransactionSurrounding()) {
            clearFutureSynchronizations();
            txnFinished = false;
        }
    }

    public boolean isTransactionBodyExecuting() {
        return isAcquired(txnActive);
    }

    public boolean isInTransactionSurrounding() {
        return isAcquired(txnSurrounding);
    }

    private void clearFutureSynchronizations() {
        futureSynchronizations.clear();
    }

    private static boolean isAcquired(int sem) {
        return sem < MAX_VALUE;
    }

    public void registerSynchronization(TransactionSynchronization synchronization) {
        if (isInTransactionSurrounding() && ! isTransactionBodyExecuting()) {
            futureSynchronizations.add(synchronization);
        } else {
            doRegisterSynchronization(synchronization);
        }
    }

    private void doRegisterSynchronization(TransactionSynchronization synchronization) {
        org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(synchronization);
    }
}
