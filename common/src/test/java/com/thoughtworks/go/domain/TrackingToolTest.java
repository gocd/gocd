/*
 * Copyright 2015 ThoughtWorks, Inc.
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
 */

package com.thoughtworks.go.domain;

import java.util.HashMap;
import java.util.List;

import com.thoughtworks.go.config.TrackingTool;
import org.junit.Test;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class TrackingToolTest {
    private TrackingTool trackingTool;

    @Test
    public void shouldSetTrackingToolAttributesFromConfigMap() {
        trackingTool = new TrackingTool();
        HashMap attributeMap = new HashMap();
        String expectedLink = "http://blah.com";
        attributeMap.put(TrackingTool.LINK, expectedLink);
        String expectedRegex = "[a-z]*";
        attributeMap.put(TrackingTool.REGEX, expectedRegex);
        trackingTool.setConfigAttributes(attributeMap);
        assertThat(trackingTool.getLink(), is(expectedLink));
        assertThat(trackingTool.getRegex(), is(expectedRegex));
    }

    @Test
    public void shouldEnsureTrackingToolLinkContainsIDForTheMatchingRegexGroup() {
        TrackingTool trackingTool = new TrackingTool("http://no-id.com", "some-regex");
        trackingTool.validate(null);
        ConfigErrors configErrors = trackingTool.errors();
        List<String> errors = configErrors.getAllOn(TrackingTool.LINK);
        assertThat(errors.size(), is(1));
        assertThat(errors, hasItem("Link must be a URL containing '${ID}'. Go will replace the string '${ID}' with the first matched group from the regex at run-time."));
    }

    @Test
    public void shouldPopulateErrorsWhenOnlyLinkOrOnlyRegexIsSpecified() {
       trackingTool = new TrackingTool("link", "");
       trackingTool.validate(null);
       assertThat(trackingTool.errors().on(TrackingTool.REGEX), is("Regex should be populated"));

       trackingTool = new TrackingTool("", "regex");
       trackingTool.validate(null);
       assertThat(trackingTool.errors().on(TrackingTool.LINK), is("Link should be populated"));
    }

    @Test
    public void shouldNotPopulateErrorsWhenTimerSpecIsValid() {
        trackingTool = new TrackingTool("myLink-${ID}", "myRegex");
        trackingTool.validate(null);
        assertThat(trackingTool.errors().isEmpty(), is(true));
    }

    @Test
    public void shouldRenderStringWithSpecifiedRegexAndLink() throws Exception {
        TrackingTool config = new TrackingTool("http://mingle05/projects/cce/cards/${ID}", "#(\\d+)");

        String result = config.render("#111: checkin message");
        assertThat(result, is("<a href=\"" + "http://mingle05/projects/cce/cards/111\" target=\"story_tracker\">#111</a>: checkin message"));
    }

    @Test
    public void shouldReturnOriginalStringIfRegexDoesNotMatch() throws Exception {
        String toRender = "evo-abc: checkin message";

        TrackingTool config = new TrackingTool("http://mingle05/projects/cce/cards/${ID}", "#(\\d+)");
        assertThat(config.render(toRender), is(toRender));
    }

    @Test
    public void shouldValidate(){
        TrackingTool tool = new TrackingTool();
        tool.validateTree(null);
        assertThat(tool.errors().on(TrackingTool.LINK), is("Link should be populated"));
        assertThat(tool.errors().on(TrackingTool.REGEX), is("Regex should be populated"));
    }
}
