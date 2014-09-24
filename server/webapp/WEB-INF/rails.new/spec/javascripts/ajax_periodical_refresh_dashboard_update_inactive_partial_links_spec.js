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

describe("ajax_periodical_refresh_dashboard_update_inactive_partial_links", function () {

    beforeEach(function () {
        contextPath = "dashboard";
        setFixtures("<div id=\"project1_profile_box\">\n" +
                "    <div id=\"project1_profile\">\n" +
                "        <a id=\"project1_forcebuild\" ><img src=\"images/lightning.png\" alt=\"Force build\" title=\"Force build\"/></a>\n" +
                "        <a id=\"project1_config_panel\" onclick=''><img src=\"/dashboard/images/icon-configure-dropdown.gif\" alt=\"Configure project\" title=\"Configure project\" width=\"35\" height=\"20\" /></a>\n" +
                "        <a id=\"project1_all_builds\" href=\"\"><img src=\"$req.getContextPath()/images/icon-view-all-builds.gif\"  alt=\"All builds\" title=\"All builds\"/></a>\n" +
                "        <a id=\"project1_all_successful_builds\" href=\"\"><img src=\"$req.getContextPath()/images/icon-all-successful-builds.gif\" alt=\"All successful builds\" title=\"All successful builds\"/></a>\n" +
                "    </div>\n" +
                "</div>"
        );
        have_called_stop = false;
        $('project1_forcebuild').onclick = "any"
    });

    afterEach(function () {
    });

    it("test_should_NOT_update_links_if_previous_status_is_not_inactive", function () {
        $('project1_profile').className = "passed"
        var json = {
            building_info: { project_name: "project1", building_status: "Passed", onclick_call: "clickcall()", force_build_action_text: "Force Build", latest_build_log_file_name: "log.log", css_class_name: "passed"}
        }
        ajax_periodical_refresh_dashboard_update_inactive_partial_links(json);
        assertTrue($('project1_forcebuild').onclick.toString().indexOf('any') > -1)
    });

    it("test_should_update_links_if_previous_status_is_inactive", function () {
        $('project1_profile').className = "build_profile inactive"
        var json = {
            building_info: { project_name: "project1", building_status: "Building", onclick_call: "clickcall()", force_build_action_text: "Force Build", latest_build_log_file_name: "log.log", css_class_name: "building"}
        }
        ajax_periodical_refresh_dashboard_update_inactive_partial_links(json);
        var isFunctionExist = $('project1_forcebuild').onclick.toString().indexOf('ajax_force_build') > -1;
        assertTrue(isFunctionExist);
        assertEquals("dashboard/project/list/all/project1", $('project1_all_builds').getAttribute('href'));
        assertEquals("dashboard/project/list/passed/project1", $('project1_all_successful_builds').getAttribute('href'));
        isFunctionExist = $('project1_config_panel').onclick.toString().indexOf('display_toolkit') > -1;
        assertTrue(isFunctionExist);

    });
});