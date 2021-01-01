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

import java.io.File;

public class GoWebXmlConfiguration {

    private static String webdefaultXml = "WEB-INF/webdefault.xml";

    public static String configuration(String warFile) {
        if (new File(warFile).isDirectory()) {
            return new File(warFile, webdefaultXml).getPath();
        }
        return "jar:file:" + warFile + "!/" + webdefaultXml;
    }
}
