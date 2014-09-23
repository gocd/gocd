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

describe("stage_history", function () {
    beforeEach(function () {
        AjaxRefreshers.clear();
        setFixtures("<a href=\"#\" id=\"stage_history_3\">3</a>\n" +
            "<input id=\"stage-history-page\" name=\"stage-history-page\">");
    });

    var actual_ajax_updater = Ajax.Updater;
    var updater_url;
    var actual_jquery_ajax = $j.ajax;
    var options;
    var afterRef;

    beforeEach(function () {
        Ajax.Updater = function (container, url, opts) {
            updater_url = url;
        };
        $j.ajax = function (opts) {
            options = opts;
        };
        afterRef = null;
    });

    afterEach(function () {
        AjaxRefreshers.clear();
        Ajax.Updater = actual_ajax_updater;
        $j.ajax = actual_jquery_ajax;
    });

    it("test_page_change", function () {
        StageHistory._changePage('url', "4");
        assertEquals("url", updater_url);
        assertEquals("4", $("stage-history-page").value);
    });

    function stub_main_refresher() {
        AjaxRefreshers.addRefresher({afterRefreshOf: function (id, fn) {
            assertEquals('stage_history', id);
            afterRef = fn;
        }}, true);
    }

    it("test_bind_link_no_link", function () {
        stub_main_refresher();
        $j("#stage-history-page").val("should-not-change");
        assertNull($('doesnt_exist'));
        assertNull(document.getElementById('doesnt_exist'));
        StageHistory.bindHistoryLink('doesnt_exist', "url-to-page-3", 3);
        $j(document).click();
        assertEquals("should-not-change", $("stage-history-page").value);
        afterRef();
        $j(document).click();
        assertEquals("should-not-change", $("stage-history-page").value);
    });

    it("test_bind_link", function () {
        stub_main_refresher();
        $j("#stage-history-page").value = "0";
        StageHistory.bindHistoryLink('#stage_history_3', "url-to-page-3", 3);
        $j('#stage_history_3').click();
        assertEquals("3", $("stage-history-page").value);
        $j("#stage-history-page").value = "0";
        afterRef();
        $j('#stage_history_3').click();
        assertEquals("3", $("stage-history-page").value);
    });
});
