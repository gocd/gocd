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

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import com.thoughtworks.go.server.domain.LogFile;
import org.apache.log4j.Logger;
import org.xml.sax.SAXException;

public class LogParser {
    private static final Logger LOGGER = Logger.getLogger(LogParser.class);

    public Map parseLogFile(LogFile buildLogFile, boolean buildWasPassed)
            throws IOException, SAXException, ParserConfigurationException {
        Map properties = new HashMap();
        com.thoughtworks.go.legacywrapper.CompositeExtractor compositeExtractor = compositeExtractor(buildWasPassed);
        parse(buildLogFile.getInputStream(), compositeExtractor);
        compositeExtractor.report(properties);
        return properties;
    }

    private void parse(InputStream logFileInputStream, com.thoughtworks.go.legacywrapper.CompositeExtractor compositeExtractor)
            throws SAXException, ParserConfigurationException, IOException {
        SAXParser saxParser = SAXParserFactory.newInstance().newSAXParser();
        try {
            saxParser.parse(logFileInputStream, compositeExtractor);
        } catch (ShouldStopParsingException e) {
            LOGGER.trace("Intentionally throwing exception to stop parsing.");
        }
    }

    private com.thoughtworks.go.legacywrapper.CompositeExtractor compositeExtractor(boolean buildWasPassed) {
        List handlers = defaultExtractors();
        return new CompositeExtractor(handlers);
    }

    private List defaultExtractors() {
        List extractors = new ArrayList();
        extractors.add(new com.thoughtworks.go.legacywrapper.DurationExtractor());
        extractors.add(new BasicInfoExtractor());
        extractors.add(new TestSuiteExtractor());
//        extractors.add(new BuildMessageExtractor());
        extractors.add(new StackTraceExtractor());
        return extractors;
    }
}