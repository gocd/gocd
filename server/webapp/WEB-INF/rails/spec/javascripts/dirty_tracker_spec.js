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

describe("dirty_tracker", function () {
    beforeEach(function () {
        setFixtures("<div class='under_test'>\n" +
            "    <div class=\"change_materials\">\n" +
            "        <div class=\"sub_tabs_container\">\n" +
            "            <ul>\n" +
            "                <li id=\"materials_tab\" class=\"current_tab\"><a>Materials</a></li>\n" +
            "                <li id=\"environment_variables_tab\" ><a>Environment Variables</a></li>\n" +
            "            </ul>\n" +
            "            <div class=\"clear\"></div>\n" +
            "        </div>\n" +
            "        <div class=\"sub_tab_container rounded-corner-for-tab-container\">\n" +
            "        </div>\n" +
            "    </div>\n" +
            "</div>");
    });

    afterEach(function () {
    });

    it("test_should_update_when_revision_is_updated", function () {
        var dirtyTracker = new DirtyTracker('materials_tab');
        dirtyTracker.update(1);
        assertEquals('Materials', $$('#materials_tab.updated')[0].down('a').innerHTML);
    });

    it("test_should_remove_updated_when_update_is_undone", function () {
        var dirtyTracker = new DirtyTracker('materials_tab');
        dirtyTracker.update(1);
        dirtyTracker.update(1);
        dirtyTracker.undoUpdate(1);
        assertTrue($$('#materials_tab.updated').length == 0);
    });

    it("test_should_not_remove_updated_when_atleast_one_is_updated", function () {
        var dirtyTracker = new DirtyTracker('materials_tab');
        dirtyTracker.update(1);
        dirtyTracker.update(2);
        dirtyTracker.undoUpdate(1);
        assertTrue($$('#materials_tab.updated').length != 0);
    });
});
