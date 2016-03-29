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

package com.thoughtworks.go.domain;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class DefaultCommentRenderer implements CommentRenderer {
    private static final Log LOGGER = LogFactory.getLog(DefaultCommentRenderer.class);
    private final String link;
    private final String regex;

    public DefaultCommentRenderer(String link, String regex) {
        this.link = link;
        this.regex = regex;
    }

    public String render(String text) {
        if (StringUtils.isBlank(text)) {
            return "";
        }
        if (regex.isEmpty() || link.isEmpty()) {
            Comment comment = new Comment();
            comment.escapeAndAdd(text);
            return comment.render();
        }
        try {
            java.util.regex.Matcher matcher = Pattern.compile(regex).matcher(text);
            int start = 0;
            Comment comment = new Comment();
            while (hasMatch(matcher)) {
                comment.escapeAndAdd(text.substring(start, matcher.start()));
                comment.add(dynamicLink(matcher));
                start = matcher.end();
            }
            comment.escapeAndAdd(text.substring(start));
            return comment.render();
        } catch (PatternSyntaxException e) {
            LOGGER.warn(String.format("Illegal regular expression: %s - %s", regex, e.getMessage()));
        }
        return text;
    }

    private boolean hasMatch(Matcher matcher) {
        return matcher.find() && id(matcher) != null;
    }

    private String dynamicLink(Matcher matcher) {
        String linkWithRealId = StringEscapeUtils.escapeHtml(link.replace("${ID}", id(matcher)));
        return String.format("<a href=\"%s\" target=\"story_tracker\">%s</a>", linkWithRealId, textOnLink(matcher));
    }

    private String textOnLink(Matcher matcher) {
        return StringEscapeUtils.escapeHtml(matcher.group());
    }

    private String contentsOfFirstGroupThatMatched(Matcher matcher) {
        for(int i = 1; i <= matcher.groupCount(); i++) {
            String groupContent = matcher.group(i);
            if (groupContent != null) {
                return groupContent;
            }
        }
        return null;
    }

    private String id(Matcher matcher) {
        return matcher.groupCount() > 0 ? contentsOfFirstGroupThatMatched(matcher) : matcher.group();
    }

    private static class Comment {
        private final StringBuilder buffer = new StringBuilder();

        public String render() {
            return buffer.toString();
        }

        public void escapeAndAdd(String text) {
            buffer.append(StringEscapeUtils.escapeHtml(text));
        }

        public void add(String text) {
            buffer.append(text);
        }
    }
}
