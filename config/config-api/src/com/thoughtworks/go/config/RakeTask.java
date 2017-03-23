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

@ConfigTag("rake")
public class RakeTask extends BuildTask {
    private final String RAKE = "Rake";
    public static final String TYPE="rake";
    @Override
    public String getTaskType() {
        return "rake";
    }

    public String getTypeForDisplay() {
        return RAKE;
    }

    public String arguments() {
        StringBuffer buffer = new StringBuffer();
        if (buildFile != null) {
            buffer.append("-f ").append('\"').append(FileUtil.normalizePath(buildFile)).append('\"');
        }
        if (target != null) {
            buffer.append(" ").append(target);
        }
        return buffer.toString();
    }

    @Override
    public String command() {
        return "rake";
    }

}
