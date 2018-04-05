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

package com.thoughtworks.studios.shine.cruise.builder;

import com.thoughtworks.studios.shine.net.URLRepository;

import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;

public class JunitXML {
  private String suiteName;
  private int testCaseCount;
  private List<Integer> erroredTests = new ArrayList<>();
  private List<Integer> failedTests = new ArrayList<>();
  private boolean invalidXML = false;

  public JunitXML(String suiteName, int testCaseCount) {
    this.suiteName = suiteName;
    this.testCaseCount = testCaseCount;
  }

  public JunitXML(String suiteName) {
    this(suiteName, 1);
  }

  public void registerStubContent(URLRepository repository, String uuid) {
    repository.registerArtifact(uuid + "TEST-" + suiteName + ".xml", toString());
  }

  @Override
  public String toString() {
    if (invalidXML) {
      return "I AM NOT XML!";
    }

    String results = "" +
      "<?xml version='1.0' encoding='UTF-8' ?>" +
      "<testsuite errors='" + erroredTests.size() + "' failures='" + failedTests.size() + "' hostname='foo.bar.com' name='" + suiteName + "' tests='" + testCaseCount + "' time='1.76' timestamp='2010-02-04T01:34:53'>" +
      "  <properties>" +
      "    <property name='java.vendor' value='Apple Inc.' />" +
      "  </properties>" +
      generateTestCase() +
      "</testsuite>";
    return results;
  }

  private String generateTestCase() {
    String testCase = "";

    for (int i = 0; i < testCaseCount; i++) {
      if (!erroredTests.contains(i + 1) && !failedTests.contains(i + 1)) {
        testCase += "  <testcase classname='" + suiteName + "' name='test" + (i + 1) + "' time='1.049' />";
      }
      if (erroredTests.contains(i + 1)) {
        testCase += "  <testcase classname='" + suiteName + "' name='test" + (i + 1) + "' time='1.049'>";
        testCase += "    <error message='Something went wrong' type='com.foo.MyException'>com.foo.MyException: Something went wrong...</error>";
        testCase += "  </testcase>";
      } else if (failedTests.contains(i + 1)) {
        testCase += "  <testcase classname='" + suiteName + "' name='test" + (i + 1) + "' time='1.049'>";
        testCase += "    <failure message='Something assert failed...' type='junit.framework.AssertionFailedError'>junit.framework.AssertionFailedError: Something assert failed...</failure>";
        testCase += "  </testcase>";
      }
    }
    return testCase;
  }

  public JunitXML errored(Integer... erroredTests) {
    this.erroredTests = Arrays.asList(erroredTests);
    return this;
  }

  public JunitXML failed(Integer... failedTests) {
    this.failedTests = Arrays.asList(failedTests);
    return this;
  }

  public JunitXML invalidXML() {
    this.invalidXML = true;
    return this;
  }

  public static JunitXML junitXML(String suiteName, int testCount) {
    return new JunitXML(suiteName, testCount);
  }

  public static JunitXML invalidJunitXML() {
    return new JunitXML("BrokenTestSuite").invalidXML();
  }


}
