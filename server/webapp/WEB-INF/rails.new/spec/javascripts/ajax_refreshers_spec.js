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

describe("ajax_refreshers", function () {
    var first, second;
    beforeEach(function () {
        setFixtures("<div class='under_test'>\n" +
                "    <div id=\"elem_id\"></div>\n" +
                "</div>"
        );
        AjaxRefreshers.clear();
        first = mockRefresher();
        second = mockRefresher();
        AjaxRefreshers.addRefresher(first);
        AjaxRefreshers.addRefresher(second);
    });
    function mockRefresher() {
        return {
            stopRefresh: function () {
                this.stopped = true;
            },

            restartRefresh: function () {
                this.started = true;
            }
        };
    }

    afterEach(function () {
        AjaxRefreshers.clear();
    });

    /* TODO: SHOULD BE THE FIRST TEST TO RUN AS SUBSEQUENT TESTS MODIFY AJAXREFRESHERS WHICH IS A GLOBAL OBJECT <Rajesh/Sachin> */
    it("test_default_main_ajax_refresher_should_execute_callback", function () {
        var wasCalled = false;
        AjaxRefreshers.main().afterRefreshOf("foo", function () {
            wasCalled = true;
        });
        assertTrue("Default main refresher must call the afterRefreshOf without waiting for anything", wasCalled);
    });

    it("test_should_understand_main_ajax_refresher", function () {
        var main = new mockRefresher();
        var another = new mockRefresher();
        AjaxRefreshers.addRefresher(main, true);
        AjaxRefreshers.addRefresher(another, false);
        assertTrue("Should return whatever refresher that was marked main", main === AjaxRefreshers.main());
        assertTrue("Should not return non main refresher", another !== AjaxRefreshers.main());
    });

    it("test_should_disable_all_ajax_refreshers", function () {
        AjaxRefreshers.disableAjax();
        assertEquals("Should have disabled ajax on all refreshers", true, first.stopped);
        assertEquals("Should have disabled ajax on all refreshers", true, second.stopped);
    });

    it("test_should_enable_all_ajax_refreshers", function () {
        AjaxRefreshers.enableAjax();
        assertEquals("Should have enabled ajax on all refreshers", true, first.started);
        assertEquals("Should have enabled ajax on all refreshers", true, second.started);
    });
});
