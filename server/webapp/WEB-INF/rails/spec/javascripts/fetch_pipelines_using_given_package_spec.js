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

describe("fetch_pipelines_using_given_package", function () {
    var orignialAjaxGet = jQuery.get;
    beforeEach(function () {
        setFixtures("<div class='under_test'>\n" +
            "    <a href=\"#\" id=\"link\">show pipelines used in</a>\n" +
            "\n" +
            "    <div class=\"container\"></div>\n" +
            "</div>");
    });

    afterEach(function () {
        jQuery.get = orignialAjaxGet;
    });

    beforeEach(function () {
    });

    it("testShouldGetPiplinesUsedInBasedOnSelectedPackageId", function () {
        new FetchPipelinesUsingGivenPackage("url", "#link", ".container").init();
        var wasCalled = false;
        jQuery.get = function (url, func) {
            if (url == "url") {
                wasCalled = true
            }
            func("data");

        };
        jQuery("#link").trigger("click");
        assertEquals(true, wasCalled);
        assertEquals(jQuery(".container").html(), "data");
    });
});
