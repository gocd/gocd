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

import java.util.Arrays;

import com.thoughtworks.go.helper.CommandSnippetMother;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class CommandSnippetXmlParserTest {
    private CommandSnippetXmlParser xmlParser;

    @Before
    public void setUp() {
        xmlParser = new CommandSnippetXmlParser();
    }

    @Test
    public void shouldCreateCommandSnippetFromValidXmlWithoutComment() {
        String fileName = "fileName";
        CommandSnippet commandSnippet = xmlParser.parse("<exec command='ls'><arg>-la</arg><arg>file</arg></exec>", fileName, "/some/path/filename.xml");

        assertThat(commandSnippet.isValid(), is(true));
        assertThat(commandSnippet.getCommandName(), is("ls"));
        assertThat(commandSnippet.getArguments(), is(Arrays.asList("-la", "file")));
        assertThat(commandSnippet.getBaseFileName(), is(fileName));
        assertThat(commandSnippet.getName(), is(fileName));
        assertThat(commandSnippet.getRelativePath(), is("/some/path/filename.xml"));
    }

    @Test
    public void shouldThrowExceptionIfXmlIsMalformed() throws Exception {
        CommandSnippet commandSnippet = xmlParser.parse("<exec command='nuget' >/exec>", "nuget-filename", "/some/path/filename.xml");
        assertThat(commandSnippet.isValid(), is(false));
        assertThat(commandSnippet.getName(), is("nuget-filename"));
    }

    @Test
    public void shouldNotConsiderXmlValidIfCommandTagIsEmpty() throws Exception {
        CommandSnippet commandSnippet = xmlParser.parse("<exec command=' ' ><arg>pack</arg><arg>component.nuspec</arg></exec>", "nuget", "/some/path/filename.xml");
        assertThat(commandSnippet.getErrorMessage(), is("Reason: Command attribute cannot be blank in a command snippet."));
        assertThat(commandSnippet.isValid(), is(false));
    }

    @Test
    public void shouldNotConsiderXmlValidIfItContainsInvalidTags() throws Exception {
        CommandSnippet commandSnippet = xmlParser.parse("<exec command='nuget' invalidTag='foo'><arg>pack</arg><arg>component.nuspec</arg></exec>", "nuget", "/some/path/filename.xml");
        assertThat(commandSnippet.getErrorMessage(), is("Reason: Attribute 'invalidTag' is not allowed to appear in element 'exec'."));
        assertThat(commandSnippet.isValid(), is(false));
    }

    @Test
    public void shouldNotConsiderXmlValidIfItContainsMoreThanOneExecTag() throws Exception {
        CommandSnippet commandSnippet = xmlParser.parse("<exec command='nuget' ></exec><exec command='ls'></exec>", "nuget", "/some/path/filename.xml");
        assertThat(commandSnippet.getErrorMessage(), is("Reason: Error on line 1: The markup in the document following the root element must be well-formed."));
        assertThat(commandSnippet.isValid(), is(false));
    }

    @Test
    public void shouldConsiderXmlInvalidIfRootTagIsNotExec() throws Exception {
        CommandSnippet commandSnippet = xmlParser.parse("<hello command='nuget' ><arg>pack</arg><arg>component.nuspec</arg></hello>", "nuget", "/some/path/filename.xml");
        assertThat(commandSnippet.getErrorMessage(), is("Reason: Invalid XML tag \"hello\" found."));
        assertThat(commandSnippet.isValid(), is(false));
    }

    @Test
    public void shouldConsiderXmlInvalidIfExecHasChildrenThatAreNotArgTag() throws Exception {
        CommandSnippet commandSnippet = xmlParser.parse("<exec command='nuget' ><arg>pack</arg><hello>component.nuspec</hello></exec>", "nuget", "/some/path/filename.xml");
        assertThat(commandSnippet.getErrorMessage(), is("Reason: Invalid content was found starting with element 'hello'. One of '{arg}' is expected."));
        assertThat(commandSnippet.isValid(), is(false));
    }

    @Test
    public void shouldGetCommandNameFromNameTagInSnippetComment() throws Exception {
        String snippetWithFullComment = CommandSnippetMother.validSnippetWithFullComment("blah", "ls");
        CommandSnippet commandSnippet = xmlParser.parse(snippetWithFullComment, "filename", "/some/path/filename.xml");

        assertThat(commandSnippet.getName(), is("blah"));
    }

    @Test
    public void shouldConsiderXmlInvalidIfThereIsNoMatchingClosingTag () throws Exception {
        CommandSnippet commandSnippet = xmlParser.parse("<exec command='nuget'> <arg>pack </exec>", "nuget", "/some/path/filename.xml");
        assertThat(commandSnippet.getErrorMessage(), is("Reason: Error on line 1: The element type \"arg\" must be terminated by the matching end-tag \"</arg>\"."));
        assertThat(commandSnippet.isValid(), is(false));
    }

    @Test
    public void shouldConsiderXmlInvalidIfContainsUpperCaseTags () throws Exception {
        CommandSnippet commandSnippet = xmlParser.parse("<EXEC COMMAND='NUGET'><ARG>PACK</ARG></EXEC>", "nuget", "/some/path/filename.xml");
        assertThat(commandSnippet.getErrorMessage(), is("Reason: Invalid XML tag \"EXEC\" found."));
        assertThat(commandSnippet.isValid(), is(false));
    }

    @Test
    public void shouldConsiderXmlInvalidIfXmlFileIsBlank () throws Exception {
        CommandSnippet commandSnippet = xmlParser.parse("", "nuget", "/some/path/filename.xml");
        assertThat(commandSnippet.getErrorMessage(), is("Reason: Error on line -1: Premature end of file."));
        assertThat(commandSnippet.isValid(), is(false));
    }
}
