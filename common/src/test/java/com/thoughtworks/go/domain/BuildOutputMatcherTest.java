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

import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;

import org.junit.Test;

public class BuildOutputMatcherTest {

    @Test
    public void shouldMatchBuildOutput() {
        BuildOutputMatcher matcher = new BuildOutputMatcher();
        String matchedString = matcher.match("clean:");
        assertThat(matchedString, is("clean"));
    }

    @Test
    public void shouldMatchTargetsWithDash() {
        BuildOutputMatcher matcher = new BuildOutputMatcher();
        String matchedString = matcher.match("The machine is going down:\n-compile.test:");
        assertThat(matchedString, is("compile.test"));
        matchedString = matcher.match("The machine is going down:\n-compile-test:");
        assertThat(matchedString, is("compile-test"));
        matchedString = matcher.match("The machine is going down:\n-compile?test?:");
        assertThat(matchedString, is("compile?test?"));
    }

    @Test
    public void shouldNotMatchIrrelevantOutput() {
        BuildOutputMatcher matcher = new BuildOutputMatcher();
        String matchedString = matcher.match("The machine is going down:");
        assertThat(matchedString, is(nullValue()));
    }

    @Test
    public void shouldMatchMultilineOutput() {
        BuildOutputMatcher matcher = new BuildOutputMatcher();
        String matchedString = matcher.match("The machine is going down:\n-compile:");
        assertThat(matchedString, is("compile"));
    }

    @Test
    public void shouldOnlyMatchAtBeginingAndEndOfLines() {
        BuildOutputMatcher matcher = new BuildOutputMatcher();
        String matchedString = matcher.match("The machine -foobar:is going down:\n-compile:");
        assertThat(matchedString, is("compile"));
    }

    @Test
    public void shouldParseAnt() throws Exception {
        BuildOutputMatcher matcher = new BuildOutputMatcher();
        String output = "foo bar\n"
                + "clean.target:\n"
                + "   [delete] Deleting directory /home/cceuser/projects/hg-cruise/cruise/target";
        String matchedString = matcher.match(output);
        assertThat(matchedString, is("clean.target"));
    }

    @Test
    public void shouldParseRake() throws Exception {
        BuildOutputMatcher matcher = new BuildOutputMatcher();
        String output = "Started\n"
                + "/usr/lib/ruby/1.8/openssl/x509.rb:24: warning: instance variable @config not initialized\n"
                + "/usr/lib/ruby/1.8/openssl/x509.rb:24: warning: instance variable @config not initialized\n"
                + "/usr/lib/ruby/1.8/openssl/x509.rb:24: warning: instance variable @config not initialized\n"
                + "/usr/lib/ruby/1.8/rdoc/parsers/parse_c.rb:204: warning: method redefined; discarding old progress\n"
                + "............................................................."
                + "............................................................."
                + "..........................................................................\n"
                + "Finished in 30.080708 seconds.";
        String matchedString = matcher.match(output);
        assertThat(matchedString, is(get30Dot()));
    }

    public String get30Dot() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 30; i++) {
            sb.append('.');
        }
        return sb.toString();
    }

    @Test
    public void shouldMatchLastAvailableItem() {
        BuildOutputMatcher matcher = new BuildOutputMatcher();
        String matchedString = matcher.match("The machine -foobar:is going down:\n-compile:\n-coverage:");
        assertThat(matchedString, is("coverage"));
    }


}
