/*************************GO-LICENSE-START*********************************
 * Copyright 2017 ThoughtWorks, Inc.
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

describe("pluggable_scm_configuration_module", function () {
    beforeEach(function () {
        setFixtures("<form accept-charset=\"UTF-8\" action=\"/go/admin/pipelines\" class=\"new_pipeline_group\" id=\"pipeline_edit_form\" method=\"post\" name=\"pipeline_edit_form\" novalidate=\"novalidate\"><div style=\"margin:0;padding:0;display:inline\"><input name=\"utf8\" type=\"hidden\" value=\"✓\"><input name=\"authenticity_token\" type=\"hidden\" value=\"DT/yFqLOMDb2JzWpGI3ZQmAa51+vzsvlwfiF/2EdmNc=\"></div>\n" +
        "\n" +
        "    <input id=\"config_md5\" name=\"config_md5\" type=\"hidden\" value=\"53231bc673457061df6bb0653427351b\">\n" +
        "\n" +
            "<div class=\"steps_wrapper sub_tabs_container\"></div>\n" +
            "<div class=\"steps_panes panes sub_tab_container_content\">\n" +
              "<div id=\"tab-content-of-materials\">\n" +
                "<div class=\"form_content\">\n" +
                  "<div id=\"material_forms\" class=\"fieldset\">\n" +
                    "<div class=\"hidden material_entry pluggable_material_pluginId\">\n" +
            "\n" +
                      "<div class=\"form_item\">\n" +
            "\n" +
                        "<div id=\"pipeline_group_pipeline_materials_pluggable_material_pluginId_angular_container\" ng-controller=\"PluggableSCMConfigurationController\">\n" +
                          "<input id=\"pipeline_group_pipeline_materials_pluggable_material_pluginId_pluginId\" name=\"pipeline_group[pipeline][materials][pluggable_material_pluginId][pluginId]\" type=\"hidden\" value=\"com.plugin.id\" />\n" +
                          "<input id=\"pipeline_group_pipeline_materials_pluggable_material_pluginId_scmId\" name=\"pipeline_group[pipeline][materials][pluggable_material_pluginId][scmId]\" type=\"hidden\" />    \n" +
                          "<div class=\"form_item_block required\">\n" +
                            "<label for=\"pipeline_group_pipeline_materials_pluggable_material_pluginId_name\">Material Name<span class='asterisk'>*</span></label>\n" +
                            "<input class=\"form_input required\" name=\"pipeline_group[pipeline][materials][pluggable_material_pluginId][name]\" type=\"text\" />      \n" +
                          "</div>\n" +
                          "<div class=\"plugged_material_template required\">\n" +
            "\n" +
                            "<div class=\"form_item_block\">\n" +
                              "<label>URL:<span class=\"asterisk\">*</span></label>\n" +
                              "<input type=\"text\" ng-model=\"url\" ng-required=\"true\" tabindex=\"1\"/>\n" +
                              "<span class=\"form_error\" ng-show=\"GOINPUTNAME[url].$error.server\">\n" +
                                "{{ GOINPUTNAME[url].$error.server }}\n" +
                              "</span>\n" +
                            "</div>\n" +
                            "<div class=\"form_item_block\">\n" +
                              "<label>Username:</label>\n" +
                              "<input type=\"text\" ng-model=\"username\" ng-required=\"false\" tabindex=\"2\"/>\n" +
                              "<span class=\"form_error\" ng-show=\"GOINPUTNAME[username].$error.server\">\n" +
                                "{{ GOINPUTNAME[username].$error.server }}\n" +
                              "</span>\n" +
                            "</div>\n" +
                            "<div class=\"form_item_block\">\n" +
                              "<label>Password:</label>\n" +
                              "<input type=\"password\" ng-model=\"password\" ng-required=\"false\" tabindex=\"3\"/>\n" +
                              "<span class=\"form_error\" ng-show=\"GOINPUTNAME[password].$error.server\">\n" +
                                "{{ GOINPUTNAME[password].$error.server }}\n" +
                              "</span>\n" +
                            "</div>\n" +
                          "</div>\n" +
            "\n" +
                          "<div class=\"form_item_block\">\n" +
                            "<label>Destination Directory</label>\n" +
                            "<input class=\"form_input\" name=\"pipeline_group[pipeline][materials][pluggable_material_pluginId][folder]\" type=\"text\" />    \n" +
                          "</div>\n" +
            "\n" +
                          "<div class=\"form_item_block checkbox_row material_options\">\n" +
                            "<input name=\"pipeline_group[pipeline][materials][pluggable_material_pluginId][autoUpdate]\" type=\"hidden\" value=\"0\" />\n" +
                            "<input checked=\"checked\" class=\"form_input\" id=\"pipeline_group_pipeline_materials_pluggable_material_pluginId_autoUpdate\" name=\"pipeline_group[pipeline][materials][pluggable_material_pluginId][autoUpdate]\" type=\"checkbox\" value=\"true\" />    \n" +
                            "<label for=\"pipeline_group_pipeline_materials_pluggable_material_pluginId_autoUpdate\">Poll for new changes</label>\n" +
                          "</div>\n" +
            "\n" +
                          "<button class=\"check_connection submit button\" ng-click=\"checkConnection('/go/admin/materials/pluggable_scm/check_connection/com.plugin.id')\" type=\"button\" value=\"CHECK CONNECTION\"><span>CHECK CONNECTION</span></button>    \n" +
                          "<div ng-class=\"['connection_test_message', check_connection_state]\">\n" +
                            "<pre ng-bind=\"check_connection_message\" ng-show=\"check_connection_message\"></pre>\n" +
                          "</div>\n" +
            "\n" +
                        "</div>\n" +
                      "</div>\n" +
                      "<p class=\"required\">\n" +
                        "<span class=\"asterisk\">*</span> indicates a required field\n" +
                      "</p>\n" +
            "\n" +
                    "</div>\n" +
            "\n" +
                  "</div>\n" +
                "</div>\n" +
              "</div>\n" +
            "</div>" +
        "</form>\n");
    });

    it("testShouldSetCorrectNameAttributesOnTemplateInputs", function () {
        var plugged_material_controller_element_id = "pipeline_group_pipeline_materials_pluggable_material_pluginId_angular_container";
        var form_name_prefix = "pipeline_group[pipeline][materials][pluggable_material_pluginId]";
        new PluggableSCMConfigurationModule().initialize(plugged_material_controller_element_id, form_name_prefix);
        new PluggableSCMConfigurationModule().bootstrap();

        assertNotNull(angular.module("pluggable_scm_configuration_module"));

        var template_selector = "#" + plugged_material_controller_element_id + " .plugged_material_template";
        assertEquals(3, jQuery(template_selector).find("input").size());
        assertEquals("pipeline_group[pipeline][materials][pluggable_material_pluginId][url]", jQuery(template_selector).find('input[ng-model="url"]')[0].name);
        assertEquals("pipeline_group[pipeline][materials][pluggable_material_pluginId][username]", jQuery(template_selector).find('input[ng-model="username"]')[0].name);
        assertEquals("pipeline_group[pipeline][materials][pluggable_material_pluginId][password]", jQuery(template_selector).find('input[ng-model="password"]')[0].name);
    });
});
