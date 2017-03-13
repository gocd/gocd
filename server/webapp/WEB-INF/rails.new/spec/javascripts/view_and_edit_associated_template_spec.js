/*
 * Copyright 2017 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

describe("ViewAndEditAssociatedTemplate", function () {
  beforeEach(function () {
    setFixtures("<div class='under_test'>\n" +
      "    <div class=\"templates\">\n" +
      "        <select id=\"select_template\" name=\"pipeline[templateName]\">\n" +
      "            <option value=\"template1\">template1</option>\n" +
      "            <option value=\"template2\" selected=\"selected\">template2</option>\n" +
      "        </select>\n" +
      "        <span id='links' />\n" +
      "    </div>\n" +
      "\n" +
      "</div>");
  });

  var actual_url = null;
  var actual_options = null;
  var actual_error_wrapper = null;

  beforeEach(function () {
    Util.ajax_modal = function (url, options, error_wrapper) {
      actual_url = url;
      actual_options = options;
      actual_error_wrapper = error_wrapper;
    }
  });

  afterEach(function () {
    actual_url = null;
    actual_options = null;
    actual_error_wrapper = null;
  });

  it("should open the modal with the selected template", function () {
    var templateMap = {template1: {canView: true, canEdit: true}, template2: {canView: true, canEdit: true}};
    viewAndEditAssociatedTemplate("#select_template").addViewAndEditTemplateLinks(templateMap, "go/config_view/templates/__template_name__", "go/admin/templates/__template_name__/general" );
    fire_event(jQuery('a.view_template_link').get(0), 'click');

    assertEquals("go/config_view/templates/template2", actual_url);
    assertEquals(false, actual_options.overlayClose);
    assertEquals('template2', actual_options.title);
    assertEquals('text', actual_error_wrapper("text"));
  });

  it('should disable the view link if unauthorized to view selected template', function () {
    var templateMap = {template1: {canView: true, canEdit: true}, template2: {canView: false, canEdit: false}};
    viewAndEditAssociatedTemplate("#select_template").addViewAndEditTemplateLinks(templateMap, "/go/config_view/templates/__template_name__", "/go/admin/templates/__template_name__/general" );
    assertEquals("Unauthorized to view template", jQuery("span.view_template_link").attr('title'));
  });

  it('should create and enable the button to edit selected templated for authorized user', function () {
    var templateMap = {template1: {canView: false, canEdit: false}, template2: {canView: true, canEdit: true}};
    viewAndEditAssociatedTemplate("#select_template").addViewAndEditTemplateLinks(templateMap, "/go/config_view/templates/__template_name__", "/go/admin/templates/__template_name__/general" );
    assertEquals("Edit", jQuery("a.edit_template_link").html());
    assertEquals("/go/admin/templates/template2/general", jQuery("a.edit_template_link").attr('href'));
  });

  it('should create and enable the button to edit selected templated for authorized user', function () {
    var templateMap = {template1: {canView: false, canEdit: false}, template2: {canView: true, canEdit: false}};
    viewAndEditAssociatedTemplate("#select_template").addViewAndEditTemplateLinks(templateMap, "/go/config_view/templates/__template_name__", "/go/admin/templates/__template_name__/general" );
    assertEquals("Edit", jQuery("span.edit_template_link").html());
    assertEquals("Unauthorized to edit template", jQuery("span.edit_template_link").attr('title'));
  });

  it('should not construct view and edit links if template name is blank', function () {
    setFixtures("<div class='under_test'>\n" +
      "    <div class=\"templates\">\n" +
      "    </div>\n" +
      "\n" +
      "</div>");
    viewAndEditAssociatedTemplate("#select_template").addViewAndEditTemplateLinks({}, "go/config_view/templates/__template_name__", "go/admin/templates/__template_name__/general" )
    assertEquals(null, jQuery("span.edit_template_link").html());
    assertEquals(null, jQuery("span.view_template_link").html());
    assertEquals(null, jQuery("a.view_template_link").html());
    assertEquals(null, jQuery("a.edit_template_link").html());
  });
});