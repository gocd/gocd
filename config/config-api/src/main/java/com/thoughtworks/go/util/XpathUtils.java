/*
 * Copyright 2021 ThoughtWorks, Inc.
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

import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.*;
import java.nio.charset.StandardCharsets;

public class XpathUtils {

    private static XPathFactory xPathFactory = XPathFactory.newInstance();

    public static String evaluate(File file, String xpath)
            throws XPathExpressionException, IOException {
        try(InputStream stream = new FileInputStream(file)) {
            InputSource inputSource = new InputSource(stream);
            return evaluate(xPathFactory, inputSource, xpath);
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
        XPathFactory factory = XPathFactory.newInstance();
        XPathExpression expression = factory.newXPath().compile(xpath);
        Boolean b = (Boolean) expression.evaluate(inputSource, XPathConstants.BOOLEAN);
        return b != null && b;
    }

    public static boolean nodeExists(String xmlPartial, String xpath) throws XPathExpressionException {
        return nodeExists(new ByteArrayInputStream(xmlPartial.getBytes(StandardCharsets.UTF_8)), xpath);
    }

    private static String evaluate(XPathFactory factory, InputSource inputSource, String xpath)
            throws XPathExpressionException {
        XPathExpression expression = factory.newXPath().compile(xpath);
        return expression.evaluate(inputSource).trim();
    }

}
