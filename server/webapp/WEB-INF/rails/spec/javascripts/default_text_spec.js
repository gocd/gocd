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

describe("default_text", function(){
    beforeEach(function(){
        setFixtures("<div class='under_test'>\n" +
            "    <form name='tri_state_form' id=\"form\">\n" +
            "        <input type=\"text\" id='default_text' name=\"text_field\"/>\n" +
            "        <input type=\"submit\" id=\"submit_button\"/>\n" +
            "    </form>\n" +
            "</div>");
    });

    beforeEach(function(){
        default_text = $('default_text');
        default_text.value = "";
        default_text_model = new DefaultText(default_text, "my default text");
    });

    afterEach(function(){
        Event.stopObserving(default_text);
    });

    it("test_should_set_default_text_as_value", function(){
        assertEquals("default text should be present on the textbox", "my default text", default_text.value);
        assertTrue("must have default css class when default text is shown", default_text.hasClassName('shadedDefaultValue'));
    });

    it("test_should_clear_default_text_on_focus", function(){
        fire_event(default_text, 'focus');
        assertEquals("default text should be cleared on focus", "", default_text.value);
        assertFalse("must not have default css class when default text is shown", default_text.hasClassName('shadedDefaultValue'));
    });

    it("test_should_bring_back_default_text_on_blur", function(){
        fire_event(default_text, 'focus');
        fire_event(default_text, 'blur');
        assertEquals("default text should be brought back on blur", "my default text", default_text.value);
        assertTrue("must have default css class when default text is shown", default_text.hasClassName('shadedDefaultValue'));
    });

    it("test_should_not_bring_back_default_text_on_blur_when_has_text", function(){
        fire_event(default_text, 'focus');
        default_text.value = "type something in";
        fire_event(default_text, 'blur');
        assertEquals("user's text should be retained on blur", "type something in", default_text.value);
        assertFalse("must not have default css class when default text is shown", default_text.hasClassName('shadedDefaultValue'));
    });

    it("test_should_not_remove_user_entered_text_when_focusing", function(){
        fire_event(default_text, 'focus');
        default_text.value = "type something in";
        fire_event(default_text, 'blur');
        fire_event(default_text, 'focus');
        assertEquals("user's text should be retained on focus", "type something in", default_text.value);
        assertFalse("must not have default css class when default text is shown", default_text.hasClassName('shadedDefaultValue'));
    });

    it("test_should_not_remove_user_entered_text_even_when_same_as_default_text", function(){
        fire_event(default_text, 'focus');
        default_text.value = "my default text";
        fire_event(default_text, 'keyup');
        fire_event(default_text, 'blur');
        fire_event(default_text, 'focus');
        assertEquals("user's text should be retained on focus", "my default text", default_text.value);
        assertFalse("must not have default css class when default text is shown", default_text.hasClassName('shadedDefaultValue'));
    });

    it("test_should_clear_itslef_before_form_submission_if_registered", function(){
        assertEquals("my default text", default_text.value);
        default_text_model.clear();
        assertEquals("", default_text.value);
    });
});
