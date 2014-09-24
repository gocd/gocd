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

describe("user_options_on_header", function () {
    beforeEach(function () {
        jQuery.ajax = function () {};

        setFixtures("<div class='under_test'>\n" +
            "    <div class=\"current_user icon\">\n" +
            "        <a class=\"current_user_name\" href=\"#\">admin</a>\n" +
            "        <ul class=\"enhanced_dropdown hidden\">\n" +
            "            <li>\n" +
            "                <a href=\"/go/tab/mycruise/user\">Preferences</a>\n" +
            "            </li>\n" +
            "            <li class=\"logout\">\n" +
            "                <a id=\"nav-logout\" class=\"sign_out\" href=\"/go/auth/logout\">Sign out</a>\n" +
            "            </li>\n" +
            "        </ul>\n" +
            "    </div>\n" +
            "</div>");

        new MicroContentPopup.register();
        new UserOptionsOnHeader().init();
    });

    var data;
    var orignialAjax = jQuery.ajax;

    afterEach(function () {
        jQuery.ajax = orignialAjax;
    });

    it("testShouldHideOptionsWithPreferencesOnUsernameByDefault", function () {
        assertEquals('1', true, jQuery(".enhanced_dropdown").hasClass("hidden"));
        assertEquals('2', false, jQuery(".current_user").hasClass("selected"));
    });

    xit("testShouldDisplayOptionsWithPreferencesOnUsernameLinkClick", function () {
        jQuery(".current_user").trigger('click');
        assertEquals('3', false, jQuery(".enhanced_dropdown").hasClass("hidden"));
        assertEquals('4', true, jQuery(".current_user").hasClass("selected"));

    });
    it("testShouldHideOptionsWithPreferencesOnUsernameOnDocumentClick", function () {
        jQuery(".current_user").trigger('click');
        jQuery(document).trigger('click');
        assertEquals('5', true, jQuery(".enhanced_dropdown").hasClass("hidden"));
        assertEquals('6', false, jQuery(".current_user").hasClass("selected"));
    });
});