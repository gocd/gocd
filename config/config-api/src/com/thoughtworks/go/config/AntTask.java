/*
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
 */

package com.thoughtworks.go.config;

import com.thoughtworks.go.util.FileUtil;

/**
 * @understands configuration of Ant
 */
@ConfigTag("ant")
public class AntTask extends BuildTask {
    private final String ANT = "Ant";
    public static final String TYPE = "ant";

    @Override
    public String getTaskType() {
        return TYPE;
    }

    public String getTypeForDisplay() {
        return ANT;
    }

    public String arguments() {
        StringBuffer buffer = new StringBuffer();
        if (buildFile != null) {
            buffer.append("-f \"").append(FileUtil.normalizePath(buildFile)).append('\"');
        }
        if (target != null) {
            buffer.append(" ").append(target);
        }
        return buffer.toString();
    }

    @Override
    public String command() {
        return "ant";
    }
}
