/*
 * Copyright Thoughtworks, Inc.
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
package com.thoughtworks.go.server.util;

import com.thoughtworks.go.server.GoServer;
import com.thoughtworks.go.util.SystemEnvironment;
import lombok.experimental.UtilityClass;

@UtilityClass
public final class GoLauncher {
    public static void main(String[] args) {
        ServerLogging.initialize();

        new SystemEnvironment().setProperty(SystemEnvironment.USE_COMPRESSED_JAVASCRIPT, Boolean.toString(true));

        try {
            new GoServer().go();
        } catch (Exception e) {
            System.err.println("ERROR: Failed to start GoCD server. Please check the logs.");
            //noinspection CallToPrintStackTrace
            e.printStackTrace();
            System.exit(1);
        }
    }
}
