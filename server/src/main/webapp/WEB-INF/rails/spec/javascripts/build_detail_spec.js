/*
 * Copyright 2024 Thoughtworks, Inc.
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
    expect($(expectedId).is(':visible')).toBe(false);
    expect($('#directory').hasClass('directory')).toBe(true);
    BuildDetail.tree_navigator($('#link'), "dashboard/download/project1/log123.xml/abc");
    expect($(expectedId).is(':visible')).toBe(true);
    expect($('#directory').hasClass('opened_directory')).toBe(true);
    BuildDetail.tree_navigator($('#link'), "dashboard/download/project1/log123.xml/abc");
    expect($(expectedId).is(':visible')).toBe(false);
    expect($('#directory').hasClass('directory')).toBe(true);
  });

  it("test_should_click_current_tab_element_should_not_move_current_tab", function () {
    $('#li1').click();
    expect($('#li1').hasClass('current_tab')).toBe(true);
    expect($('#tab-content-of-dummy-first-tab').is(':visible')).toBe(true);
    $('#li1').click();
    expect($('#li1').hasClass('current_tab')).toBe(true);
    expect($('#tab-content-of-dummy-first-tab').is(':visible')).toBe(true);
  });

  it("test_should_click_another_element_should_move_current_tab", function () {
    expect($('#li1').hasClass('current_tab')).toBe(true);
    expect($('#tab-content-of-dummy-first-tab').is(':visible')).toBe(true);
    expect($('#tab-content-of-dummy-second-tab').is(':visible')).toBe(false);
    $('#li2').click();
    expect($('#li2').hasClass('current_tab')).toBe(true);
    expect($('#li1').hasClass('current_tab')).toBe(false);
    expect($('#tab-content-of-dummy-second-tab').is(':visible')).toBe(true);
    expect($('#tab-content-of-dummy-first-tab').is(':visible')).toBe(false);
  });

  it("test_should_return_subcontainer", function () {
    var theContainer = BuildDetail.getSubContainer($("#link"));
    expect(theContainer.id == "dashboard_download_project1_log123.xml_abc").toBe(true);
  });
});
