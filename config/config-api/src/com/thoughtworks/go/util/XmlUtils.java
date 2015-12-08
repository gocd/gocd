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

package com.thoughtworks.go.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.net.URL;
import java.util.regex.Pattern;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

public class XmlUtils {
    private static final String PROLOG_START = "<?xml ";
    private static final String PROLOG_END = " ?>";

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

    public static Element validate(InputStream inputStream, URL resource, XsdErrorTranslator errorHandler, SAXBuilder builder, String xsds) throws Exception {
        builder.setFeature("http://apache.org/xml/features/validation/schema", true);
        builder.setProperty("http://apache.org/xml/properties/schema/external-noNamespaceSchemaLocation", resource.toURI().toString());
        builder.setProperty("http://apache.org/xml/properties/schema/external-schemaLocation", xsds);
        builder.setValidation(true);
        builder.setErrorHandler(errorHandler);
        Document cruiseRoot = builder.build(inputStream);
        if (errorHandler.hasValidationError()) {
            throw new XsdValidationException(errorHandler.translate());
        }
        return cruiseRoot.getRootElement();
    }

    public static Element validate(String xmlContent, URL resource, XsdErrorTranslator errorHandler, SAXBuilder builder) {
        Document documentRoot = null;
        try {
            builder.setFeature("http://apache.org/xml/features/validation/schema", true);
            builder.setProperty("http://apache.org/xml/properties/schema/external-noNamespaceSchemaLocation", resource.toURI().toString());
            builder.setValidation(true);
            builder.setErrorHandler(errorHandler);
            documentRoot = builder.build(new StringReader(xmlContent));
        } catch (Exception e) {
            throw new XsdValidationException(e.getMessage());
        }

        if (errorHandler.hasValidationError()) {
            throw new XsdValidationException(errorHandler.translate());
        }
        return documentRoot.getRootElement();
    }

    public static String asXml(Document document) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        writeXml(document, buffer);
        return buffer.toString();
    }

    public static Document buildXmlDocument(String content) throws JDOMException, IOException {
        SAXBuilder saxBuilder = new SAXBuilder();
        return saxBuilder.build(new StringReader(content));
    }

    public static boolean doesNotMatchUsingXsdRegex(Pattern pattern, String textToMatch) {
        return !matchUsingRegex(pattern, textToMatch);
    }

    public static boolean matchUsingRegex(Pattern pattern, String textToMatch) {
        return pattern.matcher(textToMatch).matches();
    }

    public static String stripProlog(String line) {
        if (line.matches("^\\s*<\\?xml\\b.*$")) {
            int start = line.indexOf(PROLOG_START);
            int end = line.indexOf(PROLOG_END) + PROLOG_END.length();
            if (end > start) {
                String prolog = line.substring(start, end);
                line = line.replace(prolog, "");
            }
        }
        return line;
    }
}
