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

package com.thoughtworks.go.legacywrapper;

import java.io.File;
import java.io.FilenameFilter;

import com.thoughtworks.go.server.util.CCDateFormatter;

import org.joda.time.DateTime;

class NearestLogFilter implements FilenameFilter {
    private final long time;

    private long duration = Long.MAX_VALUE;

    public NearestLogFilter(DateTime time) {
        this.time = Long.parseLong(CCDateFormatter.yyyyMMddHHmmss(time));
    }

    public boolean accept(File dir, String name) {
        long diff = Math.abs(Long.parseLong(CCDateFormatter.getBuildDateFromLogFileName(name)) - time);
        if (0 < diff && diff < duration) {
            duration = diff;
            return true;
        }
        return false;
    }
}