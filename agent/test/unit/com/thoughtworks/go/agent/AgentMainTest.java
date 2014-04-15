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

package com.thoughtworks.go.agent;

import com.thoughtworks.go.util.SystemEnvironment;
import org.junit.experimental.theories.DataPoint;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

@RunWith(Theories.class)
public class AgentMainTest {

    static class UrlChange {
        final String from;
        final String to;

        UrlChange(String from, String to) {
            this.from = from;
            this.to = to;
        }

        @Override public String toString() {
            return "UrlChange{" +
                    "from='" + from + '\'' +
                    ", to='" + to + '\'' +
                    '}';
        }
    }
    
    @DataPoint public static UrlChange fromCruiseToGo = new UrlChange("https://foo.bar:8154/cruise", "https://foo.bar:8154/go/");
    @DataPoint public static UrlChange fromCruiseSlashToGo = new UrlChange("https://foo.bar:8154/cruise/", "https://foo.bar:8154/go/");
    @DataPoint public static UrlChange noReplacementNeededAsDoesNotEndInCruise = new UrlChange("https://foo.bar:8154/cruise/foo/bar", "https://foo.bar:8154/cruise/foo/bar");
    @DataPoint public static UrlChange fromSomethingPrefixingCruiseToGo = new UrlChange("https://foo.bar:8154/prefix/cruise", "https://foo.bar:8154/prefix/go/");
    @DataPoint public static UrlChange noReplacementNeeded = new UrlChange("https://foo.bar:8154/prefix/suffix", "https://foo.bar:8154/prefix/suffix");
    @DataPoint public static UrlChange noReplacementNeededWithCruiser = new UrlChange("https://foo.bar:8154/prefix/cruiser", "https://foo.bar:8154/prefix/cruiser");

    @Theory
    public void shouldOnlyReplaceOldUrlWith_Cruise_ContextWith_Go(UrlChange urlChange) {
        AgentMain.setServiceUrl(urlChange.from);
        assertThat(new SystemEnvironment().getServiceUrl(), is(urlChange.to));
    }
}
