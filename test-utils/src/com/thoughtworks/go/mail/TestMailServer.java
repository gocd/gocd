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

package com.thoughtworks.go.mail;

import java.io.File;
import java.io.IOException;

//import com.ericdaugherty.mail.server.Mail;


public class TestMailServer {
    private static File homeFolder;

    public static void start() throws IOException {
//        homeFolder = FileUtil.createTempFolder("jes-" + System.currentTimeMillis());
//
//        FileUtils.copyURLToFile(TestMailServer.class.getResource("log.properties"), new File(homeFolder, "log.conf"));
//        FileUtils.copyURLToFile(TestMailServer.class.getResource("mail.properties"), new File(homeFolder, "mail.conf"));
//        FileUtils.copyURLToFile(TestMailServer.class.getResource("user.properties"), new File(homeFolder, "user.conf"));
//
//        Mail.main(new String[]{homeFolder.getAbsolutePath()});
    }

    public static void stop() {
//        Mail.shutdown();
//        if(homeFolder != null && homeFolder.exists()){
//            homeFolder.delete();
//        }
//
    }
}
