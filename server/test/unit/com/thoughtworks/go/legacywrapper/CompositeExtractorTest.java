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

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import com.thoughtworks.go.helpers.DataUtils;
import org.jmock.Mock;
import org.jmock.cglib.MockObjectTestCase;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

public class CompositeExtractorTest extends MockObjectTestCase {

    public void testShouldCallAllHandlers() throws Exception {
        Mock handler1 = mock(SAXBasedExtractor.class);
        Mock handler2 = mock(SAXBasedExtractor.class);

        handler1.expects(once()).method("characters").with(eq(null), eq(0), eq(2));
        handler2.expects(once()).method("characters").with(eq(null), eq(0), eq(2));

        handler1.expects(once()).method("canStop").will(returnValue(false));

        com.thoughtworks.go.legacywrapper.CompositeExtractor handler =
                new com.thoughtworks.go.legacywrapper.CompositeExtractor(extractors(handler1, handler2));

        handler.characters(null, 0, 2);
    }

    public void testShouldCallAllExtractors() throws Exception {
        Mock extractor1 = mock(SAXBasedExtractor.class);
        Mock extractor2 = mock(SAXBasedExtractor.class);

        extractor1.expects(once()).method("report").with(ANYTHING);
        extractor2.expects(once()).method("report").with(ANYTHING);

        com.thoughtworks.go.legacywrapper.CompositeExtractor handler =
                new com.thoughtworks.go.legacywrapper.CompositeExtractor(extractors(extractor1, extractor2));

        handler.report(null);
    }

    public void testShouldThrowExceptionToStopParsingWhenAllHandlersCanStop() throws Exception {
        Mock handler1 = mock(SAXBasedExtractor.class);
        Mock handler2 = mock(SAXBasedExtractor.class);

        handler1.expects(once()).method("characters").with(eq(null), eq(0), eq(2));
        handler2.expects(once()).method("characters").with(eq(null), eq(0), eq(2));

        handler1.expects(once()).method("canStop").will(returnValue(true));
        handler2.expects(once()).method("canStop").will(returnValue(true));

        com.thoughtworks.go.legacywrapper.CompositeExtractor handler =
                new com.thoughtworks.go.legacywrapper.CompositeExtractor(extractors(handler1, handler2));

        try {
            handler.characters(null, 0, 2);
            fail();
        } catch (ShouldStopParsingException e) {
            // ok.
        }
    }

    public void testShouldCallRespondingCallbacksWhenParseRealXml() throws Exception {
        File logXml = DataUtils.getFailedBuildLbuildAsFile().getFile();
        SAXParser parser = SAXParserFactory.newInstance().newSAXParser();
        com.thoughtworks.go.legacywrapper.CompositeExtractor extractor =
                new com.thoughtworks.go.legacywrapper.CompositeExtractor(Arrays.asList(new SAXBasedExtractor[] {new ExtractorStub()}));

        parser.parse(logXml, extractor);
        Map result = new HashMap();
        extractor.report(result);

        assertTrue(((Boolean) result.get("allCallbackWereCalled")).booleanValue());
    }

    private List extractors(final Mock handler1, final Mock handler2) {
        SAXBasedExtractor[] extractors = new SAXBasedExtractor[]{
                (SAXBasedExtractor) handler1.proxy(), (SAXBasedExtractor) handler2.proxy()};
        return Arrays.asList(extractors);
    }

    private static class ExtractorStub extends SAXBasedExtractor {

        private boolean charCalled;

        private boolean startElementCalled;

        private boolean endElementCalled;

        public void report(Map resultSet) {
            resultSet.put("allCallbackWereCalled", charCalled && startElementCalled
                    && endElementCalled);
        }

        public void characters(char[] arg0, int arg1, int arg2) throws SAXException {
            charCalled = true;
        }

        public void endElement(String arg0, String arg1, String arg2) throws SAXException {
            startElementCalled = true;
        }

        public void startElement(String arg0, String arg1, String arg2, Attributes arg3)
                throws SAXException {
            endElementCalled = true;
        }
    }
}