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

describe("timer", function () {
    beforeEach(function () {
        setFixtures("<div id=\"project1_profile_box\">\n" +
            "    <div id=\"project1_profile\" class=\"passed\">\n" +
            "        <div style=\"float:right\">\n" +
            "            <a><img src=\"images/wrench.png\" alt=\"Configure project\" title=\"Configure project\" /></a>\n" +
            "            <a><img src=\"images/bin.png\" alt=\"Remove Project\" title=\"Remove Project\" /></a>\n" +
            "        </div>\n" +
            "        <div style=\"float:right;padding-right:2px;\">\n" +
            "            <a id=\"project1_forcebuild\" onclick='ajax_force_build(\"projectName\", \"project1\")'><img src=\"images/lightning.png\" alt=\"Force build\" title=\"Force build\"/></a>\n" +
            "        </div>\n" +
            "        <p style=\"padding:1px;\"><strong><a id=\"project1_build_detail\" href=\"buildDetail.html?filename=log20060704155710Lbuild.489.xml&project=project1\">project1 passed</a>\n" +
            "        <a id=\"project1_build_date\" style=\"text-decoration: none;\">at 12:21 on 9 Dec 2005</a></strong>\n" +
            "        <a><img src=\"images/flag_green.png\" alt=\"Past successful builds\" title=\"Past successful builds\"/></a> |  <a href=\"projectDetail.html?project=project1\" title=\"All Builds of Project project1\"><img src=\"images/bricks.png\"/></a></p>\n" +
            "        <div id=\"project1_timer_area\" style=\"display:none;\">\n" +
            "            <span id=\"project1_time_remaining_label\"></span>\n" +
            "            <span id=\"project1_time_remaining\"></span>\n" +
            "        </div>\n" +
            "    </div>\n" +
            "</div>\n" +
            "<div id=\"timer_area\" class=\"timer_area\"></div>");
    });
    Timer.prototype.get_elapsed_time = function () {
        return  this.elapsed_time;
    }

    PeriodicalExecuter.prototype.getCallBack = function () {
        return  this.callback;
    }
    var old_toggle = Element.toggle;
    beforeEach(function () {
        Element.addMethods({toggle: old_toggle});
        $('project1_time_remaining_label').update("");
        $('project1_time_remaining').update("");
        $('timer_area').show();
    });

    it("test_should_inoke_timer_should_register_elapse_function_as_periodical_executer", function () {
        var timer = new Timer("project1");
        timer.start()
        var executer = timer.getPeriodicalExecuter()
        assertNotUndefined(executer);
        assertTrue(executer.getCallBack().toString().indexOf('.update()') > 0);
    });

    it("test_should_increase_1_each_time_when_invoked", function () {
        var timer = new Timer("project1");
        timer.elapse();
        assertEquals("00:00:01", timer.report_elapsed());
    });

    it("test_should_format_second_to_time_format", function () {
        var timer = new Timer("project1");
        assertEquals("00:00:01", timer.time(1));
        assertEquals("00:01:00", timer.time(60));
        assertEquals("00:01:30", timer.time(90));
        assertEquals("01:01:12", timer.time(3672));
    });

    it("test_timer_should_update_project_profile_box_with_specific_time", function () {
        var timer = new Timer("project1");
        timer.update();
        var content = $('project1_time_remaining').innerHTML
        assertEquals("00:00:01", content);
    });

    it("test_timer_should_update_project_profile_box_with_specific_time_and_elapsed_time", function () {
        var timer = new Timer("project1");
        timer.set_elapsed_time(10);
        timer.update();
        var content = $('project1_time_remaining').innerHTML
        assertEquals("00:00:11", content);
    });

    it("test_should_not_throw_any_exception_when_the_target_html_element_missing", function () {
        var timer = new Timer("project_no_exist");
        try {
            timer.update();
        } catch (err) {
            fail('should not throw exception when element is missing')
        }
    });

    it("test_should_be_able_to_stop_executer", function () {
        var timer = new Timer("project1");
        timer.start();
        assertFalse(timer.is_stopped());
        timer.stop();
        assertTrue(timer.is_stopped());
        assertFalse($('project1_timer_area').visible());
    });

// TODO: ignored for now
    function should_hide_timer_area() {
        var is_invoked = false;
        Element.addMethods({toggle: function () {
            is_invoked = true
        }});
        var timer = new Timer("project1");
        timer.toggle(function (elem) {
            return true
        });
        assertTrue(is_invoked);
    }
});
