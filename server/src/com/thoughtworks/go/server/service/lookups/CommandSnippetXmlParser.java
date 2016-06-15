/*************************GO-LICENSE-START*********************************
 * Copyright 2016 ThoughtWorks, Inc.
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

package com.thoughtworks.go.server.service.lookups;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.thoughtworks.go.util.StringUtil;
import org.apache.log4j.Logger;
import org.jdom.Comment;
import org.jdom.Document;
import org.jdom.Element;

import static com.thoughtworks.go.util.XmlUtils.buildXmlDocument;

public class CommandSnippetXmlParser {
    private static final Logger LOGGER = Logger.getLogger(CommandSnippetXmlParser.class);

    public CommandSnippet parse(String xmlContent, String fileName, String relativeFilePath) {
        try {
            Document document = buildXmlDocument(xmlContent, CommandSnippet.class.getResource("command-snippet.xsd"));
            CommandSnippetComment comment = getComment(document);

            Element execTag = document.getRootElement();
            String commandName = execTag.getAttributeValue("command");
            List<String> arguments = new ArrayList<>();
            for (Object child : execTag.getChildren()) {
                Element element = (Element) child;
                arguments.add(element.getValue());
            }
            return new CommandSnippet(commandName, arguments, comment, fileName, relativeFilePath);
        } catch (Exception e) {
            String errorMessage = String.format("Reason: %s", e.getMessage());
            LOGGER.info(String.format("Could not load command '%s'. %s", fileName, errorMessage));
            return CommandSnippet.invalid(fileName, errorMessage, new EmptySnippetComment());
        }
    }

    private CommandSnippetComment getComment(Document document) {
        for (Object content : document.getContent()) {
            if (content instanceof Comment) {
                return new CommandSnippetTextComment(((Comment) content).getText());
            }
        }
        return new EmptySnippetComment();
    }

    static class CommandSnippetTextComment implements CommandSnippetComment {
        private final String SPACES = "\\s*";
        private final String content;
        private final Pattern commentLineKeyValuePattern = Pattern.compile(String.format("^%s(.+?)%s:%s(.+?)%s$", SPACES, SPACES, SPACES, SPACES));
        private Map<String, String> snippetTagMap = new HashMap<>();

        public CommandSnippetTextComment(String content) {
            this.content = content;
            constructMapOfSnippetTags();
        }

        private void constructMapOfSnippetTags() {
            String[] linesInComment = content.split("\n");
            for (String lineInComment : linesInComment) {
                Matcher matcher = commentLineKeyValuePattern.matcher(lineInComment);
                if (matcher.matches()) {
                    snippetTagMap.put(matcher.group(1).toLowerCase(), matcher.group(2));
                }
            }
        }

        @Override
        public String getName() {
            return snippetTagMap.get("name");
        }

        @Override
        public String getDescription() {
            return snippetTagMap.get("description");
        }

        @Override
        public String getAuthor() {
            return snippetTagMap.get("author");
        }

        @Override
        public List<String> getKeywords() {
            String keywords = snippetTagMap.get("keywords");
            if (StringUtil.isBlank(keywords)) {
                return new ArrayList<>();
            }
            return Arrays.asList(keywords.toLowerCase().split(String.format("%s,%s", SPACES, SPACES)));
        }

        @Override
        public String getAuthorInfo() {
            return snippetTagMap.get("authorinfo");
        }

        @Override
        public String getMoreInfo() {
            return snippetTagMap.get("moreinfo");
        }
    }
}
