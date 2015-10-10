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

package com.thoughtworks.go.legacywrapper;

import java.util.List;
import java.util.Map;

import com.thoughtworks.go.helpers.DataUtils;
import com.thoughtworks.go.server.domain.BuildTestCase;
import com.thoughtworks.go.server.domain.BuildTestCaseResult;
import com.thoughtworks.go.server.domain.BuildTestSuite;
import com.thoughtworks.go.server.domain.LogFile;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.assertThat;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.BlockJUnit4ClassRunner;

@RunWith(BlockJUnit4ClassRunner.class)
public class LogParserTest {
    private LogParser logParser;

    @Before
    public void setup() {
        logParser = new LogParser();
    }

    @Test
    public void testCanReadError() throws Exception {
        LogFile logFile = new LogFile(DataUtils.getFailedBuildLbuildAsFile().getFile());
        boolean isPassed = false;
        Map map = logParser.parseLogFile(logFile, isPassed);


        List suites = getTestSuites(map);
        BuildTestSuite firstSuite = (BuildTestSuite) suites.get(0);

        List erroringTestCases = firstSuite.getErrorTestCases();
        assertThat(erroringTestCases.size(), is(1));

        BuildTestCase erroredTest = (BuildTestCase) erroringTestCases.get(0);
        String expectedClassName = "net.sourceforge.cruisecontrol.sampleproject.connectfour.PlayingStandTest";
        String expectedNoClassDefFoundError = "java.lang.NoClassDefFoundError: org/objectweb/asm/CodeVisitor";
        String exptectedClassPath = "at net.sf.cglib.core.KeyFactory$Generator.generateClass(KeyFactory.java:165)";
        assertThat(erroredTest.getClassname(), is(expectedClassName));
        assertThat(erroredTest.getDuration(), is("0.016"));
        assertThat(erroredTest.getName(), is("testFourConnected"));
        assertThat(erroredTest.didError(), is(true));
        assertThat(erroredTest.getMessage(), is("org/objectweb/asm/CodeVisitor"));
        assertThat(erroredTest.getMessageBody(), containsString(expectedNoClassDefFoundError));
        assertThat(erroredTest.getMessageBody(), containsString(exptectedClassPath));
    }

    @Test
    public void testCanReadFailure() throws Exception {
        LogFile logFile = new LogFile(DataUtils.getFailedBuildLbuildAsFile().getFile());
        boolean isPassed = false;
        Map map = logParser.parseLogFile(logFile, isPassed);


        List suites = getTestSuites(map);
        BuildTestSuite firstSuite = (BuildTestSuite) suites.get(0);

        List failingCases = firstSuite.getFailingTestCases();
        assertThat(firstSuite.getNumberOfFailures(), is(3));
        assertThat(failingCases.size(), is(3));

        BuildTestCase failingTest = (BuildTestCase) failingCases.get(0);
        String expectedNoClassDefFoundError = "junit.framework.AssertionFailedError: Error during schema validation";
        String exptectedClassPath = "at junit.framework.Assert.fail(Assert.java:47)";
        String className = "net.sourceforge.cruisecontrol.sampleproject.connectfour.PlayingStandTest";
        assertThat(failingTest.getClassname(), is(className));
        assertThat(failingTest.getDuration(), is("3.807"));
        assertThat(failingTest.getName(), is("testSomething"));
        assertThat(failingTest.getResult(), is(BuildTestCaseResult.FAILED));
        assertThat(failingTest.getMessage(), is("Not the expected result"));
        assertThat(failingTest.getMessageBody(), containsString(expectedNoClassDefFoundError));
        assertThat(failingTest.getMessageBody(), containsString(exptectedClassPath));
    }

    private List getTestSuites(Map data) {
        return (List) data.get("testsuites");
    }
}
