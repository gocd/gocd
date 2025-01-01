/*
 * Copyright Thoughtworks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.thoughtworks.go.util;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.regex.Pattern;

public class XmlUtils {
    public static void writeXml(Document document, OutputStream outputStream) throws IOException {
        xmlOutputter().output(document, outputStream);
    }

    private static XMLOutputter xmlOutputter() {
        Format format = Format.getPrettyFormat().setEncoding("utf-8").setLineSeparator("\n");
        return new XMLOutputter(format);
    }

    public static void writeXml(Element element, OutputStream outputStream) throws IOException {
        xmlOutputter().output(element, outputStream);
    }

    public static Document buildXmlDocument(InputStream inputStream) throws IOException, JDOMException {
        return new SafeSaxBuilder().build(inputStream);
    }

    public static Document buildXmlDocument(File file) throws IOException, JDOMException {
        return new SafeSaxBuilder().build(file);
    }

    public static Document buildXmlDocument(String xmlContent) throws IOException, JDOMException {
        return new SafeSaxBuilder().build(new StringReader(xmlContent));
    }

    public static Document buildValidatedXmlDocument(InputStream inputStream, URL schemaLocation) throws URISyntaxException, IOException, JDOMException {
        ValidatingSaxBuilder builder = new ValidatingSaxBuilder(schemaLocation);
        XsdErrorTranslator errorHandler = new XsdErrorTranslator();
        builder.setErrorHandler(errorHandler);

        Document document = builder.build(inputStream);
        if (errorHandler.hasValidationError()) {
            throw new XsdValidationException(errorHandler.translate());
        }
        return document;
    }

    public static boolean doesNotMatchUsingXsdRegex(Pattern pattern, String textToMatch) {
        return !matchUsingRegex(pattern, textToMatch);
    }

    public static boolean matchUsingRegex(Pattern pattern, String textToMatch) {
        return pattern.matcher(textToMatch).matches();
    }
}
