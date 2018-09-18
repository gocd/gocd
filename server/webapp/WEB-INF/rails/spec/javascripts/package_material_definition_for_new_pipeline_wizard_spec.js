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

describe("package_material_definition_for_new_pipeline_wizard", function () {
    beforeEach(function () {
        jQuery.ajax = function () {
        }

        data = {
            "tw-repo1": {"name": "tw-repo1-name", "plugin_id": "yum", "is_plugin_missing": false, "packages": [
                {"id": "p1", "name": "package1-name"},
                {"id": "p2", "name": "package3-name"}
            ]},
            "tw-repo2": {"name": "tw-repo2-name", "plugin_id": "yum", "is_plugin_missing": false, "packages": [
                {"id": "p3", "name": "package3-name"},
                {"id": "p4", "name": "package4-name"}
            ]},
            "tw-repo3": {"name": "tw-repo3-name", "plugin_id": "missing_plugin", "is_plugin_missing": true, "packages": [
                {"id": "p5", "name": "package5-name"},
                {"id": "p6", "name": "package6-name"}
            ]}
        }

        setFixtures("<div class='under_test'>\n" +
            "    <form id=\"pipeline_edit_form\">\n" +
            "        <select id=\"repository\"></select>\n" +
            "        <div>\n" +
            "            <input id=\"chooseExisting\" type=\"radio\" name=\"material[create_or_associate_pkg_def]\" checked=\"checked\" value=\"associate\">\n" +
            "            <input id=\"addNew\" type=\"radio\" name=\"material[create_or_associate_pkg_def]\" value=\"create\">\n" +
            "        </div>\n" +
            "        <select id=\"package\" class=\"required\"></select>\n" +
            "\n" +
            "        <div class=\"package_configuration\"></div>\n" +
            "    </form>\n" +
            "</div>");
    });

    var data;
    var orignialAjax = jQuery.ajax;

    afterEach(function () {
        data = null;
        jQuery.ajax = orignialAjax;
    });

    it("testShouldInitializeRepositoryAndPackageDropdowns_AddNewMaterial", function () {
            new PackageMaterialDefinitionForNewPipelineWizard(data, null, null, "show_path", "new_path", "oops...no plugin!!!").init();
            var options = jQuery("#repository option");
            assertEquals(4, options.length);
            assertContains("[Select]", jQuery(options[0]).text());
            assertContains("tw-repo1-name", jQuery(options[1]).text());
            assertContains("tw-repo2-name", jQuery(options[2]).text());
            assertContains("tw-repo3-name", jQuery(options[3]).text());
            assertEquals("", jQuery("#repository").val());
            assertEquals("", jQuery("#package").val());
            assertEquals(true, jQuery("#package").is(":disabled"));
            assertEquals(true, jQuery("input[value*='associate']").is(":disabled"));
            assertEquals(true, jQuery("input[value*='create']").is(":disabled"));
        }
    );

    it("testShouldPopulatePackagesBasedOnSelectedRepository", function () {
            new PackageMaterialDefinitionForNewPipelineWizard(data, null, null, "show_path", "new_path", "oops...no plugin!!!").init();
            selectRepositoryOption("tw-repo2");

            verifyPackageOptionsIsForRepo2WithSelectedValue("");
        }
    );

    it("testShouldDisablePackageDropdownIfNoRepositoryIsSelected", function () {
            new PackageMaterialDefinitionForNewPipelineWizard(data, null, null, "show_path", "new_path", "oops...no plugin!!!").init();
            selectRepositoryOption("");

            var options = jQuery("#package option");
            assertEquals(true, jQuery("#package").is(":disabled"));
            assertEquals(true, jQuery("input[value*='associate']").is(":disabled"));
            assertEquals(true, jQuery("input[value*='create']").is(":disabled"));

            selectRepositoryOption("tw-repo2");
            assertEquals(false, jQuery("#package").is(":disabled"));
            assertEquals(false, jQuery("input[value*='associate']").is(":disabled"));
            assertEquals(false, jQuery("input[value*='create']").is(":disabled"));
        }
    );

    it("testShouldPreselectRepoAndPackageIfProvided_EditMaterial", function () {
            new PackageMaterialDefinitionForNewPipelineWizard(data, "tw-repo2", "p3", "show_path", "new_path", "oops...no plugin!!!").init();
            assertEquals("tw-repo2", jQuery("#repository").val());
            assertEquals("p3", jQuery("#package").val());
            assertEquals("package3-name", jQuery("#package option[value='p3']").text());
            assertEquals(false, jQuery("#package").is(":disabled"));
            assertEquals(false, jQuery("input[value*='associate']").is(":disabled"));
            assertEquals(false, jQuery("input[value*='create']").is(":disabled"));
        }
    );

    it("testShouldDisabledAndHidePackageDropdownAndCallGetNewPkgDefinition_IfAddNewOptionIsSelected", function () {
            var wasCalled = false;
            jQuery.ajax = function (options) {
                if (options.url == "new_path") {
                    wasCalled = true
                }
            };

            new PackageMaterialDefinitionForNewPipelineWizard(data, null, null, "show_path", "new_path", "oops...no plugin!!!").init();
            selectRepositoryOption("tw-repo2");
            assertEquals(false, jQuery("#package").is(":disabled"));

            selectAddNewOrExistingOption('create');
            assertEquals(true, jQuery("#package").is(":disabled"));
            assertEquals("hidden", jQuery("#package").attr("hidden"));
            assertEquals(true, wasCalled)
        }
    );

    it("testShouldPopulatePackageOptionsBasedOnCurrentlySelectedRepoWhenSwitchingToChooseExisting", function () {
            new PackageMaterialDefinitionForNewPipelineWizard(data, null, null, "show_path", "new_path", "oops...no plugin!!!").init();
            selectRepositoryOption("tw-repo2");

            selectAddNewOrExistingOption('create');
            selectRepositoryOption("tw-repo1");

            jQuery(".package_configuration").html("contents of new page");

            selectAddNewOrExistingOption('associate');
            verifyPackageOptionsIsForRepo1();
            assertEquals("", jQuery(".package_configuration").text())
        }
    );

    it("testShouldClearPackageConfigContainerIfNoPackageIsSelected", function () {
            new PackageMaterialDefinitionForNewPipelineWizard(data, "tw-repo2", "p3", "show_path", "new_path", "oops...no plugin!!!").init();
            jQuery(".package_configuration").html("some text");
            selectPackageOption("");
            assertEquals("", jQuery(".package_configuration").text())
        }
    );

    it("testShouldResetPackageIfNoRepoIsSelected", function () {
            new PackageMaterialDefinitionForNewPipelineWizard(data, null, null, "show_path", "new_path", "oops...no plugin!!!").init();
            selectRepositoryOption("tw-repo2");
            selectPackageOption("p3");
            jQuery(".package_configuration").html("some text");

            selectRepositoryOption("");

            assertEquals("", jQuery("#package").val());
            assertEquals(true, jQuery("#package").is(":disabled"));
            assertEquals("", jQuery(".package_configuration").text());
        }
    );

    xit("testShouldPopulateRelevantPackageOptionsAndConfigWhenSwitchingFromAddNewToChooseExisting", function () {
            var callToGetShowConfig = false;
            jQuery.ajax = function (options) {
                if (options.url == "show_path") {
                    callToGetShowConfig = true;
                }
            };
            new PackageMaterialDefinitionForNewPipelineWizard(data, null, null, "show_path", "new_path", "oops...no plugin!!!").init();
            selectRepositoryOption("tw-repo2");
            selectPackageOption("p3");
            assertEquals(true, callToGetShowConfig);
            callToGetShowConfig = false;

            selectAddNewOrExistingOption('create');

            selectAddNewOrExistingOption('associate');
            assertEquals("disabled", false, jQuery("#package").is(":disabled"));
            assertEquals("hidden", false, jQuery("#package").is(":hidden"));

            verifyPackageOptionsIsForRepo2WithSelectedValue("");
//            assertEquals(true, callToGetShowConfig);
        }
    );

    xit("testShouldDisableAndDisplayErrorMessageIfSelectedRepoIsAssociatedToAMissingPlugin", function () {
            new PackageMaterialDefinitionForNewPipelineWizard(data, null, null, "show_path", "new_path", "oops...plugin ''{0}'' missing!!!").init();
            var packageConfigDiv = jQuery(".package_configuration");

            selectRepositoryOption("tw-repo3");
            var displayedError = "oops...plugin 'missing_plugin' missing!!!";
            assertEquals("1", displayedError, packageConfigDiv.text());

            selectAddNewOrExistingOption('create');
            assertEquals("2", displayedError, packageConfigDiv.text());

            selectAddNewOrExistingOption('associate');
            assertEquals("3", displayedError, packageConfigDiv.text());

            selectRepositoryOption("tw-repo2");
            assertEquals("4", "", packageConfigDiv.text());

            selectAddNewOrExistingOption('create');
            assertEquals("5", "", packageConfigDiv.text());

            selectRepositoryOption("tw-repo3");
            assertEquals("6", displayedError, packageConfigDiv.text());
        }
    );

    it("testShouldClearErrorMessagesForChooseExistingScenarioIfUserSwitchesToCreateNewOption", function () {
        new PackageMaterialDefinitionForNewPipelineWizard(data, null, null, "show_path", "new_path", "oops...no plugin!!!").init();
        selectRepositoryOption("tw-repo2");
        jQuery("#package").valid();
        assertEquals(false, jQuery("label[for='package']").is(":hidden"));
        selectAddNewOrExistingOption('create');
        assertEquals(true, jQuery("label[for='package']").is(":hidden"));
    });

// utility
    function selectRepositoryOption(option) {
        jQuery("#repository").val(option);
        jQuery("#repository").trigger("change");
    }

    function selectPackageOption(option) {
        jQuery("#package").val(option);
        jQuery("#package").trigger("change");
    }

    function selectAddNewOrExistingOption(option) {
        jQuery("input[value*='" + option + "']").attr("checked", "checked");
        jQuery("input[name*='material[create_or_associate_pkg_def]']").trigger("change");
    }

    function verifyPackageOptionsIsForRepo1() {
        var options = jQuery("#package option");
        assertEquals(3, options.length);
        assertContains("[Select]", jQuery(options[0]).text());
        assertContains("package1-name", jQuery(options[1]).text());
        assertContains("package3-name", jQuery(options[2]).text());
        assertEquals("", jQuery("#package").val());
    }

    function verifyPackageOptionsIsForRepo2WithSelectedValue(selectedPackage) {
        selectedPackage = selectedPackage || "";
        var options = jQuery("#package option");
        assertEquals(3, options.length);
        assertContains("[Select]", jQuery(options[0]).text());
        assertContains("package3-name", jQuery(options[1]).text());
        assertContains("package4-name", jQuery(options[2]).text());
        assertEquals(selectedPackage, jQuery("#package").val());
    }
});
