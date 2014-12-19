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
 *************************GO-LICENSE-END**********************************/

describe("toggle_css_class", function () {
    beforeEach(function () {
        setFixtures("<div class='under_test'>\n" +
            "    <div id=\"parent_div\">\n" +
            "        <div>\n" +
            "            <a id=\"clickable\">CLickable</a>\n" +
            "        </div>\n" +
            "    </div>\n" +
            "\n" +
            "    <div id=\"pare.nt.div1\" class=\"hidereveal_collapsed\">\n" +
            "        <span class=\"hidereveal_expander\">expander</span>\n" +
            "        <div class=\"hidereveal_content\">contents here</div>\n" +
            "    </div>\n" +
            "</div>");
    });
    var clickable;
    var container;
    var originalmarkup;
    beforeEach(function () {
        originalmarkup = $$(".under_test")[0].innerHTML;
        container = $("pare.nt.div1");
        make_collapsable("pare.nt.div1");
        clickable = $$(".hidereveal_expander")[0]
    });

    afterEach(function () {
        $$(".under_test")[0].innerHTML = originalmarkup;
    });

    it("test_make_collapsible", function () {
        assertTrue("should not be expanded", container.hasClassName('hidereveal_collapsed'));
        fire_event($$(".hidereveal_expander")[0], 'click');
        assertFalse("should be expanded", container.hasClassName('hidereveal_collapsed'));
    });

    it("test_prevents_click_from_bubbling", function () {
        var click_bubbled = false;
        Event.observe($('pare.nt.div1'), 'click', function () {
            click_bubbled = true;
        });
        fire_event($$(".hidereveal_expander")[0], 'click');
        assertFalse("parent must not hear click as it can lead to text selection if done fast", click_bubbled);
    });
});