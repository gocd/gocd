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

describe("package_repository_treeview", function () {
    beforeEach(function () {
        setFixtures("<div class='under_test'>\n" +
            "    <div class=\"navigation\">\n" +
            "      <input type=\"text\" placeholder=\"Search for Repository or Package\" class=\"search\">\n" +
            "\n" +
            "        <p class=\"no-items\" style=\"display: none\">No repository found.</p>\n" +
            "\n" +
            "        <ul class=\"repositories treenav accordion\">\n" +
            "            <li title=\"2222\" class=\"collapsed has-children\">\n" +
            "                <span class=\"handle\"></span>\n" +
            "                <a href=\"/go/admin/package_repositories/6de7c2f3-41cd-4155-b568-87c85eba567d/edit\">2222</a>\n" +
            "                <ul class=\"packages\">\n" +
            "                    <li class=\"selected last\" title=\"11\">\n" +
            "                        <a href=\"/go/admin/package_definitions/6de7c2f3-41cd-4155-b568-87c85eba567d/41b3ff98-be63-400e-916b-3f974d417aff/with_repository_list\">11</a>                                <form method=\"post\" id=\"delete_package_41b3ff98-be63-400e-916b-3f974d417aff\" class=\"edit_java_com_thoughtworks_cruise_domain_packagerepository_package_definition\" action=\"/go/admin/package_definitions/6de7c2f3-41cd-4155-b568-87c85eba567d/41b3ff98-be63-400e-916b-3f974d417aff\" style=\"display: none;\"><div style=\"margin:0;padding:0;display:inline\"><input type=\"hidden\" value=\"delete\" name=\"_method\"><input type=\"hidden\" value=\"ZyIeir8dGOj3unkWUJoKLEIWsZ3n9mksay1NI/6Fvac=\" name=\"authenticity_token\"></div>\n" +
            "                        <input type=\"hidden\" value=\"19e8301bd69a14431a71bb1117b64bba\" name=\"config_md5\" id=\"config_md5\">\n" +
            "                                <span id=\"package_delete_from_tree_41b3ff98-be63-400e-916b-3f974d417aff\">\n" +
            "                                <button id=\"delete_button_from_tree_41b3ff98-be63-400e-916b-3f974d417aff\" class=\"remove\" type=\"button\" title=\"Delete this package.\"></button>\n" +
            "                        </span>\n" +
            "\n" +
            "                    </form>\n" +
            "                    </li>\n" +
            "                </ul>\n" +
            "            </li>\n" +
            "            <li title=\"33\" class=\"has-children collapsed\">\n" +
            "                <span class=\"handle\"></span>\n" +
            "                <a href=\"/go/admin/package_repositories/3b71cc6a-8743-40b6-b67b-3da14e2832aa/edit\">33</a>\n" +
            "                <ul class=\"packages\">\n" +
            "                    <li class=\"empty-node last\"><span>No packages found</span></li>\n" +
            "                </ul>\n" +
            "            </li>\n" +
            "            <li title=\"222\" class=\"has-children collapsed\">\n" +
            "                <span class=\"handle\"></span>\n" +
            "                <a href=\"/go/admin/package_repositories/056c5bff-3b79-4c96-957d-f6f70ea09acf/edit\">222</a>\n" +
            "                <ul class=\"packages\">\n" +
            "                    <li class=\"empty-node last\"><span>No packages found</span></li>\n" +
            "                </ul>\n" +
            "            </li>\n" +
            "        </ul>\n" +
            "    </div>\n" +
            "</div>");
    });

    it("testShouldSearchThePackagesWithTextsEnteredInTextboxFromTreenav", function () {
        var searchBox = jQuery('.search');
        new CustomTreeView().init();
        new CustomTreeView().bindTreeSearch(searchBox);
        searchBox.val('222');
        searchBox.trigger('keyup');
        assertEquals(2, jQuery('.repositories > li:visible').length);
        searchBox.val('');
    });
    it("testShouldShowNotFoundMsgIfItsNotFindingAnyPackageOrRepo", function () {
        var searchBox = jQuery('.search');
        new CustomTreeView().init();
        new CustomTreeView().bindTreeSearch(searchBox);
        searchBox.val('abcd');
        searchBox.trigger('keyup');
        assertEquals(0, jQuery('.repositories > li:visible').length);
        assertEquals(true, jQuery('.no-items').is(":visible"));
    });

});