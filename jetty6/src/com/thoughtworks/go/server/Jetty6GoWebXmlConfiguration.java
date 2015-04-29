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

import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;

public class Jetty6GoWebXmlConfiguration {
    private static String webdefaultXml = "WEB-INF/webdefault-jetty6.xml";

    public static String configuration(String warFile) throws IOException, SAXException {
        if (new File(warFile).isDirectory()) {
            return new File(warFile, webdefaultXml).getPath();
        }
        return "jar:file:" + warFile + "!/" + webdefaultXml;
    }
}
