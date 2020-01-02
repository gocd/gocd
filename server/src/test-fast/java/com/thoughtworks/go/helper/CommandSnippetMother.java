/*
 * Copyright 2020 ThoughtWorks, Inc.
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
package com.thoughtworks.go.helper;

import com.thoughtworks.go.server.service.lookups.CommandSnippet;
import com.thoughtworks.go.server.service.lookups.CommandSnippetXmlParser;
import org.apache.commons.lang3.StringUtils;

public class CommandSnippetMother {

    public static CommandSnippet validSnippetWithFileName(String commandName, String fileName) {
        return new CommandSnippetXmlParser().parse(validXMLSnippetContentForCommand(commandName), fileName, String.format("/some/path/%s.xml",fileName));
    }

    public static CommandSnippet validSnippet(final String name) {
        return validSnippetWithFileName(name, name);
    }

    public static CommandSnippet validSnippetWithKeywords(final String name, final String... keywords){
        String comment = "<!--\n" + commandSnippetCommentWithKeywords(name, keywords) + "-->\n";
        return new CommandSnippetXmlParser().parse(comment + validXMLSnippetContentForCommand(name), "filename", "/some/path/filename.xml");
    }

    public static CommandSnippet invalidSnippetWithEmptyCommand(final String fileName){
        return new CommandSnippetXmlParser().parse("<exec command=''><arg>pack</arg><arg>component.nuspec</arg></exec>", fileName, "/some/path/filename.xml");
    }

    public static CommandSnippet invalidSnippetWithInvalidContentInArg(final String name){
        return new CommandSnippetXmlParser().parse(String.format("<exec command='%s'><hello></hello></exec>", name), name, "/some/path/filename.xml");
    }

    public static String validSnippetWithFullComment(String nameInComment, String commandName) {
        return "<!--\n" + commandSnippetComment(nameInComment) + "-->\n"
                + validXMLSnippetContentForCommand(commandName);
    }

    public static String commandSnippetComment(String nameInComment) {
        return commandSnippetCommentWithKeywords(nameInComment, "file", "doc Transfer", "coPY");
    }

    private static String commandSnippetCommentWithKeywords(String nameInComment, String... keywords) {
        String keywordsWithCommasAndSpacesBetweenThem = StringUtils.join(keywords, " , ");
        return "name: " + nameInComment + " \n"
                + "description: some-description\n"
                + "moreinfo: http://some-url\n"
                + "keywords: " + keywordsWithCommasAndSpacesBetweenThem + " \n"
                + "author: Go Team\n"
                + "authorinfo: TWEr@thoughtworks.com\n";
    }

    public static String validXMLSnippetContentForCommand(String commandName) {
        return String.format("<exec command='%s'><arg>pack</arg><arg>component.nuspec</arg></exec>", commandName);
    }
}
