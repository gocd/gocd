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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.io.IOUtils;
import org.xml.sax.InputSource;

public class XpathUtils {

    private static XPathFactory xPathFactory = XPathFactory.newInstance();

    public static String evaluate(File file, String xpath)
            throws XPathExpressionException, FileNotFoundException {
        InputStream stream = (new FileInputStream(file));
        try {
            InputSource inputSource = new InputSource(stream);
            return evaluate(xPathFactory, inputSource, xpath);
        } finally {
            IOUtils.closeQuietly(stream);
        }
    }

    public static boolean nodeExists(File file, String xpath) throws XPathExpressionException, FileNotFoundException {
        FileInputStream stream = new FileInputStream(file);
        try {
            return nodeExists(new InputSource(stream), xpath);
        } finally {
            IOUtils.closeQuietly(stream);
        }
    }

    public static boolean nodeExists(InputSource inputSource, String xpath) throws XPathExpressionException {
        XPathFactory factory = XPathFactory.newInstance();
        XPathExpression expression = factory.newXPath().compile(xpath);
        Boolean b = (Boolean) expression.evaluate(inputSource, XPathConstants.BOOLEAN);
        return b != null && b;
    }

    private static String evaluate(XPathFactory factory, InputSource inputSource, String xpath)
            throws XPathExpressionException {
        XPathExpression expression = factory.newXPath().compile(xpath);
        return expression.evaluate(inputSource).trim();
    }
}
