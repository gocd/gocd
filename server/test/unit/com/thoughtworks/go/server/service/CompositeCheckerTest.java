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

package com.thoughtworks.go.server.service;

import com.thoughtworks.go.serverhealth.HealthStateType;
import com.thoughtworks.go.serverhealth.ServerHealthState;
import static com.thoughtworks.go.server.service.CompositeCheckerTest.StubCheckerFactory.ERROR_SERVER_HEALTH_STATE;
import static com.thoughtworks.go.server.service.CompositeCheckerTest.StubCheckerFactory.SUCCESS_SERVER_HEALTH_STATE;
import static com.thoughtworks.go.server.service.CompositeCheckerTest.StubCheckerFactory.WARNING_SERVER_HEALTH_STATE;
import static com.thoughtworks.go.server.service.CompositeCheckerTest.StubCheckerFactory.successful;
import com.thoughtworks.go.server.service.result.OperationResult;
import com.thoughtworks.go.server.service.result.ServerHealthStateOperationResult;

import static org.junit.Assert.assertThat;

import org.hamcrest.core.Is;
import org.junit.Test;

public class CompositeCheckerTest {

    @Test
    public void shouldReturnSuccessWhenAllTheCheckersReturnSuccess() {
        CompositeChecker checker = new CompositeChecker(successful(), successful());
        ServerHealthStateOperationResult result = new ServerHealthStateOperationResult();
        checker.check(result);
        assertThat(result.getServerHealthState(), Is.is(SUCCESS_SERVER_HEALTH_STATE));
    }

    @Test
    public void shouldReturnWarningWhenOneCheckerIsWarning() {
        CompositeChecker checker = new CompositeChecker(StubCheckerFactory.warning(), StubCheckerFactory.successful());
        ServerHealthStateOperationResult result = new ServerHealthStateOperationResult();
        checker.check(result);
        assertThat(result.getServerHealthState(), Is.is(WARNING_SERVER_HEALTH_STATE));
    }

    @Test
    public void shouldReturnErrorWhenWarningIsFollowedByError() {
        CompositeChecker checker = new CompositeChecker(StubCheckerFactory.warning(), StubCheckerFactory.error());
        ServerHealthStateOperationResult result = new ServerHealthStateOperationResult();
        checker.check(result);
        assertThat(result.getServerHealthState(), Is.is(ERROR_SERVER_HEALTH_STATE));
    }

    @Test
    public void shouldReturnTheFirstError() {
        CompositeChecker checker = new CompositeChecker(StubCheckerFactory.error(), StubCheckerFactory.anotherError());
        ServerHealthStateOperationResult result = new ServerHealthStateOperationResult();
        checker.check(result);
        assertThat(result.getServerHealthState(), Is.is(ERROR_SERVER_HEALTH_STATE));
    }

   static class StubCheckerFactory {
       private static final HealthStateType HEALTH_STATE_TYPE_IDENTIFIER = HealthStateType.invalidConfig();

        static final ServerHealthState ERROR_SERVER_HEALTH_STATE = ServerHealthState.error("error", "royally screwed",
                HEALTH_STATE_TYPE_IDENTIFIER);
        static final ServerHealthState ANOTHER_ERROR_SERVER_HEALTH_STATE = ServerHealthState.error("Second", "Hi World",
                HEALTH_STATE_TYPE_IDENTIFIER);
        static final ServerHealthState SUCCESS_SERVER_HEALTH_STATE = ServerHealthState.success(
                HEALTH_STATE_TYPE_IDENTIFIER);
       static final ServerHealthState WARNING_SERVER_HEALTH_STATE = ServerHealthState.warning("warning", "warning",
               HEALTH_STATE_TYPE_IDENTIFIER);

       public static SchedulingChecker successful() {
            return new SchedulingChecker() {
                public void check(OperationResult result) {
                    result.success(HEALTH_STATE_TYPE_IDENTIFIER);
                }
            };
        }

        public static SchedulingChecker warning() {
            return new SchedulingChecker() {
                public void check(OperationResult result) {
                    result.warning("warning", "warning", HEALTH_STATE_TYPE_IDENTIFIER);
                }
            };
        }

        public static SchedulingChecker error() {
            return new SchedulingChecker() {
                public void check(OperationResult result) {
                    result.error("error", "royally screwed", HEALTH_STATE_TYPE_IDENTIFIER);
                }
            };
        }

        public static SchedulingChecker anotherError() {
            return new SchedulingChecker() {
                public void check(OperationResult result) {
                    result.error("Second", "Hi World", HEALTH_STATE_TYPE_IDENTIFIER);
                }
            };
        }
   }
}
