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

package com.thoughtworks.go.server;

import org.mortbay.jetty.webapp.WebXmlConfiguration;
import org.mortbay.xml.XmlParser;
import org.xml.sax.SAXException;

import javax.servlet.UnavailableException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class Jetty6GoWebXmlConfiguration extends WebXmlConfiguration {
    private static String webdefaultXml = "WEB-INF/webdefault-jetty6.xml";

    public void initialize(String warFileLocation) throws IOException, SAXException, UnavailableException, ClassNotFoundException {
        super.initialize(configuration(WebXmlConfiguration.webXmlParser(), warFileLocation));
    }

    private XmlParser.Node configuration(XmlParser parser, String warFile) throws IOException, SAXException {
        XmlParser.Node node;
        if (new File(warFile).isDirectory()) {
            FileInputStream in = new FileInputStream(new File(warFile, webdefaultXml));
            node = parser.parse(in);
            in.close();
        }
        else {
            ZipFile file = new ZipFile(warFile);
            node = parser.parse(file.getInputStream(new ZipEntry(webdefaultXml)));
            file.close();
        }
        return node;
    }
}
