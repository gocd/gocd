/*************************GO-LICENSE-START*********************************
 * Copyright 2015 ThoughtWorks, Inc.
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

package com.thoughtworks.go.util;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

import java.io.*;
import java.net.URL;
import java.util.regex.Pattern;

public class XmlUtils {
    public static void writeXml(Document document, OutputStream outputStream) throws IOException {
        xmlOutputer().output(document, outputStream);
    }

    private static XMLOutputter xmlOutputer() {
        Format format = Format.getPrettyFormat().setEncoding("utf-8").setLineSeparator("\n");
        return new XMLOutputter(format);
    }


    public static void writeXml(Element element, OutputStream outputStream) throws IOException {
        xmlOutputer().output(element, outputStream);
    }

    public static Document buildXmlDocument(InputStream inputStream, URL resource, String xsds) throws Exception {
        return buildXmlDocument(inputStream, new ValidatingSaxBuilder(resource, xsds));
    }

    public static Document buildXmlDocument(String xmlContent, URL resource) throws Exception {
        return buildXmlDocument(new ByteArrayInputStream(xmlContent.getBytes()), new ValidatingSaxBuilder(resource));
    }

    private static Document buildXmlDocument(InputStream inputStream, SAXBuilder builder) throws JDOMException, IOException {
        XsdErrorTranslator errorHandler = new XsdErrorTranslator();
        builder.setErrorHandler(errorHandler);

        Document cruiseRoot = builder.build(inputStream);
        if (errorHandler.hasValidationError()) {
            throw new XsdValidationException(errorHandler.translate());
        }
        return cruiseRoot;
    }

    public static boolean doesNotMatchUsingXsdRegex(Pattern pattern, String textToMatch) {
        return !matchUsingRegex(pattern, textToMatch);
    }

    public static boolean matchUsingRegex(Pattern pattern, String textToMatch) {
        return pattern.matcher(textToMatch).matches();
    }
}
