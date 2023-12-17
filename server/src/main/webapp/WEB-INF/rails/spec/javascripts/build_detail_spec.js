/*
 * Copyright 2023 Thoughtworks, Inc.
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
describe("build_detail", function () {

  var originalBuildDetail = BuildDetail;
  beforeEach(function () {
    BuildDetail = originalBuildDetail;
    contextPath = "/go";
    setFixtures(`
      <div id="dashboard_download_project1_log123.xml_abc_0" class="subdir-container" style="display:none"></div>
      <div class="dir-container">
          <span id="directory" class="directory"><a id="link" onclick=ajax_tree_navigator("url")>$fileName</a></span>
      </div>
      <div id="dashboard_download_project1_log123.xml_abc" class="subdir-container" style="display:none"></div>
      <div id="dashboard_download_project1_log123.xml_abc_1" class="subdir-container" style="display:none"></div>
      
      <div id="any">
          <div  class="sub_tabs_container">
          <ul>
              <li id="li1" class="current_tab"><a>dummy-first-tab</a><a class="tab_button_body_match_text">dummy-first-tab</a></li>
              <li id="li2"><a>dummy-second-tab</a><a class="tab_button_body_match_text">dummy-second-tab</a></li>
          </ul>
          </div>
      </div>
      <div id="tab-content-of-dummy-first-tab">
      </div>
      <div id="tab-content-of-dummy-second-tab" style="display:none">
      </div>
      <div class="widget" id="build-output-console-warnning" style="display: none;">No build output.</div>
    `);

    window.tabsManager = new TabsManager();
  });

  afterEach(function () {
    BuildDetail = originalBuildDetail;
    contextPath = undefined;
    window.tabsManager = undefined;
  });

  it("test_should_expand_the_directory", function () {
    var expectedId = "#dashboard_download_project1_log123\\.xml_abc";
    assertFalse(jQuery(expectedId).is(':visible'));
    assertTrue('need class name directory', jQuery('#directory').hasClass('directory'));
    BuildDetail.tree_navigator(jQuery('#link'), "dashboard/download/project1/log123.xml/abc");
    assertTrue('div should be visible', jQuery(expectedId).is(':visible'));
    assertTrue('class name should be opened_directory', jQuery('#directory').hasClass('opened_directory'));
    BuildDetail.tree_navigator(jQuery('#link'), "dashboard/download/project1/log123.xml/abc");
    assertFalse(jQuery(expectedId).is(':visible'));
    assertTrue(jQuery('#directory').hasClass('directory'));
  });

  it("test_should_click_current_tab_element_should_not_move_current_tab", function () {
    jQuery('#li1').click();
    assertTrue(jQuery('#li1').hasClass('current_tab'));
    assertTrue(jQuery('#tab-content-of-dummy-first-tab').is(':visible'));
    jQuery('#li1').click();
    assertTrue(jQuery('#li1').hasClass('current_tab'));
    assertTrue(jQuery('#tab-content-of-dummy-first-tab').is(':visible'));
  });

  it("test_should_click_another_element_should_move_current_tab", function () {
    assertTrue("'li1' should have class 'current_tab'", jQuery('#li1').hasClass('current_tab'));
    assertTrue("'tab-content-of-dummy-first-tab' should be visible", jQuery('#tab-content-of-dummy-first-tab').is(':visible'));
    assertFalse("'tab-content-of-dummy-second-tab' should not be visible", jQuery('#tab-content-of-dummy-second-tab').is(':visible'));
    jQuery('#li2').click();
    assertTrue("'li2' should have class 'current_tab'", jQuery('#li2').hasClass('current_tab'));
    assertFalse("'li1' should not have class 'current_tab'", jQuery('#li1').hasClass('current_tab'));
    assertTrue("'dummy-second-tab' should be visible", jQuery('#tab-content-of-dummy-second-tab').is(':visible'));
    assertFalse("'dummy-first-tab' should not be visible", jQuery('#tab-content-of-dummy-first-tab').is(':visible'));
  });

  it("test_should_return_subcontainer", function () {
    var theContainer = BuildDetail.getSubContainer(jQuery("#link"));
    assertTrue("should return dashboard_download_project1_log123.xml_abc", theContainer.id == "dashboard_download_project1_log123.xml_abc");
  });
});
