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

package com.thoughtworks.go.util.command;

import java.net.MalformedURLException;

import org.hamcrest.core.Is;
import org.hamcrest.core.IsNot;
import org.hamcrest.core.StringContains;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;

public class UrlArgumentTest {
    private static final String URL_WITH_PASSWORD = "http://username:password@somehere";

    private CommandArgument argument;

    @Before
    public void setup() throws MalformedURLException {
        argument = new UrlArgument(URL_WITH_PASSWORD);
    }

    @Test public void shouldReturnStringValueForCommandLine() throws Exception {
        Assert.assertThat(argument.forCommandline(), Is.is(URL_WITH_PASSWORD));
    }

    @Test public void shouldReturnStringValueForReporting() throws Exception {
        Assert.assertThat(argument.forDisplay(), Is.is("http://username:******@somehere"));
    }

    @Test public void shouldReturnValueForToString() throws Exception {
        Assert.assertThat(argument.toString(), Is.is("http://username:******@somehere"));
    }

    @Test public void shouldNotChangeNormalURL() throws Exception {
        String normal = "http://normal/foo/bar/baz?a=b&c=d#fragment";
        UrlArgument url = new UrlArgument(normal);
        Assert.assertThat(url.toString(), Is.is(normal));
    }

    @Test public void shouldWorkWithSvnSshUrl() throws Exception {
        String normal = "svn+ssh://user:password@10.18.7.51:8153";
        UrlArgument url = new UrlArgument(normal);
        Assert.assertThat(url.toString(), Is.is("svn+ssh://user:******@10.18.7.51:8153"));
    }

    @Test public void shouldWorkWithJustUser() throws Exception {
        String normal = "svn+ssh://user@10.18.7.51:8153";
        UrlArgument url = new UrlArgument(normal);
        Assert.assertThat(url.forDisplay(), Is.is("svn+ssh://user@10.18.7.51:8153"));
    }

    @Test public void shouldIgnoreArgumentsThatAreNotRecognisedUrls() throws Exception {
        String notAUrl = "C:\\foo\\bar\\baz";
        UrlArgument url = new UrlArgument(notAUrl);
        Assert.assertThat(url.toString(), Is.is(notAUrl));
    }

    @Test public void shouldBeEqualBasedOnCommandLine() throws Exception {
        UrlArgument url1 = new UrlArgument("svn+ssh://user:password@10.18.7.51:8153");
        UrlArgument url3 = new UrlArgument("svn+ssh://user:password@10.18.7.51:8153");
        Assert.assertThat(url1, is(url3));
    }

    @Test public void shouldBeEqualBasedOnCommandLineForHttpUrls() throws Exception {
        UrlArgument url1 = new UrlArgument("http://user:password@10.18.7.51:8153");
        UrlArgument url2 = new UrlArgument("http://user:other@10.18.7.51:8153");
        UrlArgument url3 = new UrlArgument("http://user:password@10.18.7.51:8153");
        Assert.assertThat(url1, is(url3));
        Assert.assertThat(url1, Is.is(not(url2)));
    }

    @Test public void shouldIgnoreTrailingSlashesOnURIs() throws Exception {
        UrlArgument url1 = new UrlArgument("file:///not-exist/svn/trunk/");
        UrlArgument url2 = new UrlArgument("file:///not-exist/svn/trunk");
        Assert.assertThat(url1, is(url2));
    }

    @Test
     public void shouldMaskPasswordInHgUrlWithBranch(){
         UrlArgument url = new UrlArgument("http://cce:password@10.18.3.171:8080/hg/connect4/trunk#foo");
         Assert.assertThat(url.hostInfoForCommandline(), Is.is("http://cce:password@10.18.3.171:8080"));
         Assert.assertThat(url.hostInfoForDisplay(), Is.is("http://cce:******@10.18.3.171:8080"));
     }

    @Test //BUG #2973
    public void shouldReplaceAllThePasswordsInSvnInfo() throws Exception {
        String output = "<?xml version=\"1.0\"?>\n"
                + "<info>\n"
                + "<entry\n"
                + "   kind=\"dir\"\n"
                + "   path=\".\"\n"
                + "   revision=\"294\">\n"
                + "<url>http://cce:password@10.18.3.171:8080/svn/connect4/trunk</url>\n"
                + "<repository>\n"
                + "<root>http://cce:password@10.18.3.171:8080/svn/connect4</root>\n"
                + "<uuid>b7cc39fa-2f96-0d44-9079-2001927d4b22</uuid>\n"
                + "</repository>\n"
                + "<wc-info>\n"
                + "<schedule>normal</schedule>\n"
                + "<depth>infinity</depth>\n"
                + "</wc-info>\n"
                + "<commit\n"
                + "   revision=\"294\">\n"
                + "<author>cce</author>\n"
                + "<date>2009-06-09T06:13:05.109375Z</date>\n"
                + "</commit>\n"
                + "</entry>\n"
                + "</info>";

        UrlArgument url = new UrlArgument("http://cce:password@10.18.3.171:8080/svn/connect4/trunk");
        String result = url.replaceSecretInfo(output);
        Assert.assertThat(result, StringContains.containsString("<url>http://cce:******@10.18.3.171:8080/svn/connect4/trunk</url>"));
        Assert.assertThat(result, StringContains.containsString("<root>http://cce:******@10.18.3.171:8080/svn/connect4</root>"));
        Assert.assertThat(result, IsNot.not(StringContains.containsString("cce:password")));
    }

    @Test //BUG #5471
    public void shouldMaskAuthTokenInUrl() {
        UrlArgument url = new UrlArgument("https://9bf58jhrb32f29ad0c3983a65g594f1464jgf9a3@somewhere");
        Assert.assertThat(url.forDisplay(), Is.is("https://******@somewhere"));
    }
}
