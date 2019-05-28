/*
 * Copyright 2019 ThoughtWorks, Inc.
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
package com.thoughtworks.go.config;

import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

public class MingleConfigTest {

    @Before
    public void setUp() throws Exception {
    }

    @Test
    public void shouldBuildUrlGivenPath() throws Exception {
        MingleConfig mingleConfig = new MingleConfig("http://foo.bar:7019/baz", "go-project");
        assertThat(mingleConfig.urlFor("/cards/123"), is("http://foo.bar:7019/baz/cards/123"));
        assertThat(mingleConfig.urlFor("cards/123"), is("http://foo.bar:7019/bazcards/123"));

        mingleConfig = new MingleConfig("http://foo.bar:7019/baz/", "go-project");
        assertThat(mingleConfig.urlFor("/cards/123"), is("http://foo.bar:7019/baz/cards/123"));
        assertThat(mingleConfig.urlFor("cards/123"), is("http://foo.bar:7019/baz/cards/123"));

        mingleConfig = new MingleConfig("http://foo.bar:7019", "go-project");
        assertThat(mingleConfig.urlFor("/cards/123"), is("http://foo.bar:7019/cards/123"));

        mingleConfig = new MingleConfig("http://foo.bar:7019/quux?hi=there", "go-project");
        assertThat(mingleConfig.urlFor("/cards/123"), is("http://foo.bar:7019/quux/cards/123?hi=there"));
    }

    @Test
    public void shouldUnderstandGettingMql() {
        MingleConfig mingleConfig = new MingleConfig("http://foo.bar:7019/baz", "go-project", "foo = bar");
        assertThat(mingleConfig.getQuotedMql(), is("\"foo = bar\""));

        mingleConfig = new MingleConfig("http://foo.bar:7019/baz", "go-project");
        assertThat(mingleConfig.getQuotedMql(), is("\"\""));

        mingleConfig = new MingleConfig("http://foo.bar:7019/baz", "go-project", "\"foo\" = 'bar'");
        assertThat(mingleConfig.getQuotedMql(), is("\"\\\"foo\\\" = 'bar'\""));

        mingleConfig = new MingleConfig("http://foo.bar:7019/baz", "go-project", "\\\"foo\\\" = 'bar'");
        assertThat(mingleConfig.getQuotedMql(), is("\"\\\\\\\"foo\\\\\\\" = 'bar'\""));
    }

    @Test
    public void shouldSetMingleConfigFromConfigAttributes() {
        MingleConfig mingleConfig = new MingleConfig();
        Map configMap = new HashMap();
        configMap.put(MingleConfig.BASE_URL, "http://mingle01.tw.com/mingle");
        configMap.put(MingleConfig.PROJECT_IDENTIFIER, "go");
        configMap.put(MingleConfig.MQL_GROUPING_CONDITIONS, Collections.singletonMap(MqlCriteria.MQL, "Some MQL"));

        mingleConfig.setConfigAttributes(configMap);

        assertThat(mingleConfig.getBaseUrl(), is("http://mingle01.tw.com/mingle"));
        assertThat(mingleConfig.getProjectIdentifier(), is("go"));
        assertThat(mingleConfig.getMqlCriteria().getMql(), is("Some MQL"));
    }

    @Test
    public void shouldReturnInvalidIfTheURLIsNotAHTTPSURL() {
        MingleConfig mingleConfig = new MingleConfig("http://some-mingle-instance", "go");
        mingleConfig.validate(new ConfigSaveValidationContext(null));
        assertThat(mingleConfig.errors().isEmpty(), is(false));
        assertThat(mingleConfig.errors().on(MingleConfig.PROJECT_IDENTIFIER), is(nullValue()));
        assertThat(mingleConfig.errors().on(MingleConfig.BASE_URL), is("Should be a URL starting with https://"));
    }

    @Test
    public void shouldReturnInvalidIfTheProjectIdentifierIsInvalid() {
        MingleConfig mingleConfig = new MingleConfig("https://some-mingle-instance", "wrong project identifier");
        mingleConfig.validate(new ConfigSaveValidationContext(null));
        assertThat(mingleConfig.errors().isEmpty(), is(false));
        assertThat(mingleConfig.errors().on(MingleConfig.BASE_URL), is(nullValue()));
        assertThat(mingleConfig.errors().on(MingleConfig.PROJECT_IDENTIFIER), is("Should be a valid mingle identifier."));
    }

    @Test
    public void shouldValidateEmptyConfig() {
        MingleConfig config = new MingleConfig();
        config.validate(null);
        assertThat(config.errors().isEmpty(), is(true));
    }

    @Test
    public void shouldGetQuotedProjectIdentifier() {
        MingleConfig mingleConfig = new MingleConfig("baseUrl", "Ethan's_Project");
        assertThat(mingleConfig.getQuotedProjectIdentifier(), is("\"Ethan's_Project\""));

        mingleConfig = new MingleConfig("baseUrl", "Ethan\"s_Project");
        assertThat(mingleConfig.getQuotedProjectIdentifier(), is("\"Ethan\\\"s_Project\""));
    }

    @Test
    public void shouldRenderStringWithSpecifiedRegexAndLink() throws Exception {
        MingleConfig config = new MingleConfig("http://mingle05", "cce");

        String result = config.render("#111: checkin message");
        assertThat(result, is("<a href=\"" + "http://mingle05/projects/cce/cards/111\" target=\"story_tracker\">#111</a>: checkin message"));
    }

    @Test
    public void shouldReturnOriginalStringIfRegexDoesNotMatch() throws Exception {
        String toRender = "evo-abc: checkin message";

        MingleConfig config = new MingleConfig("http://mingle05", "cce");
        assertThat(config.render(toRender), is(toRender));
    }

    @Test
    public void shouldGetMingleConfigAsTrackingTool() {
        MingleConfig mingleConfig = new MingleConfig("http://foo.bar:7019/baz", "go-project");
        TrackingTool expectedTrackingTool = new TrackingTool("http://foo.bar:7019/baz/projects/go-project/cards/${ID}", "#(\\d+)");

        assertThat(mingleConfig.asTrackingTool(), is(expectedTrackingTool));
    }
}
