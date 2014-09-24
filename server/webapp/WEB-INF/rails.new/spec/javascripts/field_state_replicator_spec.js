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

describe("field_state_replicator", function(){
    var replicator = new FieldStateReplicator();

    beforeEach(function(){
        setFixtures("<div class='under_test'>\n" +
            "    <form name='form_a' id=\"form_a\">\n" +
            "        <input type=\"checkbox\" id=\"form_a_chkbox\"/>\n" +
            "        <input type=\"text\" id=\"form_a_textfield\"/>\n" +
            "    </form>\n" +
            "    <form name='form_b' id=\"form_b\">\n" +
            "        <input type=\"checkbox\" id=\"form_b_chkbox\"/>\n" +
            "        <input type=\"text\" id=\"form_b_textfield\"/>\n" +
            "    </form>\n" +
            "</div>");
    });

    beforeEach(function() {
        $('form_a_textfield').value = "";
        $('form_a_chkbox').checked = false;
        $('form_b_textfield').value = "foo bar";
        $('form_b_chkbox').checked = false;
        replicator.register($('form_a_chkbox'), 'chkbox');
        replicator.register($('form_a_textfield'), 'text');
    });

    afterEach(function() {
        replicator.unregister($('form_a_chkbox'), 'chkbox');
        replicator.unregister($('form_b_chkbox'), 'chkbox');
        replicator.unregister($('form_a_textfield'), 'text');
        replicator.unregister($('form_b_textfield'), 'text');
    });

    it("test_replicates_checkbox_value_after_registration", function() {
        assertFalse("Form b checkbox must start out unchecked", $('form_b_chkbox').checked);
        replicator.register($('form_b_chkbox'), 'chkbox');
        var chkbox_a = $('form_a_chkbox');
        chkbox_a.checked = true;
        fire_event(chkbox_a, 'change');
        assertTrue("Form a checkbox state change must stick", $('form_a_chkbox').checked);
        assertTrue("Form b checkbox state must reflect that of form a", $('form_b_chkbox').checked);
        var chkbox_b = $('form_b_chkbox');
        chkbox_b.checked = false;
        fire_event(chkbox_b, 'change');
        assertFalse("Form b checkbox state must stick", $('form_b_chkbox').checked);
        assertFalse("Form a checkbox state must reflect that of form b", $('form_a_chkbox').checked);
    });

    it("test_replicates_text_field_value_after_registration", function() {
        assertEquals("Form b field must start out with given value", "foo bar", $('form_b_textfield').value);
        replicator.register($('form_b_textfield'), 'text');
        $('form_a_textfield').value = "baz quux";
        fire_event($('form_a_textfield'), 'change');
        assertEquals("Form a textfield state must stick", "baz quux", $('form_a_textfield').value);
        assertEquals("Form b textfield state must reflect that of form a", "baz quux", $('form_b_textfield').value);
        $('form_b_textfield').value = "bar baz";
        fire_event($('form_b_textfield'), 'change');
        assertEquals("Form b textfield state must sick", "bar baz", $('form_b_textfield').value);
        assertEquals("Form a textfield state must reflect that of form b", "bar baz", $('form_a_textfield').value);
    });

    it("test_replicates_checkbox_value_on_registration", function() {
        var chkbox_a = $('form_a_chkbox');
        chkbox_a.checked = true;
        assertTrue("Form a checkbox must start out checked", $('form_a_chkbox').checked);
        assertFalse("Form b checkbox must start out unchecked", $('form_b_chkbox').checked);
        replicator.register($('form_b_chkbox'), 'chkbox');
        assertTrue("Form a checkbox state change must stick", $('form_a_chkbox').checked);
        assertTrue("Form b checkbox state must reflect that of form a", $('form_b_chkbox').checked);
    });

    it("test_replicates_text_field_value_on_registration", function() {
        $('form_a_textfield').value = "baz quux";
        assertEquals("Form a textfield must start out with given value", "baz quux", $('form_a_textfield').value);
        assertEquals("Form b field must start out with given default", "foo bar", $('form_b_textfield').value);
        replicator.register($('form_b_textfield'), 'text');
        assertEquals("Form a textfield state must stick", "baz quux", $('form_a_textfield').value);
        assertEquals("Form b textfield state must reflect that of form a", "baz quux", $('form_b_textfield').value);
    });

    it("test_stops_replicating_checkbox_value_after_unregistration", function() {
        replicator.register($('form_b_chkbox'), 'chkbox');
        replicator.unregister($('form_b_chkbox'), 'chkbox');
        var chkbox_a = $('form_a_chkbox');
        chkbox_a.checked = true;
        fire_event(chkbox_a, 'change');
        assertTrue("Form a checkbox state must stick", $('form_a_chkbox').checked);
        assertFalse("Form b checkbox state must remain unaltered", $('form_b_chkbox').checked);
        chkbox_a.checked = false;
        var chkbox_b = $('form_b_chkbox');
        chkbox_b.checked = true;
        fire_event(chkbox_b, 'change');
        assertFalse("Form a checkbox state must remain unaleterd", $('form_a_chkbox').checked);
        assertTrue("Form b checkbox state must stick", $('form_b_chkbox').checked);
    });

    it("test_stops_replicating_text_field_value_after_registration", function() {
        replicator.register($('form_b_textfield'), 'text');
        replicator.unregister($('form_b_textfield'), 'text');
        $('form_b_textfield').value = "foo bar";
        var text_a = $('form_a_textfield');
        text_a.value = "hello world";
        fire_event(text_a, 'change');
        assertEquals("Form a field state must stick", "hello world", $('form_a_textfield').value);
        assertEquals("Form b field state must remain unaltered", "foo bar", $('form_b_textfield').value);
        var text_b = $('form_b_textfield');
        text_b.value = "bar baz";
        fire_event(text_b, 'change');
        assertEquals("Form a field state must remain unaleterd", "hello world", $('form_a_textfield').value);
        assertEquals("Form b field state must stick", "bar baz", $('form_b_textfield').value);
    });
});
