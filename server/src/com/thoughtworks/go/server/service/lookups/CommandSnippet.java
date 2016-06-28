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

package com.thoughtworks.go.server.service.lookups;

import java.util.ArrayList;
import java.util.List;

import com.thoughtworks.go.util.StringUtil;

/**
 * @understands: a single task snippet
 */
public class CommandSnippet {

    private final String baseFileName;
    private String commandName;
    private List<String> arguments = new ArrayList<>();
    private CommandSnippetComment comment;
    private String relativeFilePath = "";

    private boolean valid = true;
    private String errorMessage;

    public static CommandSnippet invalid(String fileName, String errorMessage, CommandSnippetComment comment) {
        return new CommandSnippet(fileName, errorMessage, comment);
    }

    public CommandSnippet(String commandName, List<String> arguments, CommandSnippetComment commandSnippetComment, String fileName,
                          String relativeFilePath) {
        this.baseFileName = fileName;
        this.commandName = commandName;
        this.arguments = arguments;
        this.comment = commandSnippetComment;
        this.relativeFilePath = relativeFilePath;
    }

    private CommandSnippet(String fileName, String errorMessage, CommandSnippetComment comment) {
        this.baseFileName = fileName;
        this.errorMessage = errorMessage;
        this.comment = comment;
        this.valid = false;
    }

    public String getDescription(){
        return comment.getDescription();
    }

    public String getAuthor(){
        return comment.getAuthor();
    }

    public String getAuthorInfo(){
        return comment.getAuthorInfo();
    }

    public String getMoreInfo(){
        return comment.getMoreInfo();
    }

    public String getName() {
        return StringUtil.isBlank(comment.getName()) ? getBaseFileName() : comment.getName();
    }

    public String getBaseFileName() {
        return baseFileName;
    }

    public String getCommandName() {
        return commandName;
    }

    public List<String> getArguments() {
        return arguments;
    }

    public boolean isValid() {
        return valid;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public String getRelativePath() {
        return relativeFilePath;
    }

    public boolean isExactMatchOfName(String nameToCheckAgainst) {
        return getName().equalsIgnoreCase(nameToCheckAgainst);
    }

    public boolean hasNameWhichStartsWith(String prefixToCheckAgainst) {
        return getName().toLowerCase().startsWith(prefixToCheckAgainst);
    }

    public boolean containsKeyword(String keyword) {
        return comment.getKeywords().contains(keyword);
    }

    @Override
    public String toString() {
        return org.apache.commons.lang.builder.ToStringBuilder.reflectionToString(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        CommandSnippet that = (CommandSnippet) o;

        if (arguments != null ? !arguments.equals(that.arguments) : that.arguments != null) {
            return false;
        }
        if (baseFileName != null ? !baseFileName.equals(that.baseFileName) : that.baseFileName != null) {
            return false;
        }
        if (commandName != null ? !commandName.equals(that.commandName) : that.commandName != null) {
            return false;
        }
        if (relativeFilePath != null ? !relativeFilePath.equals(that.relativeFilePath) : that.relativeFilePath != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = baseFileName != null ? baseFileName.hashCode() : 0;
        result = 31 * result + (commandName != null ? commandName.hashCode() : 0);
        result = 31 * result + (arguments != null ? arguments.hashCode() : 0);
        result = 31 * result + (relativeFilePath != null ? relativeFilePath.hashCode() : 0);
        return result;
    }
}
