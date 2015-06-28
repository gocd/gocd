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

package com.thoughtworks.go.server.dao;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class ThreadSafetyChecker {
    private long testTimeoutTime;
    private List<Operation> operations;
    private ConcurrentMap<Thread, Throwable> exceptions;

    public ThreadSafetyChecker(long timeoutInMillisecondsForEveryThreadJoin) {
        this.testTimeoutTime = timeoutInMillisecondsForEveryThreadJoin;
        this.operations = new ArrayList<Operation>();
        this.exceptions = new ConcurrentHashMap<Thread, Throwable>();
    }

    public void addOperation(Operation operation) {
        operations.add(operation);
    }

    public void run(final int numberOfTimesToRunTheStuffInsideTheOperations) throws Exception {
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread thread, Throwable throwable) {
                exceptions.put(thread, throwable);
            }
        });

        List<Thread> threads = createThreads(numberOfTimesToRunTheStuffInsideTheOperations);
        startThreads(threads);
        waitForThreadsToFinish(threads);

        assertThat(exceptions.toString(), exceptions.size(), is(0));
    }

    private void waitForThreadsToFinish(List<Thread> threads) throws Exception {
        for (Thread thread : threads) {
            thread.join(testTimeoutTime);
        }
    }

    private List<Thread> createThreads(final int numberOfTimesToRunTheStuffInsideTheOperations) {
        List<Thread> threads = new ArrayList<Thread>();

        for (int i = 0; i < operations.size(); i++) {
            final Operation operation = operations.get(i);

            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    for (int runIndex = 0; runIndex < numberOfTimesToRunTheStuffInsideTheOperations; runIndex++) {
                        operation.execute(runIndex);
                    }
                }
            }, "ThreadSafetyChecker-" + i);

            threads.add(thread);
        }

        return threads;
    }

    private void startThreads(List<Thread> threads) {
        for (Thread thread : threads) {
            thread.start();
        }
    }

    public static abstract class Operation {
        public abstract void execute(int runIndex);
    }
}
