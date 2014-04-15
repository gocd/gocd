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

import java.util.Map;

import com.thoughtworks.go.legacywrapper.SAXBasedExtractor;
import com.thoughtworks.go.server.domain.BuildTestCase;
import com.thoughtworks.go.server.domain.BuildTestCaseResult;
import org.apache.commons.lang.StringUtils;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

public class TestCaseExtractor extends SAXBasedExtractor {

    private boolean readingTestCase;

    private boolean readingError;

    private boolean readingFailure;

    private BuildTestCase testcase;

    private String name;

    private String duration;

    private String classname;

    private String errorOrFailureMessage;

    private String errorOrFailureDetail = "";

    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        if ("testcase".equals(qName)) {
            readingTestCase = true;
            name = getAttribute(attributes, "name");
            duration = getAttribute(attributes, "time");
            classname = getAttribute(attributes, "classname");
        }
        String attributeName = "message";
        if (readingTestCase && "error".equals(qName)) {
            readingError = true;
            errorOrFailureMessage = getAttribute(attributes, attributeName);
        }
        if (readingTestCase && "failure".equals(qName)) {
            readingFailure = true;
            errorOrFailureMessage = getAttribute(attributes, attributeName);
        }
    }

    public void characters(char[] ch, int start, int length) throws SAXException {
        if (readingError || readingFailure) {
            String text = new String(ch, start, length);
            if (StringUtils.isBlank(text)) {
                return;
            }
            errorOrFailureDetail += text;
        }
    }

    public void endElement(String uri, String localName, String qName) throws SAXException {
        if (readingError || readingFailure) {
            testcase = new BuildTestCase(name, duration, classname, errorOrFailureMessage, errorOrFailureDetail,
                    readingError ? BuildTestCaseResult.ERROR : BuildTestCaseResult.FAILED);
        }
        if ("testcase".equals(qName)) {
            if (!(readingError || readingFailure)) {
                testcase = new BuildTestCase(name, duration, classname, "", "", BuildTestCaseResult.PASSED);
            }
            readingTestCase = false;
            readingError = false;
            readingFailure = false;
            errorOrFailureDetail = "";
        }
    }

    public void report(Map resultSet) {
        if (testcase != null) {
            resultSet.put("testcase", testcase);
        }
    }
}