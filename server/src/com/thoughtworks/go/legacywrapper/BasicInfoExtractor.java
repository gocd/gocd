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
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

public class BasicInfoExtractor extends SAXBasedExtractor {

    private boolean readingInfo;

    private String projectName;

    private String label;

    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        if ("info".equals(qName)) {
            readingInfo = true;
            return;
        }
        if (readingInfo && "property".equals(qName)) {
            String propName = attributes.getValue("name");
            if ("projectname".equals(propName)) {
                projectName = attributes.getValue("value");
            }
            if ("label".equals(propName)) {
                label = attributes.getValue("value");
            }
        }
    }

    public void endElement(String uri, String localName, String qName) throws SAXException {
        if ("info".equals(qName)) {
            readingInfo = false;
            canStop(true);
        }
    }

    public void report(Map resultSet) {
        resultSet.put("projectname", projectName);
        resultSet.put("label", label);
    }
}
