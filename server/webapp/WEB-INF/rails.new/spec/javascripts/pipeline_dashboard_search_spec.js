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

describe("pipeline_dashboard_search", function () {

    var originalDeBounceHandler = deBounceHandler;
    beforeEach(function () {
        setFixtures("<div class='under_test'>\n" +
            "    <input  placeholder='Search for Pipeline' id='pipeline-search' type=\"text\"/>\n" +
            "\n" +
            "    <div class=\"pipeline\">\n" +
            "        <h3><a href=\"#\">Pipeline1</a></h3>\n" +
            "    </div>\n" +
            "    <div class=\"pipeline\">\n" +
            "        <h3><a href=\"#\">Pipeline1-2</a></h3>\n" +
            "    </div>\n" +
            "    <div class=\"pipeline\">\n" +
            "        <h3><a href=\"#\">Pipeline2</a></h3>\n" +
            "    </div>\n" +
            "    <div class=\"pipeline\">\n" +
            "        <h3><a href=\"#\">Pipeline2-2</a></h3>\n" +
            "    </div>\n" +
            "</div>");

        deBounceHandler = {
            deBounceSearch: function (callback) {
                return callback;
            }
        };

        PipelineSearch.initialize('#pipeline-search');
    });

    afterEach(function () {
        jQuery(".should-delete-later").remove();
        deBounceHandler = originalDeBounceHandler;
    });

    xit("test_should_search_the_pipeline_with_text_entered_in_textbox", function () {
        searchFor('pipeline1');

        assertVisiblePipelines("pipeline1", 2);
        assertHighlightedPipelines(2);

        var countMessage = jQuery('.count');
        assertTrue(countMessage[0].visible());
        assertEquals('Pipeline counts in found message is incorrect', 'Total 2 matches found.', countMessage.text().trim());
    });


    it("test_should_get_all_hidden_pipeline_back_if_we_click_on_crossbutton", function () {
        var searchBox = jQuery('#pipeline-search');
        var clearButton = jQuery('.clear');
        searchFor('pipeline1');

        clearButton.click();

        assertEquals('Search text-box should be blank', '', searchBox.val());
        assertVisiblePipelines("", 4);
        assertHighlightedPipelines(0);
    });


    it("test_should_clear_the_textbox_if_we_press_esckey", function () {
        var searchBox = jQuery('#pipeline-search');
        searchFor('pipeline1');

        var esc = jQuery.Event("keyup", { keyCode: 27 });
        searchBox.trigger(esc);

        assertEquals('Search text-box should be blank', '', searchBox.val());
        assertVisiblePipelines("", 4);
        assertHighlightedPipelines(0);
    });


    xit("test_should_force_search_to_run_again_when_dashboard_refresh_event_is_received_with_modifications", function () {
        searchFor('pipeline1');
        assertHighlightedPipelines(2);
        assertVisiblePipelines("pipeline1", 2);

        jQuery(".under_test").append("<div class='pipeline should-delete-later' id='newly-added-pipeline'><h3><a href='#'>Pipeline1-3</a></h3></div>");
        jQuery(document).trigger("dashboard-refresh-completed", false);

        assertHighlightedPipelines(3);
        assertVisiblePipelines("pipeline1", 3);
    });


    xit("test_should_not_force_search_to_run_again_when_dashboard_refresh_event_is_received_with_no_modifications", function () {
        searchFor('pipeline1');
        assertVisiblePipelines("pipeline1", 2);

        jQuery(".under_test").append("<div class='pipeline should-delete-later' id='newly-added-pipeline'><h3><a href='#'>Pipeline1-3</a></h3></div>");
        jQuery(document).trigger("dashboard-refresh-completed", true);

        assertHighlightedPipelines(2);
    });


    function assertVisiblePipelines(searchText, expectedNumberOfVisiblePipelines) {
        var allPipelines = jQuery('.pipeline');
        var visiblePipelines = [];
        var invisiblePipelines = [];

        allPipelines.each(function () {
            if (jQuery(this).css('opacity') == 1) {
                visiblePipelines.push(jQuery(this).find("h3 a").text());
            } else {
                invisiblePipelines.push(jQuery(this).find("h3 a").text());
            }
        });

        assertEquals('Expected ' + expectedNumberOfVisiblePipelines + ' visible pipelines.', expectedNumberOfVisiblePipelines, visiblePipelines.length);
        for (var i = 0; i < visiblePipelines.length; i++) {
            if (visiblePipelines[i].toLowerCase().indexOf(searchText.toLowerCase()) === -1) {
                fail("Pipeline should not be visible: " + visiblePipelines[i]);
            }
        }

        for (var i = 0; i < invisiblePipelines.length; i++) {
            if (invisiblePipelines[i].toLowerCase().indexOf(searchText.toLowerCase()) !== -1) {
                fail("Pipeline should be visible: " + invisiblePipelines[i]);
            }
        }
    }

    function assertHighlightedPipelines(expectedNumberOfPipelinesWithHighlighting) {
        assertEquals(expectedNumberOfPipelinesWithHighlighting, jQuery('.pipeline h3 a .highlight').length);
    }

    function searchFor(textToSearchFor) {
        var searchBox = jQuery('#pipeline-search');
        searchBox.val(textToSearchFor);
        searchBox.trigger('keyup');
    }
});
