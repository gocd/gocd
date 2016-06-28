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

package com.thoughtworks.go.domain;

import com.google.gson.Gson;
import com.thoughtworks.go.websocket.MessageEncoding;
import org.junit.Test;

import static com.thoughtworks.go.util.MapBuilder.map;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class BuildCommandTest {

    @Test
    public void testGetArgs() {
        assertThat(new BuildCommand("foo", map("foo", new Gson().toJson(new String[]{"arg1", "arg2"}))).getArrayArg("foo"), is(new String[]{"arg1","arg2"}));
        assertThat(new BuildCommand("foo", map("foo", "true")).getBooleanArg("foo"), is(true));
        assertThat(new BuildCommand("foo", map("foo", "true")).getBooleanArg("bar"), is(false));
        assertThat(new BuildCommand("foo", map("foo", "bar")).getStringArg("foo"), is("bar"));
    }


    @Test
    public void defaultSubCommandsShouldBeEmpty() {
        assertThat(new BuildCommand("foo").getSubCommands().size(), is(0));
        assertThat(new BuildCommand("foo", map("arg1", "42")).getSubCommands().size(), is(0));
    }

    @Test
    public void testDumpComposedCommand() {
        assertThat(BuildCommand.compose(new BuildCommand("bar1"), BuildCommand.compose(new BuildCommand("barz"))).dump(), is("compose\n    bar1\n    compose\n        barz"));
    }

    @Test
    public void defaultRunIfIsPassed() {
        assertThat(new BuildCommand("cmd").getRunIfConfig(), is("passed"));
        assertThat(new BuildCommand("cmd").runIf("any").getRunIfConfig(), is("any"));
    }

    @Test
    public void encodeDecode() {
        BuildCommand bc = BuildCommand.compose(new BuildCommand("bar1", map("arg1", "1", "arg2", "2")), BuildCommand.compose(new BuildCommand("barz")));
        bc.setRunIfConfig("any");
        bc.setTest(new BuildCommand("t", map("k1", "v1")));
        bc.setOnCancel(BuildCommand.compose(BuildCommand.echo("foo"), BuildCommand.echo("bar")));
        assertThat(MessageEncoding.decodeData(MessageEncoding.encodeData(bc), BuildCommand.class), is(bc));
    }
}
