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

package com.thoughtworks.go.domain.materials.git;

import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.util.DateUtils;

public class GitModificationParser {
    private LinkedList<Modification> modifications = new LinkedList<>();
    private static final String SPACES = "\\s+";
    private static final String COMMENT_INDENT = "\\s{4}";
    private static final String COMMENT_TEXT = "(.*)";
    private static final String HASH = "(\\w+)";
    private static final String DATE = "(.+)";
    private static final String AUTHOR = "(.+)";
    private static final Pattern COMMIT_PATTERN = Pattern.compile("^commit" + SPACES + HASH + "$");
    private static final Pattern AUTHOR_PATTERN = Pattern.compile("^Author:"+ SPACES + AUTHOR + "$");
    private static final Pattern DATE_PATTERN = Pattern.compile("^Date:" + SPACES + DATE + "$");
    private static final Pattern COMMENT_PATTERN = Pattern.compile("^" + COMMENT_INDENT + COMMENT_TEXT + "$");

    public List<Modification> parse(List<String> output) {
        for (String line : output) {
            processLine(line);
        }
        return modifications;
    }

    public List<Modification> getModifications() {
        return modifications;
    }

    public void processLine(String line) {
        Matcher matcher = COMMIT_PATTERN.matcher(line);
        if (matcher.matches()) {
            modifications.add(new Modification("", "", null, null, matcher.group(1)));
        }
        Matcher authorMatcher = AUTHOR_PATTERN.matcher(line);
        if (authorMatcher.matches()) {
            modifications.getLast().setUserName(authorMatcher.group(1));
        }
        Matcher dateMatcher = DATE_PATTERN.matcher(line);
        if (dateMatcher.matches()) {
            modifications.getLast().setModifiedTime(DateUtils.parseISO8601(dateMatcher.group(1)));
        }
        Matcher commentMatcher = COMMENT_PATTERN.matcher(line);
        if (commentMatcher.matches()) {
            Modification last = modifications.getLast();
            String comment = last.getComment();
            if (!comment.isEmpty()) comment += "\n";
            last.setComment(comment + commentMatcher.group(1));
       }
    }
}
