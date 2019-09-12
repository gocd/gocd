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
package com.thoughtworks.go.server.transaction;

import org.springframework.transaction.TransactionStatus;

import java.util.function.Consumer;

public class TransactionTemplate {
    private org.springframework.transaction.support.TransactionTemplate transactionTemplate;

    private static final ThreadLocal<TransactionContext> txnCtx = ThreadLocal.withInitial(() -> new TransactionContext());

    public TransactionTemplate(org.springframework.transaction.support.TransactionTemplate transactionTemplate) {
        this.transactionTemplate = transactionTemplate;
    }

    public void execute(final Consumer<TransactionStatus> action) {
        transactionTemplate.execute(status -> {
            txnCtx().transactionPushed();
            try {
                action.accept(status);
                return null;
            } finally {
                txnCtx().transactionPopped();
            }
        });
    }

    public <T> T execute(final org.springframework.transaction.support.TransactionCallback<T> action) {
        return transactionTemplate.execute(status -> {
            txnCtx().transactionPushed();
            try {
                return action.doInTransaction(status);
            } finally {
                txnCtx().transactionPopped();
            }
        });
    }

    public Object executeWithExceptionHandling(final TransactionCallback action) throws Exception {
        try {
            return transactionTemplate.execute((org.springframework.transaction.support.TransactionCallback) status -> {
                txnCtx().transactionPushed();
                try {
                    return action.doWithExceptionHandling(status);
                } finally {
                    txnCtx().transactionPopped();
                }
            });
        } catch (TransactionCallbackExecutionException e) {
            throw (Exception) e.getCause();
        }
    }

    public static boolean isTransactionBodyExecuting() {
        return txnCtx().isTransactionBodyExecuting();
    }

    public <T extends Exception> Object transactionSurrounding(TransactionSurrounding<T> surrounding) throws T {
        txnCtx().surroundingPushed();
        try {
            return surrounding.surrounding();
        } finally {
            txnCtx().surroundingPopped();
        }
    }

    static TransactionContext txnCtx() {
        return txnCtx.get();
    }

    public static interface TransactionSurrounding<T extends Exception> {
        Object surrounding() throws T;
    }
}
