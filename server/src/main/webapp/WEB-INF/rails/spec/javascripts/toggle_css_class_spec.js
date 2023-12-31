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
describe("toggle_css_class", function () {

  beforeEach(function () {
    setFixtures(`
      <div class='under_test'>
        <div id="parent_div">
          <div>
            <a id="clickable">CLickable</a>
          </div>
        </div>
    
        <div id="pare.nt.div1" class="hidereveal_collapsed">
          <span class="hidereveal_expander">expander</span>
          <div class="hidereveal_content">contents here</div>
        </div>
      </div>`
    );
  });

  var clickable;
  var container;
  var originalmarkup;

  beforeEach(function () {
    originalmarkup = $(".under_test")[0].innerHTML;
    container = $("#pare\\.nt\\.div1");
    make_collapsable("pare.nt.div1");
    clickable = $(".hidereveal_expander")[0];
  });

  afterEach(function () {
    $(".under_test")[0].innerHTML = originalmarkup;
  });

  it("test_make_collapsible", function () {
    expect(container.hasClass('hidereveal_collapsed')).toBe(true);
    $(".hidereveal_expander").click();
    expect(container.hasClass('hidereveal_collapsed')).toBe(false);
  });

  it("test_prevents_click_from_bubbling", function () {
    var click_bubbled = false;
    $('#pare\\.nt\\.div1').on('click', function () {
      click_bubbled = true;
    });
    $(".hidereveal_expander").click();
    expect(click_bubbled).toBe(false);
  });
});
