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

package com.thoughtworks.go.agent.testhelpers;

import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunNotifier;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;


public final class GoServerRunner extends SpringJUnit4ClassRunner {
    public static final String PASSWORD = "Crui3CertSigningPassword";
    private FakeGoServer fakeGoServer;

    public GoServerRunner(Class<?> testClass) throws org.junit.runners.model.InitializationError {
        super(testClass);
        fakeGoServer = new FakeGoServer(9090, 8443);
    }

    public void run(RunNotifier runNotifier) {
        try {
            // could be smarter if this is too slow, start only if not started already
            // shut down on JVM shut down instead
            fakeGoServer.start();
        } catch (Exception e) {
            runNotifier.fireTestFailure(new Failure(getDescription(), e));
        }
        try {
            super.run(runNotifier);
        } finally {
            try {
                fakeGoServer.stop();
            } catch (Exception e) {
                runNotifier.fireTestFailure(new Failure(getDescription(), e));
            }
        }
    }
}