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
package com.thoughtworks.go.server;

import org.apache.commons.io.FileUtils;
import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import java.io.File;
import java.io.FileInputStream;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;


public class JUnitReportGenerator {

    public static void main(String[] args) throws Exception {
        Document doc = new SAXReader().read(new FileInputStream(new File("/home/cruise/sample_junit.xml")));
        Element suite = (Element) doc.selectSingleNode("//testsuite");
        Element rootElement = doc.getRootElement();
        for (int i = 0; i < 50000; i++) {
            Element copy = suite.createCopy();
            setAttr(i, copy, "name");
            setAttr(i, copy, "hostname");
            List<Element> elements = copy.selectNodes(".//testcase");
            for (Element element : elements) {
                setAttr(i, element, "classname");
                setAttr(i, element, "name");
            }
            rootElement.add(copy);
        }
        FileUtils.writeStringToFile(new File("/tmp/repo/imagine.xml"), doc.asXML(), UTF_8);
    }

    private static void setAttr(int i, Element test, final String attr) {
        Attribute classname = test.attribute(attr);
        classname.setValue(classname.getValue() + i);
    }
}