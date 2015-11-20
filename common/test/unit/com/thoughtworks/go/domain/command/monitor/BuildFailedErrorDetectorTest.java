/*
 * Copyright 2015 ThoughtWorks, Inc.
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
 */

package com.thoughtworks.go.domain.command.monitor;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Before;
import org.junit.Test;

public class BuildFailedErrorDetectorTest {
    private Mockery context;
    private Reporter reporter;
    private BuildFailedErrorDetector detector;

    @Before
    public void setUp() {
        context = new JUnitRuleMockery();
        reporter = context.mock(Reporter.class);
        detector = new BuildFailedErrorDetector(reporter);
    }

    @Test public void shouldReportNothingByDefault() throws Exception {
        detector.consumeLine("Something normal happened");
    }

    @Test public void shouldReportErrorWhenBuildFailed() throws Exception {
        context.checking(new Expectations(){{
            one(reporter).failing("Command reported [BUILD FAILED].");
        }});
        detector.consumeLine(" BUILD FAILED ");
    }

    @Test public void shouldMatchEntireLineWhenDetectingError() throws Exception {
        detector.consumeLine("blah blah blah BUILD FAILED");
    }

    @Test public void shouldNotFailWhenInnerBuildFails() throws Exception {
        detector.consumeLine("inner-build:");
        detector.consumeLine("    [antcall] BUILD FAILED");
    }

}
