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

import com.thoughtworks.go.legacywrapper.SAXBasedExtractor;
import com.thoughtworks.go.legacywrapper.ShouldStopParsingException;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

/**
 * TODO Dynamic Proxy/Functor
 */
public class CompositeExtractor extends SAXBasedExtractor {

    private static final SAXException SHOULD_STOP_EXCEPTION = new ShouldStopParsingException("");

    private final List handlers;

    public CompositeExtractor(List handlers) {
        this.handlers = handlers;
    }

    public void report(Map resultSet) {
        for (int i = 0; i < handlers.size(); i++) {
            extractor(i).report(resultSet);
        }
    }

    public void characters(char[] ch, int start, int length) throws SAXException {
        for (int i = 0; i < handlers.size(); i++) {
            extractor(i).characters(ch, start, length);
        }
        throwExceptionIfCanStop();
    }

    public void endElement(String uri, String localName, String qName) throws SAXException {
        for (int i = 0; i < handlers.size(); i++) {
            extractor(i).endElement(uri, localName, qName);
        }
        throwExceptionIfCanStop();
    }

    public void startElement(String uri, String localName, String qName, Attributes attributes)
            throws SAXException {
        for (int i = 0; i < handlers.size(); i++) {
            extractor(i).startElement(uri, localName, qName, attributes);
        }
        throwExceptionIfCanStop();
    }

    private void throwExceptionIfCanStop() throws SAXException {
        for (int i = 0; i < handlers.size(); i++) {
            if (!extractor(i).canStop()) {
                return;
            }
        }
        throw SHOULD_STOP_EXCEPTION;
    }

    private SAXBasedExtractor extractor(final int i) {
        return ((SAXBasedExtractor) handlers.get(i));
    }
}
