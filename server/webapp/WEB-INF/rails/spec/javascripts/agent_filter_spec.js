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

describe("agent_filter", function () {
    var agentFilter;
    var orignialAjax;
    var url = "http://foo.bar/go/autocomplete";
    var urls;

    afterEach(function () {
        jQuery.ajax = orignialAjax;
    });

    beforeEach(function () {
        setFixtures("<div class='under_test'>\n" +
                "    <div id=\"dd_ajax_float\"></div>\n" +
                "    <div id=\"filter_help\" class=\"enhanced_dropdown hidden\"></div>\n" +
                "    <input type=\"text\" id=\"filter_text\" value=\"\"/>\n" +
                "    <input type=\"button\" id=\"perform_filter\" name=\"Perform Filter\"/>\n" +
                "</div>"
        )
        urls = { "ip": url + "/ip", "resource": url + "/resource", "os": url + "/os" };
        agentFilter = new AgentFilter(jQuery("#filter_text").get(0), jQuery("#filter_help").get(0), jQuery('#dd_ajax_float').get(0), urls);
        orignialAjax = jQuery.ajax;
        jQuery("#filter_text").val("");
    })

    it("testShouldCloseThePopupWhenThereIsTextEnteredInTheTextBox", function () {
        agentFilter.createHelp();
        fire_event($('filter_text'), 'click');
        assertFalse("The popup should be open now", jQuery("#filter_help").get(0).hasClassName("hidden"));
        jQuery("#filter_text").val("performance");
        fire_event($('filter_text'), 'keydown');
        assertTrue("The popup should be closed now since the text box got some text", jQuery("#filter_help").get(0).hasClassName("hidden"));
    });

    it("testShouldNotIssueAnAjaxRequestWhenAColonIsNotEntered", function () {
        var wasCalled = false;
        jQuery.ajax = function () {
            wasCalled = true;
        };
        agentFilter.hookupAutocomplete();
        var filterTextJQuer = jQuery("#filter_text");
        filterTextJQuer.val("resource");
        filterTextJQuer.search();
        assertFalse(wasCalled);
    });

    it("testShouldNotIssueAnAjaxRequestWhenTheKeyWordIsNotKnown", function () {
        var wasCalled = false;
        jQuery.ajax = function () {
            wasCalled = true;
        };
        agentFilter.hookupAutocomplete();
        var filterTextJQuer = jQuery("#filter_text");
        filterTextJQuer.val("hello:");
        filterTextJQuer.search();
        assertFalse(wasCalled);

    });

    it("testShouldIssueAnAjaxRequestWhenAColonIsEntered", function () {
        var wasCalled = false;
        var actualUrl;
        jQuery.ajax = function (options) {
            wasCalled = true;
            actualUrl = options.url;
        };
        agentFilter.hookupAutocomplete();
        var filterTextJQuer = jQuery("#filter_text");
        filterTextJQuer.val("ip:");
        filterTextJQuer.search();
        assertTrue("An ajax call should have been issued.", wasCalled);
        assertEquals("The url that got issued is not the same as the expected.", url + "/ip", actualUrl);

    });

    it("testShouldUseTheEnteredKeyInTheUrl", function () {
        var actualUrl;
        jQuery.ajax = function (options) {
            actualUrl = options.url;
        };
        agentFilter.hookupAutocomplete();
        var filterTextJQuer = jQuery("#filter_text");
        filterTextJQuer.val("os:");
        filterTextJQuer.search();
        assertEquals("The url that got issued is not the same as the expected.", url + "/os", actualUrl);
    });

    it("testShouldSendTheTextFollowingTheKeywordAsTheQueryString", function () {
        var actualUrl;
        var data;
        jQuery.ajax = function (options) {
            actualUrl = options.url;
            data = options.data;
        };
        agentFilter.hookupAutocomplete();
        var filterTextJQuer = jQuery("#filter_text");
        filterTextJQuer.val("os: wi");
        filterTextJQuer.search();
        assertEquals("The data obtained from the ajax call is wrong", "wi", data["q"]);
    });
});



