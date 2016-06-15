/*
 * Copyright 2016 ThoughtWorks, Inc.
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

package com.thoughtworks.go.legacywrapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.thoughtworks.go.server.domain.BuildMessage;
import com.thoughtworks.go.server.domain.MessageLevel;
import org.apache.commons.lang.StringUtils;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

public class MessageExtractor extends SAXBasedExtractor {

    private boolean readingMessage;

    private String priority;

    private List messages = new ArrayList();

    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        if ("message".equals(qName)) {
            readingMessage = true;
            priority = attributes.getValue("priority"); // for ant logs

            if (priority == null) {
                priority = attributes.getValue("level"); // for nant
            }
        }
    }

    public void characters(char[] ch, int start, int length) throws SAXException {
        if (readingMessage) {
            String text = new String(ch, start, length);
            if (StringUtils.isBlank(text)) {
                return;
            }
            messages.add(text);
        }
    }

    public void endElement(String uri, String localName, String qName) throws SAXException {
        if ("message".equals(qName)) {
            readingMessage = false;
        }
    }

    public void report(Map resultSet) {
        if (!messages.isEmpty()) {
            StringBuilder result = new StringBuilder("");
            for (Object message : messages) {
                result.append(message.toString());
            }
            resultSet.put("message", new BuildMessage(result.toString(), MessageLevel.getLevelForPriority(priority)));
        }
    }
}
