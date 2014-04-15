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

package com.thoughtworks.go.junitext;

import com.googlecode.junit.ext.checkers.Checker;
import com.thoughtworks.go.util.StringUtil;

public class TagChecker implements Checker {
    private String[] tags;

    public TagChecker(String[] tags) {
        this.tags = tags;
    }
    public TagChecker(String tag) {
        this.tags = new String[]{tag};
    }

    @Override
    public boolean satisfy() {
        String testTags = System.getProperty("test.tags");
        if (StringUtil.isBlank(testTags)) {
            return false;
        }
        for (String tag : tags) {
            if (!testTags.contains(tag)) {
                return false;
            }
        }

        return true;
    }
}
