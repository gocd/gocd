/*
 * Copyright 2024 Thoughtworks, Inc.
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

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.*;

public class XpathUtils {

    private static final DocumentBuilderFactory DOC_BUILDER_FACTORY = createDocumentBuilderFactory();

    public static String evaluate(File file, String xpath) throws XPathExpressionException, IOException {
        try (InputStream stream = new FileInputStream(file)) {
            InputSource inputSource = new InputSource(stream);
            return safeEvaluate(inputSource, xpath);
        }
    }

    public static boolean nodeExists(File file, String xpath) throws XPathExpressionException, IOException {
        try (FileInputStream stream = new FileInputStream(file)) {
            return nodeExists(stream, xpath);
        }
    }

    public static boolean nodeExists(InputStream stream, String xpath) throws XPathExpressionException {
        return nodeExists(new InputSource(stream), xpath);
    }

    public static boolean nodeExists(InputSource inputSource, String xpath) throws XPathExpressionException {
        return Boolean.TRUE.equals(safeEvaluate(inputSource, xpath, XPathConstants.BOOLEAN));
    }

    public static boolean nodeExists(String xmlContent, String xpath) throws XPathExpressionException {
        return nodeExists(new InputSource(new StringReader(xmlContent)), xpath);
    }

    private static <T> T safeEvaluate(InputSource inputSource, String xpath, QName type) throws XPathExpressionException {
        try {
            return safeEvaluate(inputSource, compile(xpath), type);
        } catch (SAXException | IOException | ParserConfigurationException e) {
            throw new XPathExpressionException(e);
        }
    }

    private static String safeEvaluate(InputSource inputSource, String xpath) throws XPathExpressionException {
        try {
            return safeEvaluate(inputSource, compile(xpath));
        } catch (SAXException | IOException | ParserConfigurationException e) {
            throw new XPathExpressionException(e);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> T safeEvaluate(InputSource inputSource, XPathExpression xpath, QName type) throws XPathExpressionException, SAXException, IOException, ParserConfigurationException {
        return (T) xpath.evaluate(DOC_BUILDER_FACTORY.newDocumentBuilder().parse(inputSource), type);
    }

    private static String safeEvaluate(InputSource inputSource, XPathExpression xpath) throws XPathExpressionException, SAXException, IOException, ParserConfigurationException {
        return xpath.evaluate(DOC_BUILDER_FACTORY.newDocumentBuilder().parse(inputSource)).trim();
    }

    private static XPathExpression compile(String xpath) throws XPathExpressionException {
        return XPathFactory.newInstance().newXPath().compile(xpath);
    }

    private static DocumentBuilderFactory createDocumentBuilderFactory() {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            return factory;
        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        }
    }
}
