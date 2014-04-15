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

import org.apache.commons.lang.StringUtils;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

public class StackTraceExtractor extends SAXBasedExtractor {

    public static final String KEY_STACKTRACE = "stacktrace.stacktrace";
    public static final String KEY_ERROR = "stacktrace.error";
    private boolean readingBuild;
    private boolean readingStackTrace;
    private String stackTrace = "";
    private String error = "";

    public void startElement(String uri, String localName, String qName, Attributes attributes)
            throws SAXException {
        if ("build".equals(qName)) {
            readingBuild = true;
            error = StringUtils.defaultString(attributes.getValue("error"));
        }
        if ("stacktrace".equals(qName)) {
            readingStackTrace = true;
        }
    }

    public void characters(char[] ch, int start, int length) throws SAXException {
        if (readingBuild && readingStackTrace) {
            String text = new String(ch, start, length);
            if (StringUtils.isBlank(text)) {
                return;
            }
            stackTrace += text;
        }
    }

    public void endElement(String uri, String localName, String qName) throws SAXException {
        if ("build".equals(qName)) {
            readingBuild = false;
        }
        if ("stacktrace".equals(qName)) {
            readingStackTrace = false;
        }
    }

    public void report(Map resultSet) {
        resultSet.put(KEY_STACKTRACE, stackTrace);
        resultSet.put(KEY_ERROR, error);
    }
}