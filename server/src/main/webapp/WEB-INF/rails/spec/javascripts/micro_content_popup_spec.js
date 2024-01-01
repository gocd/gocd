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
describe("micro_content_popup", function () {
  beforeEach(function () {
    setFixtures(
      `<div class='under_test'>
            <a href="#" id="show_popup" class="build_cause_link">show it to me!</a>
            <a href="#" id="another_popup" class="build_cause_link">show it to me!</a>
            <span id="random_element">show it to me!</span>
        
            <div id="content_box" class="hidden">
                some text
            </div>
          </div>`
    );

    content_box = $('#content_box');
    popup = new MicroContentPopup(content_box);
    popup_shower = new MicroContentPopup.ClickShower(popup);
    show_link = $('#show_popup');
    popup_shower.bindShowButton(show_link.get(0));
  });
  var popup = null;
  var popup_shower = null;
  var show_link = null;
  var content_box = null;

  afterEach(function () {
    popup_shower.close();
  });

  it("test_should_close_popup_when_the_link_is_clicked_again", function () {
    show_link.click();
    expect(content_box.hasClass("hidden")).toBe(false);
    show_link.click();
    expect(content_box.hasClass("hidden")).toBe(true);
  });

  it("test_should_close_popup_when_a_random_element_is_clicked", function () {
    show_link.click();
    expect(content_box.hasClass("hidden")).toBe(false);
    $("#random_element").click();
    expect(content_box.hasClass("hidden")).toBe(true);
  });
});
