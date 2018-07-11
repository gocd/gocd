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

describe("add_new_package_definition_state", function () {
    var originalPackageMaterialDefinition = PackageMaterialDefinition;
    var originalMailbox = Modalbox;
    beforeEach(function () {
        setFixtures("<input id=\"repo_containter\" value=\"value\"/>\n" +
            "<div id=\"packageConfigContainer\"></div>\n" +
            "<span id=\"saveButton\">Save Button</span>\n" +
            "<div id=\"packageContainer\"></div>\n");
        PackageMaterialDefinition = originalPackageMaterialDefinition;
    });

    afterEach(function(){
        PackageMaterialDefinition = originalPackageMaterialDefinition;
        Modalbox = originalMailbox;
    });

    it("test_should_setup_MBFocusable_on_elements_inserted_via_ajax", function () {
        var success_message = "set via callback"
        jQuery.ajax = function (options) {
            options.success('success data');
        };
        PackageMaterialDefinition = {
            prototype: {
                displayErrorMessageIfPluginIsMissing: function () {
                }
            }
        };
        Modalbox = {
            focusableElements: "data before callback",
            _findFocusableElements: function () {
                return success_message;
            }
        };
        var is_plugin_missing_handle = {
            is_plugin_missing: false
        };

        var data = {'value': is_plugin_missing_handle}
        var def = new AddNewPackageDefinitionState(data, [], [], jQuery("#repo_containter"), jQuery("#packageContainer"), jQuery("#packageConfigContainer"), [], jQuery("#saveButton"), "url");
        def.initialize();
        expect(success_message).toBe(Modalbox.focusableElements);
    });

});