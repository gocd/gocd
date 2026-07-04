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
package com.thoughtworks.go.domain;

import org.apache.commons.text.StringEscapeUtils;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class DefaultCommentRendererTest {

    private DefaultCommentRenderer renderer;

    @Test
    public void shouldReturnOriginalStringIfNothingSpecified() {
        renderer = new DefaultCommentRenderer("", "");
        String toRender = "some string";
        String result = renderer.render(toRender);
        assertThat(result).isEqualTo(toRender);
    }

    @Test
    public void shouldReturnEmptyStringIfNothingToRender() {
        String link = "http://mingle05/projects/cce/cards/${ID}";
        String regex = "(evo-\\d+)";
        renderer = new DefaultCommentRenderer(link, regex);
        String result = renderer.render(null);
        assertThat(result).isEmpty();
    }

    @Test
    public void shouldRenderStringWithoutSpecifiedRegexAndLinkIfHasGroupsAndNoneMaterialize() {
        String link = "http://mingle05/projects/cce/cards/${ID}";
        String regex = "evo-(\\d+)|evo-";
        renderer = new DefaultCommentRenderer(link, regex);

        String result = renderer.render("evo-abc: checkin message");
        assertThat(result).isEqualTo("evo-abc: checkin message");
    }

    @Test
    public void shouldRenderStringWithSpecifiedRegexAndLinkIfHasGroupsAndOtherThanFirstMaterializes() {
        String link = "http://mingle05/projects/cce/cards/${ID}";
        String regex = "evo-(\\d+)|evo-(ab)";
        renderer = new DefaultCommentRenderer(link, regex);

        String result = renderer.render("evo-abc: checkin message");
        assertThat(result).isEqualTo("""
            <a href="http://mingle05/projects/cce/cards/ab" target="story_tracker">evo-ab</a>c: checkin message""");
    }

    @Test
    public void shouldRenderStringWithSpecifiedRegexAndLink() {
        String link = "http://mingle05/projects/cce/cards/${ID}";
        String regex = "(evo-\\d+)";
        renderer = new DefaultCommentRenderer(link, regex);

        String result = renderer.render("evo-111: checkin message");
        assertThat(result).isEqualTo("""
            <a href="http://mingle05/projects/cce/cards/evo-111" target="story_tracker">evo-111</a>: checkin message""");
    }

    @Test
    public void shouldRenderStringWithSpecifiedRegexAndLink1() {
        String link = "http://mingle05/projects/cce/cards/${ID}";
        String regex = "(?:Task |#|Bug )(\\d+)";
        renderer = new DefaultCommentRenderer(link, regex);

        assertThat(renderer.render("Task 111: checkin message")).isEqualTo("""
            <a href="http://mingle05/projects/cce/cards/111" target="story_tracker">Task 111</a>: checkin message""");
        assertThat(renderer.render("Bug 111: checkin message")).isEqualTo("""
            <a href="http://mingle05/projects/cce/cards/111" target="story_tracker">Bug 111</a>: checkin message""");
        assertThat(renderer.render("#111: checkin message")).isEqualTo("""
            <a href="http://mingle05/projects/cce/cards/111" target="story_tracker">#111</a>: checkin message""");
    }

    @Test
    public void shouldRenderStringWithRegexThatHasSubSelect() {
        String link = "http://mingle05/projects/cce/cards/${ID}";
        String regex = "evo-(\\d+)";
        renderer = new DefaultCommentRenderer(link, regex);

        String result = renderer.render("evo-111: checkin message");
        assertThat(result).isEqualTo("""
            <a href="http://mingle05/projects/cce/cards/111" target="story_tracker">evo-111</a>: checkin message""");
    }

    @Test
    public void shouldReturnMatchedStringIfRegexDoesNotHaveGroup() {
        String link = "http://mingle05/projects/cce/cards/${ID}";
        String regex = "\\d+";
        renderer = new DefaultCommentRenderer(link, regex);

        String result = renderer.render("evo-1020: checkin message");
        assertThat(result).isEqualTo("""
            evo-<a href="http://mingle05/projects/cce/cards/1020" target="story_tracker">1020</a>: checkin message""");
    }

    @Test
    public void shouldReturnMatchedStringFromFirstGroupIfMultipleGroupsAreDefined() {
        String link = "http://mingle05/projects/cce/cards/${ID}";
        String regex = "(\\d+)-(evo\\d+)";
        renderer = new DefaultCommentRenderer(link, regex);

        String result = renderer.render("1020-evo1: checkin message");
        assertThat(result).isEqualTo("""
            <a href="http://mingle05/projects/cce/cards/1020" target="story_tracker">1020-evo1</a>: checkin message""");
    }

    @Test
    public void shouldReturnOriginalStringIfRegexDoesNotMatch() {
        String link = "http://mingle05/projects/cce/cards/${ID}";
        String regex = "evo-(\\d+)";
        renderer = new DefaultCommentRenderer(link, regex);
        String toRender = "evo-abc: checkin message";

        String result = renderer.render(toRender);
        assertThat(result).isEqualTo(toRender);
    }

    @Test
    public void shouldReturnOriginalStringIfRegexIsIllegal() {
        String link = "http://mingle05/projects/cce/cards/${ID}";
        String regex = "++";
        renderer = new DefaultCommentRenderer(link, regex);
        String toRender = "evo-abc: checkin message";

        String result = renderer.render(toRender);
        assertThat(result).isEqualTo(toRender);
    }

    @Test
    public void shouldRenderUsingFixedUrlIfLinkDoesNotContainVariable() {
        String link = "http://mingle05/projects/cce/cards/wall-E";
        String regex = "(evo-\\d+)";
        renderer = new DefaultCommentRenderer(link, regex);

        String result = renderer.render("evo-111: checkin message");
        assertThat(result).isEqualTo("""
            <a href="http://mingle05/projects/cce/cards/wall-E" target="story_tracker">evo-111</a>: checkin message""");
    }

    @Test
    public void shouldUseLinkFromConfigurationRegardlessOfItsValidity() {
        String link = "aaa${ID}";
        String regex = "\\d+";
        renderer = new DefaultCommentRenderer(link, regex);

        String result = renderer.render("111: checkin message");
        assertThat(result).isEqualTo("""
            <a href="aaa111" target="story_tracker">111</a>: checkin message""");
    }

    @Test
    // #2324
    public void shouldRenderAllPossibleMatches() {
        String link = "http://mingle05/projects/cce/cards/${ID}";
        String regex = "#(\\d+)";
        renderer = new DefaultCommentRenderer(link, regex);

        String result = renderer.render("#111, #222: checkin message; #333: another message");
        assertThat(result).isEqualTo("""
            <a href="http://mingle05/projects/cce/cards/111" target="story_tracker">#111</a>, \
            <a href="http://mingle05/projects/cce/cards/222" target="story_tracker">#222</a>: checkin message; \
            <a href="http://mingle05/projects/cce/cards/333" target="story_tracker">#333</a>: another message""");
    }

    @Test
    public void shouldReplaceBasedOnRegexInsteadOfPureStringReplacement() {
        String link = "http://mingle05/projects/cce/cards/${ID}";
        String regex = "evo-(\\d+)";
        renderer = new DefaultCommentRenderer(link, regex);

        String result = renderer.render("Replace evo-1994.  Don't replace 1994");
        assertThat(result).contains("""
            <a href="http://mingle05/projects/cce/cards/1994" target="story_tracker">evo-1994</a>""");
        assertThat(result).contains("Don't replace 1994");
    }

    @Test
    public void shouldSupportUTF8() {
        String link = "http://mingle05/projects/cce/cards/${ID}";
        String regex = "#(\\d+)";
        renderer = new DefaultCommentRenderer(link, regex);

        String result = renderer.render("The story #111 is fixed by 德里克. #122 is also related to this");
        assertThat(result).isEqualTo("The story %s is fixed by %s. %s is also related to this"
            .formatted("""
                    <a href="http://mingle05/projects/cce/cards/111" target="story_tracker">#111</a>""",
                StringEscapeUtils.escapeHtml4("德里克"), """
                    <a href="http://mingle05/projects/cce/cards/122" target="story_tracker">#122</a>"""
            ));
    }

    @Test
    public void shouldEscapeTheWholeCommentIfNoneIsMatched() {
        renderer = new DefaultCommentRenderer("", "");
        String toRender = "some <string>";
        String result = renderer.render(toRender);
        assertThat(result).isEqualTo(StringEscapeUtils.escapeHtml4(toRender));
    }

    @Test
    public void shouldEscapeDynamicLink() {
        String link = "http://jira.example.com/${ID}?hello&gocd=true";
        String regex = "^ABC-[^ ]+";
        renderer = new DefaultCommentRenderer(link, regex);

        String result = renderer.render("ABC-\"><svg/onload=\"alert(1)");
        assertThat(result).isEqualTo("""
            <a href="http://jira.example.com/ABC-%22%3E%3Csvg%2Fonload%3D%22alert%281%29?hello&amp;gocd=true" target="story_tracker">\
            ABC-&quot;&gt;&lt;svg/onload=&quot;alert(1)\
            </a>""");
    }

    @Test
    public void shouldEscapeDynamicLinkWithIdGroup() {
        String link = "http://jira.example.com/${ID}?hello&gocd=true";
        String regex = "^ABC-([^ ]+)";
        renderer = new DefaultCommentRenderer(link, regex);

        String result = renderer.render("ABC-\"><svg/onload=\"alert(1)");
        assertThat(result).isEqualTo("""
            <a href="http://jira.example.com/%22%3E%3Csvg%2Fonload%3D%22alert%281%29?hello&amp;gocd=true" target="story_tracker">\
            ABC-&quot;&gt;&lt;svg/onload=&quot;alert(1)\
            </a>""");
    }

    @Test
    public void shouldUriEncodeIdentifiers() {
        String link = "http://website.com/${ID}";
        String regex = "ABC-[^ ]+";
        renderer = new DefaultCommentRenderer(link, regex);

        String result = renderer.render("ABC-1/2德");
        assertThat(result).isEqualTo("""
            <a href="http://website.com/ABC-1%2F2%E5%BE%B7" target="story_tracker">\
            ABC-1/2德\
            </a>""");
    }

    @Test
    public void shouldUriEncodeIdentifiersWithIdGroup() {
        String link = "http://website.com/${ID}";
        String regex = "ABC-([^ ]+)";
        renderer = new DefaultCommentRenderer(link, regex);

        String result = renderer.render("ABC-1/2德");
        assertThat(result).isEqualTo("""
            <a href="http://website.com/1%2F2%E5%BE%B7" target="story_tracker">\
            ABC-1/2德\
            </a>""");
    }

    @Test
    public void shouldUriEncodeIdentifiersIntoQuery() {
        String link = "http://website.com/?id=${ID}";
        String regex = "ABC-[^ ]+";
        renderer = new DefaultCommentRenderer(link, regex);

        String result = renderer.render("ABC-1?=2");
        assertThat(result).isEqualTo("""
            <a href="http://website.com/?id=ABC-1%3F%3D2" target="story_tracker">\
            ABC-1?=2\
            </a>""");
    }

    @Test
    public void shouldUriEncodeIdentifiersWithIdGroupIntoQuery() {
        String link = "http://website.com/?id=${ID}";
        String regex = "ABC-([^ ]+)";
        renderer = new DefaultCommentRenderer(link, regex);

        String result = renderer.render("ABC-1?=2");
        assertThat(result).isEqualTo("""
            <a href="http://website.com/?id=1%3F%3D2" target="story_tracker">\
            ABC-1?=2\
            </a>""");
    }

    @Test
    public void doesNotDoubleUriEncodeLink() {
        String link = "http://website.com/${ID}?encoded=%2F%22";
        String regex = "ABC-\\d+";
        renderer = new DefaultCommentRenderer(link, regex);

        String result = renderer.render("ABC-123");
        assertThat(result).isEqualTo("""
            <a href="http://website.com/ABC-123?encoded=%2F%22" target="story_tracker">\
            ABC-123\
            </a>""");
    }

    @Test
    public void doesNotDoubleUriEncodeLinkWithIdGroup() {
        String link = "http://website.com/${ID}?encoded=%2F%22";
        String regex = "ABC-(\\d+)";
        renderer = new DefaultCommentRenderer(link, regex);

        String result = renderer.render("ABC-123");
        assertThat(result).isEqualTo("""
            <a href="http://website.com/123?encoded=%2F%22" target="story_tracker">\
            ABC-123\
            </a>""");
    }
}
