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

describe("build_detail", function(){

    var originalBuildDetail = BuildDetail;
    beforeEach(function(){
        BuildDetail = originalBuildDetail;
        contextPath = "/go";
        setFixtures("<div id=\"dashboard_download_project1_log123.xml_abc_0\" class=\"subdir-container\" style=\"display:none\"></div>\n" +
            "<div class=\"dir-container\">\n" +
            "    <span id=\"directory\" class=\"directory\"><a id=\"link\" onclick=ajax_tree_navigator(\"url\")>$fileName</a></span>\n" +
            "</div>\n" +
            "<div id=\"dashboard_download_project1_log123.xml_abc\" class=\"subdir-container\" style=\"display:none\"></div>\n" +
            "<div id=\"dashboard_download_project1_log123.xml_abc_1\" class=\"subdir-container\" style=\"display:none\"></div>\n" +
            "\n" +
            "<div id=\"any\">\n" +
            "    <div  class=\"sub_tabs_container\">\n" +
            "    <ul>\n" +
            "        <li id=\"li1\" class=\"current_tab\"><a>Errors</a><a class=\"tab_button_body_match_text\">errors</a></li>\n" +
            "        <li id=\"li2\"><a>Modifications</a><a class=\"tab_button_body_match_text\">modifications</a></li>\n" +
            "        <li id=\"li3\"><a>Build Log</a><a class=\"tab_button_body_match_text\">build-log</a></li>\n" +
            "        <li id=\"li4\"><a>Tests</a><a class=\"tab_button_body_match_text\">tests</a></li>\n" +
            "    </ul>\n" +
            "    </div>\n" +
            "</div>\n" +
            "<div id=\"tab-content-of-errors\">\n" +
            "</div>\n" +
            "<div id=\"tab-content-of-modifications\" style=\"display:none\">\n" +
            "</div>\n" +
            "<div id=\"collapsible\" class=\"title_message_collapsed\"></div>\n" +
            "<div class=\"widget\" id=\"build-output-console-warnning\" style=\"display: none;\">No build output.</div>");

        window.tabsManager = new TabsManager();
    });

    afterEach(function(){
        BuildDetail = originalBuildDetail;
        contextPath = undefined;
        window.tabsManager = undefined;
    });

    it("test_should_expand_the_directory", function(){
        var expectedId = "dashboard_download_project1_log123.xml_abc";
        prepareMockRequest({status: 200}, '');
        assertFalse($(expectedId).visible())
        assertTrue('need class name directory', $('directory').hasClassName('directory'))
        BuildDetail.tree_navigator($('link'), "dashboard/download/project1/log123.xml/abc")
        assertTrue('div should be visible', $(expectedId).visible())
        assertTrue('class name should be opened_directory', $('directory').hasClassName('opened_directory'))
        BuildDetail.tree_navigator($('link'), "dashboard/download/project1/log123.xml/abc")
        assertFalse($(expectedId).visible())
        assertTrue($('directory').hasClassName('directory'))
    });

    it("test_should_click_current_tab_element_should_not_move_current_tab", function(){
        $('li1').open();
        assertTrue($('li1').hasClassName('current_tab'));
        assertTrue($('tab-content-of-errors').visible())
        $('li1').open();
        assertTrue($('li1').hasClassName('current_tab'));
        assertTrue($('tab-content-of-errors').visible())
    });

    it("test_should_click_another_element_should_move_current_tab", function(){
        assertTrue("'li1' should have className 'current_tab'", $('li1').hasClassName('current_tab'));
        assertTrue("'tab-content-of-errors' should be visible", $('tab-content-of-errors').visible())
        assertFalse("'tab-content-of-modifications' should not be visible", $('tab-content-of-modifications').visible())
        $('li2').open();
        assertTrue("'li2' should have className 'current_tab'", $('li2').hasClassName('current_tab'));
        assertFalse("'li1' should not have className 'current_tab'", $('li1').hasClassName('current_tab'));
        assertTrue("'modifications' should be visible", $('tab-content-of-modifications').visible())
        assertFalse("'errors' should not be visible", $('tab-content-of-errors').visible())
    });

    it("test_should_return_subcontainer", function(){
        var theContainer = BuildDetail.getSubContainer($("link"));
        assertTrue("should return dashboard_download_project1_log123.xml_abc", theContainer.id == "dashboard_download_project1_log123.xml_abc");
    });

    it("test_should_show_no_output_message_when_server_return_404", function(){
        prepareMockRequest({status: 404}, '');
        $('build-output-console-warnning').hide();
        assertFalse($('build-output-console-warnning').visible());
        var bd = new BuildOutputDetector('pipeline-2', 'stageName', true);
        assertEquals('/go/files/pipeline/pipeline-2/stage/stageName/cruise-output/console.log', bd.url);
        assertTrue($('build-output-console-warnning').visible());
    });

    it("test_should_show_iframe_content_when_server_return_200", function(){
        prepareMockRequest({status: 200}, 'out put file body');
        $('build-output-console-warnning').hide();
        assertFalse($('build-output-console-warnning').visible());
        var bd = new BuildOutputDetector('pipeline-2', 'stageName', true);
        assertEquals('/go/files/pipeline/pipeline-2/stage/stageName/cruise-output/console.log', bd.url);
        assertFalse($('build-output-console-warnning').visible());
    });
});