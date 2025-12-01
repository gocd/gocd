/*
 * Copyright Thoughtworks, Inc.
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

import com.thoughtworks.go.domain.ConfigErrors;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class TrackingToolTest {
    private TrackingTool trackingTool;

    @Test
    public void shouldSetTrackingToolAttributesFromConfigMap() {
        trackingTool = new TrackingTool();
        Map<String, String> attributeMap = new HashMap<>();
        String expectedLink = "http://blah.com";
        attributeMap.put(TrackingTool.LINK, expectedLink);
        String expectedRegex = "[a-z]*";
        attributeMap.put(TrackingTool.REGEX, expectedRegex);
        trackingTool.setConfigAttributes(attributeMap);
        assertThat(trackingTool.getLink()).isEqualTo(expectedLink);
        assertThat(trackingTool.getRegex()).isEqualTo(expectedRegex);
    }

    @Test
    public void shouldEnsureTrackingToolLinkContainsIDForTheMatchingRegexGroup() {
        TrackingTool trackingTool = new TrackingTool("http://no-id.com", "some-regex");
        trackingTool.validate(null);
        ConfigErrors configErrors = trackingTool.errors();
        List<String> errors = configErrors.getAllOn(TrackingTool.LINK);
        assertThat(errors.size()).isEqualTo(1);
        assertThat(errors).contains("Link must be a URL containing '${ID}'. Go will replace the string '${ID}' with the first matched group from the regex at run-time.");
    }

    @Test
    public void shouldPopulateErrorsWhenOnlyLinkOrOnlyRegexIsSpecified() {
        trackingTool = new TrackingTool("link", "");
        trackingTool.validate(null);
        assertThat(trackingTool.errors().firstErrorOn(TrackingTool.REGEX)).isEqualTo("Regex should be populated");

        trackingTool = new TrackingTool("", "regex");
        trackingTool.validate(null);
        assertThat(trackingTool.errors().firstErrorOn(TrackingTool.LINK)).isEqualTo("Link should be populated");
    }

    @Test
    public void shouldNotPopulateErrorsWhenTimerSpecIsValid() {
        trackingTool = new TrackingTool("http://myLink-${ID}", "myRegex");
        trackingTool.validate(null);
        assertThat(trackingTool.errors().isEmpty()).isTrue();

        trackingTool = new TrackingTool("https://myLink-${ID}", "myRegex");
        trackingTool.validate(null);
        assertThat(trackingTool.errors().isEmpty()).isTrue();
    }

    @Test
    public void shouldRenderStringWithSpecifiedRegexAndLink() {
        TrackingTool config = new TrackingTool("http://mingle05/projects/cce/cards/${ID}", "#(\\d+)");

        String result = config.render("#111: checkin message");
        assertThat(result).isEqualTo("<a href=\"" + "http://mingle05/projects/cce/cards/111\" target=\"story_tracker\">#111</a>: checkin message");
    }

    @Test
    public void shouldReturnOriginalStringIfRegexDoesNotMatch() {
        String toRender = "evo-abc: checkin message";

        TrackingTool config = new TrackingTool("http://mingle05/projects/cce/cards/${ID}", "#(\\d+)");
        assertThat(config.render(toRender)).isEqualTo(toRender);
    }

    @Test
    public void shouldValidate() {
        TrackingTool tool = new TrackingTool();
        tool.validateTree(null);
        assertThat(tool.errors().firstErrorOn(TrackingTool.LINK)).isEqualTo("Link should be populated");
        assertThat(tool.errors().firstErrorOn(TrackingTool.REGEX)).isEqualTo("Regex should be populated");
    }

    @Test
    public void shouldValidateLinkProtocol() {
        TrackingTool tool = new TrackingTool("file:///home/user/${ID}", "");
        tool.validate(null);
        assertThat(tool.errors().firstErrorOn(TrackingTool.LINK)).isEqualTo("Link must be a URL starting with https:// or http://");

        tool = new TrackingTool("javascript:alert(${ID})", "");
        tool.validate(null);
        assertThat(tool.errors().firstErrorOn(TrackingTool.LINK)).isEqualTo("Link must be a URL starting with https:// or http://");
    }
}
