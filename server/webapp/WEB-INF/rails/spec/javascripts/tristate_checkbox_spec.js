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

describe("tristate_checkbox", function () {
    beforeEach(function () {
        setFixtures("<div class='under_test'>\n" +
            "    <form name='tri_state_form' id=\"form\">\n" +
            "        <span id='check_box_view'></span>\n" +
            "        <select id=\"tristate\">\n" +
            "            <option value=\"state-1\">state-1</option>\n" +
            "            <option value=\"state-2\">state-2</option>\n" +
            "            <option value=\"state-3\">state-3</option>\n" +
            "        </select>\n" +
            "    </form>\n" +
            "</div>");
    });
    var panel_looked_up_with = null;
    var panel = null;
    var originalMicrocontentPopup = MicroContentPopup;

    beforeEach(function () {
        panel = null;
        MicroContentPopup = {
            lookupHandler: function (child_dom) {
                panel_looked_up_with = child_dom;
                return panel;
            }
        };
        tristate_view = $('check_box_view');
        tristate = $('tristate');
        checkbox = new TriStateCheckbox(tristate_view, tristate, true);
    });

    function tearDown(shouldReassignOriginals) {
        tristate.options[0].selected = true;
        tristate_view.stopObserving();
        delete checkbox;
        if(shouldReassignOriginals) MicroContentPopup = originalMicrocontentPopup;
    }


    afterEach(function () {
        tearDown(true);
    });

    function assertState(state) {
        assertEquals(state, tristate.value);
        assertTrue("Should have " + state + " but was [" + tristate_view.className + "]", tristate_view.hasClassName(state));
        assertTrue("Should have class tristate but was [" + tristate_view.className + "]", tristate_view.hasClassName('tristate'));
    }

    it("test_initial_default_checkbox_state_is_unmodified", function () {
        assertState('state-1');
        assertTrue(tristate.hasClassName('hidden'));
    });

    it("test_clicking_tristate_toggles_value", function () {
        assertState('state-1');
        fire_event(tristate_view, 'mousedown');
        assertState('state-2');
    });

    it("test_clicking_tristate_does_not_toggle_value_when_disabled", function () {
        tearDown(false);
        checkbox = new TriStateCheckbox(tristate_view, tristate, false);
        assertState('state-1');
        fire_event(tristate_view, 'mousedown');
        assertState('state-1');
        fire_event(tristate_view, 'mousedown');
        assertState('state-1');
    });

    it("test_clicking_tristate_multiple_times_rotates", function () {
        fire_event(tristate_view, 'mousedown');
        assertState('state-2');
        fire_event(tristate_view, 'mousedown');
        assertState('state-3');
        fire_event(tristate_view, 'mousedown');
        assertState('state-1');
    });

    it("test_mousedown_event_is_not_bubbled_up_to_avoid_selecting_text", function () {
        var parent_element = $$('.under_test')[0];
        var parent_element_received_event = false;
        Event.observe(parent_element, 'mousedown', function () {
            parent_element_received_event = true;
        });
        fire_event(tristate_view, 'mousedown');
        assertFalse("parent element should not be notified of the mousedown event", parent_element_received_event);
    });

    it("test_should_call_tristate_clicked_on_resource_selector_when_used", function () {
        var called = false;
        panel = {
            tristate_clicked: function () {
                called = true;
            }
        };

        tearDown(false);
        assertState('state-1');
        checkbox = new TriStateCheckbox(tristate_view, tristate, true);
        fire_event(tristate_view, 'mousedown');
        assertState('state-2');
        assertTrue("modify_mode was called", called);
        assertEquals("view dom element should be passed into AgentEditPopup while looking up corresponding widget", tristate_view, panel_looked_up_with);
    });

    it("test_should_ignore_if_its_contained_in_a_panel_that_does_not_listen_to_tristate_clicked", function () {
        panel = {
            tristate_clicked: "not a function"
        };

        tearDown(false);
        assertState('state-1');
        checkbox = new TriStateCheckbox(tristate_view, tristate, true);
        fire_event(tristate_view, 'mousedown');
        assertState('state-2');
    });
});
