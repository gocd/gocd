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
describe("util", function () {
  beforeEach(function () {
    setFixtures(`
      <div class='under_test'>
          <div id="elem_id">
              <a href="turner">Turner's legacy</a>
          </div>
      </div>
    `);
  });

  it("test_should_replace_element_with_spinny_image", function () {
    Util.spinny('elem_id');
    expect($('#elem_id').html()).toBe("&nbsp;");
    expect($('#elem_id').hasClass('spinny')).toBe(true);
  });

  it("test_should_do_nothing_on_empty_spinner_id", function () {
    Util.spinny('');
    expect($('#elem_id').html()).toContain("<a href=\"turner\">Turner's legacy</a>");
  });

  it("test_idToSelector", function () {
    expect(Util.idToSelector("2.1.1.2:15")).toBe("#2\\.1\\.1\\.2\\:15");
  });
});
