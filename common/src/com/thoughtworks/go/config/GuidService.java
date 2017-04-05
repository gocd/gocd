/* ************************GO-LICENSE-START*********************************
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
 * ************************GO-LICENSE-END***********************************/

package com.thoughtworks.go.config;

import com.thoughtworks.go.util.SystemEnvironment;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;

import static com.thoughtworks.go.util.ExceptionUtils.bomb;

public class GuidService {
    private static final File AGENT_GUID_FILE = new File(new SystemEnvironment().getConfigDir(), "guid.txt");

    public static void storeGuid(String guid) {
        try {
            AGENT_GUID_FILE.delete();
            FileUtils.writeStringToFile(AGENT_GUID_FILE, guid);
        } catch (IOException ioe) {
            throw bomb("Couldn't save GUID to filesystem", ioe);
        }
    }

    public static String loadGuid() {
        try {
            return FileUtils.readFileToString(AGENT_GUID_FILE).trim();
        } catch (IOException ioe) {
            throw bomb("Couldn't load GUID from filesystem", ioe);
        }
    }

    public static void deleteGuid() {
        AGENT_GUID_FILE.delete();
    }

    public static boolean guidPresent() {
        try {
            String guid = FileUtils.readFileToString(AGENT_GUID_FILE);
            return (guid != null && guid.length() > 0);
        } catch (IOException ioe) {
            return false;
        }
    }
}
