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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.thoughtworks.go.server.domain.BuildTestSuite;

import org.apache.commons.lang.StringUtils;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

public class TestSuiteExtractor extends SAXBasedExtractor {

    private TestCaseExtractor testcaseExtractor = new TestCaseExtractor();

    private List testSuites = new ArrayList();

    private BuildTestSuite singleTestSuite;

    private List testcasesForSingleTestSuite;

    private boolean readingTestSuite;

    private Map testcaseResult = new HashMap();

    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        if ("testsuite".equals(qName)) {
            readingTestSuite = true;
            singleTestSuite = createSingleTestSuite(attributes);
            testcasesForSingleTestSuite = new ArrayList();
            return;
        }
        if (readingTestSuite) {
            testcaseExtractor.startElement(uri, localName, qName, attributes);
        }
    }

    public void characters(char[] ch, int start, int length) throws SAXException {
        if (readingTestSuite) {
            testcaseExtractor.characters(ch, start, length);
        }
    }

    public void endElement(String uri, String localName, String qName) throws SAXException {
        if (readingTestSuite) {
            testcaseExtractor.endElement(uri, localName, qName);
        }
        if (readingTestSuite && "testcase".equals(qName)) {
            testcaseExtractor.report(testcaseResult);
            testcasesForSingleTestSuite.add(testcaseResult.get("testcase"));
        }

        if ("testsuite".equals(qName)) {
            readingTestSuite = false;
            singleTestSuite.appendTestCases(testcasesForSingleTestSuite);
            testSuites.add(singleTestSuite);
        }
    }

    private BuildTestSuite createSingleTestSuite(Attributes attributes) {
        String name = attributes.getValue("name");
        float duration = Float.parseFloat(StringUtils.defaultString(attributes.getValue("time"), "0.0"));
        int tests = Integer.parseInt(StringUtils.defaultString(attributes.getValue("tests"), "0"));
        int failures = Integer.parseInt(StringUtils.defaultString(attributes.getValue("failures"), "0"));
        int errors = Integer.parseInt(StringUtils.defaultString(attributes.getValue("errors"), "0"));
        return new BuildTestSuite(name, duration);
    }

    public void report(Map resultSet) {
        resultSet.put("testsuites", testSuites);
    }
}