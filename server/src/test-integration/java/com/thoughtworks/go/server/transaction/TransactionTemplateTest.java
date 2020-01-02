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

import com.thoughtworks.go.util.ReflectionUtil;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;

import java.io.IOException;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        "classpath:/applicationContext-global.xml",
        "classpath:/applicationContext-dataLocalAccess.xml",
        "classpath:/testPropertyConfigurer.xml",
        "classpath:/spring-all-servlet.xml",
})
public class TransactionTemplateTest {
    @Autowired private TransactionTemplate goTransactionTemplate;
    @Autowired private TransactionSynchronizationManager transactionSynchronizationManager;

    private boolean txnCommited;
    private boolean txnCompleted;

    private org.springframework.transaction.support.TransactionTemplate transactionTemplate;//set in setup

    @Before
    public void setUp() {
        txnCommited = false;
        txnCompleted = false;
        transactionTemplate = (org.springframework.transaction.support.TransactionTemplate) ReflectionUtil.getField(goTransactionTemplate, "transactionTemplate");
    }

    @Test
    public void shouldBubbleRuntimeExceptionToActualTransactionTemplateForCallback() throws Exception {
        TransactionTemplate template = new TransactionTemplate(transactionTemplate);
        try {
            template.executeWithExceptionHandling(new TransactionCallback() {
                @Override public Object doInTransaction(TransactionStatus status) throws Exception {
                    registerSynchronization();
                    throw new Exception("foo");
                }
            });
            fail("should have thrown exception");
        } catch (Exception e) {
            assertThat(e.getMessage(), is("foo"));
        }
        assertThat(txnCommited, is(false));
        assertThat(txnCompleted, is(true));
    }

    @Test
    public void shouldBubbleRuntimeExceptionToActualTransactionTemplateForCallbackWithoutReturnValue() throws Exception {
        TransactionTemplate template = new TransactionTemplate(transactionTemplate);
        try {
            template.executeWithExceptionHandling(new TransactionCallbackWithoutResult() {
                @Override public void doInTransactionWithoutResult(TransactionStatus status) throws Exception {
                    registerSynchronization();
                    throw new Exception("foo");
                }
            });
            fail("should have thrown exception");
        } catch (Exception e) {
            assertThat(e.getMessage(), is("foo"));
        }
        assertThat(txnCommited, is(false));
        assertThat(txnCompleted, is(true));
    }

    @Test
    public void shouldReturnValueReturnedByCallback() throws Exception {
        TransactionTemplate template = new TransactionTemplate(transactionTemplate);

        String returnVal = (String) template.executeWithExceptionHandling(new TransactionCallback() {
            @Override public Object doInTransaction(TransactionStatus status) throws Exception {
                registerSynchronization();
                return "foo";
            }
        });
        assertThat(txnCommited, is(true));
        assertThat(txnCompleted, is(true));
        assertThat(returnVal, is("foo"));
    }

    @Test
    public void shouldReturnNullForCallbackWithoutReturn() throws Exception {
        TransactionTemplate template = new TransactionTemplate(transactionTemplate);

        Object returnVal = template.executeWithExceptionHandling(new TransactionCallbackWithoutResult() {
            @Override public void doInTransactionWithoutResult(TransactionStatus status) throws Exception {
                registerSynchronization();
            }
        });
        assertThat(txnCommited, is(true));
        assertThat(txnCompleted, is(true));
        assertThat(returnVal, nullValue());
    }

    @Test
    public void shouldExecuteTransactionCallback() throws Exception {
        TransactionTemplate template = new TransactionTemplate(transactionTemplate);

        String returnVal = (String) template.execute(new org.springframework.transaction.support.TransactionCallback() {
            @Override
            public Object doInTransaction(TransactionStatus status) {
                registerSynchronization();
                return "foo";
            }
        });
        assertThat(txnCommited, is(true));
        assertThat(txnCompleted, is(true));
        assertThat(returnVal, is("foo"));
    }

    @Test
    public void shouldUnderstand_InTransaction() {
        TransactionTemplate template = new TransactionTemplate(transactionTemplate);

        final boolean[] inTransactionInBody = new boolean[] {false};
        final boolean[] inTransactionInAfterCommit = new boolean[] {true};
        final boolean[] inTransactionInAfterComplete = new boolean[] {true};

        String returnVal = (String) template.execute(new org.springframework.transaction.support.TransactionCallback() {
            @Override
            public Object doInTransaction(TransactionStatus status) {
                setTxnBodyActiveFlag(inTransactionInBody, inTransactionInAfterCommit, inTransactionInAfterComplete, 0);
                return "foo";
            }
        });

        assertThat(inTransactionInBody[0], is(true));
        assertThat(inTransactionInAfterCommit[0], is(false));
        assertThat(inTransactionInAfterComplete[0], is(false));
        assertThat(returnVal, is("foo"));
    }

    @Test
    public void shouldAllowRegistrationOfTransactionSynchronization_inTransactionSurroundingBlock_andExecuteAppropriateHooks() {
        final TransactionTemplate template = new TransactionTemplate(transactionTemplate);

        final boolean[] afterCommitHappened = new boolean[1];
        final boolean[] transactionWasActiveInSurrounding = new boolean[1];
        final boolean[] transactionWasActiveInTransaction = new boolean[1];

        String returnVal = (String) template.transactionSurrounding(new TransactionTemplate.TransactionSurrounding<RuntimeException>() {
            @Override
            public Object surrounding() {
                transactionWasActiveInSurrounding[0] = transactionSynchronizationManager.isTransactionBodyExecuting();

                transactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
                    @Override public void afterCommit() {
                        afterCommitHappened[0] = true;
                    }
                });
                return template.execute(new org.springframework.transaction.support.TransactionCallback() {
                    @Override
                    public Object doInTransaction(TransactionStatus status) {
                        transactionWasActiveInTransaction[0] = transactionSynchronizationManager.isTransactionBodyExecuting();
                        return "foo";
                    }
                });
            }
        });

        assertThat(returnVal, is("foo"));
        assertThat(afterCommitHappened[0], is(true));
        assertThat(transactionWasActiveInSurrounding[0], is(false));
        assertThat(transactionWasActiveInTransaction[0], is(true));
    }

    @Test
    public void shouldAllowRegistrationOfTransactionSynchronization_inTransactionSurroundingBlock_andNotExecuteSynchronizationIfTransactionNeverHappens() {
        TransactionTemplate template = new TransactionTemplate(transactionTemplate);

        final boolean[] afterCommitHappened = new boolean[1];

        String returnVal = (String) template.transactionSurrounding(new TransactionTemplate.TransactionSurrounding<RuntimeException>() {
            @Override
            public Object surrounding() {
                transactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
                    @Override public void afterCommit() {
                        afterCommitHappened[0] = true;
                    }
                });
                return "bar";
            }
        });

        assertThat(returnVal, is("bar"));
        assertThat(afterCommitHappened[0], is(false));
    }

    @Test
    public void should_NOT_useSynchronizationsFromOneSurroundingBlockInAnother() {
        final TransactionTemplate template = new TransactionTemplate(transactionTemplate);

        final boolean[] afterCommitHappened = new boolean[1];

        String returnVal = (String) template.transactionSurrounding(new TransactionTemplate.TransactionSurrounding<RuntimeException>() {
            @Override
            public Object surrounding() {

                transactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
                    @Override public void afterCommit() {
                        afterCommitHappened[0] = true;
                    }
                });
                return "foo";
            }
        });

        assertThat(returnVal, is("foo"));
        assertThat(afterCommitHappened[0], is(false));//because no transaction happened

        returnVal = (String) template.transactionSurrounding(new TransactionTemplate.TransactionSurrounding<RuntimeException>() {
            @Override
            public Object surrounding() {
                return template.execute(new org.springframework.transaction.support.TransactionCallback() {
                    @Override
                    public Object doInTransaction(TransactionStatus status) {
                        return "bar";
                    }
                });
            }
        });

        assertThat(returnVal, is("bar"));
        assertThat(afterCommitHappened[0], is(false));//because it registered no synchronization
    }

    @Test
    public void shouldPropagateExceptionsOutOfTransactionSurrounding() throws IOException {
        TransactionTemplate template = new TransactionTemplate(transactionTemplate);

        final boolean[] afterCommitHappened = new boolean[1];

        String returnVal = null;
        try {
            returnVal = (String) template.transactionSurrounding(new TransactionTemplate.TransactionSurrounding<IOException>() {
                @Override
                public Object surrounding() throws IOException {
                    transactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
                        @Override public void afterCommit() {
                            afterCommitHappened[0] = true;
                        }
                    });
                    throw new IOException("boo ha!");
                }
            });
            fail("should have propagated exception");
        } catch (IOException e) {
            assertThat(e.getMessage(), is("boo ha!"));
        }

        assertThat(returnVal, nullValue());
        assertThat(afterCommitHappened[0], is(false));
    }

    @Test
    public void shouldNotRegisterSynchronizationMultipleTimes() {
        final TransactionTemplate template = new TransactionTemplate(transactionTemplate);

        final int[] numberOfTimesAfterCommitHappened = new int[1];

        numberOfTimesAfterCommitHappened[0] = 0;

        String returnVal = (String) template.transactionSurrounding(new TransactionTemplate.TransactionSurrounding<RuntimeException>() {
            @Override
            public Object surrounding() {
                transactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
                    @Override public void afterCommit() {
                        numberOfTimesAfterCommitHappened[0]++;
                    }
                });
                return template.execute(new org.springframework.transaction.support.TransactionCallback() {
                    @Override
                    public Object doInTransaction(TransactionStatus status) {
                        return goTransactionTemplate.execute(new org.springframework.transaction.support.TransactionCallback() {
                            @Override
                            public Object doInTransaction(TransactionStatus status) {
                                return "foo";
                            }
                        });
                    }
                });
            }
        });

        assertThat(returnVal, is("foo"));
        assertThat(numberOfTimesAfterCommitHappened[0], is(1));
    }

    @Test
    public void should_NOT_AllowMoreThanOneTransactionInsideSurrounding() {
        final TransactionTemplate template = new TransactionTemplate(transactionTemplate);

        String returnVal = null;
        try {
            returnVal = (String) template.transactionSurrounding(new TransactionTemplate.TransactionSurrounding<RuntimeException>() {
                @Override
                public Object surrounding() {
                    template.execute(new org.springframework.transaction.support.TransactionCallback() {
                        @Override
                        public Object doInTransaction(TransactionStatus status) {
                            return "foo";
                        }
                    });

                    return template.execute(new org.springframework.transaction.support.TransactionCallback() {
                        @Override
                        public Object doInTransaction(TransactionStatus status) {
                            return "bar";
                        }
                    });
                }
            });
            fail("should not have allowed multiple top-level transactions");//this can cause assumptions of registered-synchronization to become invalid -jj
        } catch (RuntimeException e) {
            assertThat(e.getMessage(), is("Multiple independent transactions are not permitted inside single transaction surrounding."));
        }

        assertThat(returnVal, nullValue());
    }

    @Test
    public void shouldAllowMoreThanOneTransactionInsideSurrounding_ifSurroundingIsInsideTransactionAlready() {
        final TransactionTemplate template = new TransactionTemplate(transactionTemplate);

        final boolean[] firstNestedTransactionHappened = new boolean[1];
        final boolean[] secondNestedTransactionHappened = new boolean[1];

        final boolean[] firstNestedTransactionCalledTransactionSynchronization = new boolean[1];
        final boolean[] secondNestedTransactionCalledTransactionSynchronization = new boolean[1];

        final int[] numberOfTimesSynchronizationWasCalled = new int[1];

        numberOfTimesSynchronizationWasCalled[0] = 0;

        String returnVal = (String) template.execute(new org.springframework.transaction.support.TransactionCallback() {
            @Override
            public Object doInTransaction(TransactionStatus status) {
                return template.transactionSurrounding(new TransactionTemplate.TransactionSurrounding<RuntimeException>() {
                    @Override
                    public Object surrounding() {
                        transactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
                            @Override public void afterCommit() {
                                numberOfTimesSynchronizationWasCalled[0]++;
                            }
                        });

                        template.execute(new org.springframework.transaction.support.TransactionCallback() {
                            @Override
                            public Object doInTransaction(TransactionStatus status) {
                                firstNestedTransactionHappened[0] = true;
                                return "foo";
                            }
                        });

                        firstNestedTransactionCalledTransactionSynchronization[0] = numberOfTimesSynchronizationWasCalled[0] > 0;

                        Object ret = template.execute(new org.springframework.transaction.support.TransactionCallback() {
                            @Override
                            public Object doInTransaction(TransactionStatus status) {
                                secondNestedTransactionHappened[0] = true;
                                return "bar";
                            }
                        });

                        secondNestedTransactionCalledTransactionSynchronization[0] = numberOfTimesSynchronizationWasCalled[0] > 0;

                        return ret;
                    }
                });
            }
        });

        assertThat(returnVal, is("bar"));
        assertThat(numberOfTimesSynchronizationWasCalled[0], is(1));

        assertThat(firstNestedTransactionHappened[0], is(true));
        assertThat(firstNestedTransactionCalledTransactionSynchronization[0], is(false));

        assertThat(secondNestedTransactionHappened[0], is(true));
        assertThat(secondNestedTransactionCalledTransactionSynchronization[0], is(false));
    }

    @Test
    public void shouldUnderstand_InTransaction_AcrossNestedInvocations() {
        final TransactionTemplate template = new TransactionTemplate(transactionTemplate);

        final boolean[] inTransactionInBody = new boolean[] {false, false, false, false};
        final boolean[] inTransactionInAfterCommit = new boolean[] {true, true, true, true};
        final boolean[] inTransactionInAfterComplete = new boolean[] {true, true, true, true};

        String returnVal = (String) template.execute(new org.springframework.transaction.support.TransactionCallback() {
            @Override
            public Object doInTransaction(TransactionStatus status) {
                setTxnBodyActiveFlag(inTransactionInBody, inTransactionInAfterCommit, inTransactionInAfterComplete, 0);
                return template.execute(new org.springframework.transaction.support.TransactionCallback() {
                    @Override
                    public Object doInTransaction(TransactionStatus status) {
                        setTxnBodyActiveFlag(inTransactionInBody, inTransactionInAfterCommit, inTransactionInAfterComplete, 1);
                        try {
                            return template.executeWithExceptionHandling(new TransactionCallback() {
                                @Override public Object doInTransaction(TransactionStatus status) throws Exception {
                                    setTxnBodyActiveFlag(inTransactionInBody, inTransactionInAfterCommit, inTransactionInAfterComplete, 2);
                                    return template.execute(new org.springframework.transaction.support.TransactionCallback() {
                                        @Override
                                        public Object doInTransaction(TransactionStatus status) {
                                            setTxnBodyActiveFlag(inTransactionInBody, inTransactionInAfterCommit, inTransactionInAfterComplete, 3);
                                            return "baz";
                                        }
                                    });
                                }
                            });
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }
                });
            }
        });

        for (int i = 0; i < 4; i++) {
            assertThat(inTransactionInBody[i], is(true));
            assertThat(inTransactionInAfterCommit[i], is(false));
            assertThat(inTransactionInAfterComplete[i], is(false));
        }
        assertThat(returnVal, is("baz"));
    }

    private void setTxnBodyActiveFlag(final boolean[] inTransactionInBody, final boolean[] inTransactionInAfterCommit, final boolean[] inTransactionInAfterComplete, final int depth) {
        inTransactionInBody[depth] = transactionSynchronizationManager.isTransactionBodyExecuting();

        transactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
            @Override public void afterCommit() {
                inTransactionInAfterCommit[depth] = transactionSynchronizationManager.isTransactionBodyExecuting();
            }

            @Override public void afterCompletion(int status) {
                inTransactionInAfterComplete[depth] = transactionSynchronizationManager.isTransactionBodyExecuting();
            }
        });
    }

    @Test
    public void shouldUnderstand_InTransaction_ForTransactionWithExceptionHandling() throws Exception {
        TransactionTemplate template = new TransactionTemplate(transactionTemplate);

        final boolean[] inTransactionInBody = new boolean[] {false};
        final boolean[] inTransactionInAfterCommit = new boolean[] {true};
        final boolean[] inTransactionInAfterComplete = new boolean[] {true};

        String returnVal = (String) template.executeWithExceptionHandling(new TransactionCallback() {
            @Override public Object doInTransaction(TransactionStatus status) throws Exception {
                setTxnBodyActiveFlag(inTransactionInBody, inTransactionInAfterCommit, inTransactionInAfterComplete, 0);
                return "foo";
            }
        });

        assertThat(inTransactionInBody[0], is(true));
        assertThat(inTransactionInAfterCommit[0], is(false));
        assertThat(inTransactionInAfterComplete[0], is(false));
        assertThat(returnVal, is("foo"));
    }

    @Test
    public void shouldNotFailWhenNoTransactionStarted() throws InterruptedException {
        final boolean[] transactionBodyIn = new boolean[] {true};

        Thread thd = new Thread(new Runnable() {
            @Override
            public void run() {
                transactionBodyIn[0] = goTransactionTemplate.isTransactionBodyExecuting();
            }
        });
        thd.start();
        thd.join();

        assertThat(transactionBodyIn[0], is(false));
    }

    private void registerSynchronization() {
        transactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
            @Override public void afterCommit() {
                txnCommited = true;
            }

            @Override public void afterCompletion(int status) {
                txnCompleted = true;
            }
        });
    }
}
