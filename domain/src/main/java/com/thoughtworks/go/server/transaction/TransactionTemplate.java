/*
 * Copyright 2024 Thoughtworks, Inc.
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

public class TransactionTemplate {
    private final org.springframework.transaction.support.TransactionTemplate transactionTemplate;

    private static final ThreadLocal<TransactionContext> txnCtx = ThreadLocal.withInitial(TransactionContext::new);

    public TransactionTemplate(org.springframework.transaction.support.TransactionTemplate transactionTemplate) {
        this.transactionTemplate = transactionTemplate;
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
            return transactionTemplate.execute(status -> {
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

    public interface TransactionSurrounding<T extends Exception> {
        Object surrounding() throws T;
    }
}
