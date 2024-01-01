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
describe("field_state_replicator", function () {
  const replicator = new FieldStateReplicator();
  let chkbox_a;
  let chkbox_b;
  let text_a;
  let text_b;

  beforeEach(function () {
    setFixtures(
      `<div class='under_test'>
        <form name='form_a' id="form_a">
          <input type="checkbox" id="form_a_chkbox"/>
          <input type="text" id="form_a_textfield"/>
        </form>
        <form name='form_b' id="form_b">
          <input type="checkbox" id="form_b_chkbox"/>
          <input type="text" id="form_b_textfield"/>
        </form>
      </div>`
    );
    chkbox_a = $('#form_a_chkbox');
    chkbox_b = $('#form_b_chkbox');
    text_a = $('#form_a_textfield');
    text_b = $('#form_b_textfield');
  });

  beforeEach(function () {
    text_a.val("");
    chkbox_a.prop('checked', false);
    text_b.val("foo bar");
    chkbox_b.prop('checked', false);
    replicator.register(chkbox_a[0], 'chkbox');
    replicator.register(text_a[0], 'text');
  });

  afterEach(function () {
    replicator.unregister(chkbox_a[0], 'chkbox');
    replicator.unregister(chkbox_b[0], 'chkbox');
    replicator.unregister(text_a[0], 'text');
    replicator.unregister(text_b[0], 'text');
  });

  it("test_replicates_checkbox_value_after_registration", function () {
    expect(chkbox_b.is(':checked')).toBe(false);
    replicator.register(chkbox_b[0], 'chkbox');
    chkbox_a.prop('checked', true);
    chkbox_a.change();
    expect(chkbox_a.is(':checked')).toBe(true);
    expect(chkbox_b.is(':checked')).toBe(true);
    chkbox_b.prop('checked', false);
    chkbox_b.change();
    expect(chkbox_b.is(':checked')).toBe(false);
    expect(chkbox_a.is(':checked')).toBe(false);
  });

  it("test_replicates_text_field_value_after_registration", function () {
    expect(text_b.val()).toBe("foo bar");
    replicator.register(text_b[0], 'text');
    text_a.val("baz quux");
    text_a.change();
    expect(text_a.val()).toBe("baz quux");
    expect(text_b.val()).toBe("baz quux");
    text_b.val("bar baz");
    text_b.change();
    expect(text_b.val()).toBe("bar baz");
    expect(text_a.val()).toBe("bar baz");
  });

  it("test_replicates_checkbox_value_on_registration", function () {
    chkbox_a.prop('checked', true);
    expect(chkbox_a.is(':checked')).toBe(true);
    expect(chkbox_b.is(':checked')).toBe(false);
    replicator.register(chkbox_b[0], 'chkbox');
    expect(chkbox_a.is(':checked')).toBe(true);
    expect(chkbox_b.is(':checked')).toBe(true);
  });

  it("test_replicates_text_field_value_on_registration", function () {
    text_a.val("baz quux");
    expect(text_a.val()).toBe("baz quux");
    expect(text_b.val()).toBe("foo bar");
    replicator.register(text_b[0], 'text');
    expect(text_a.val()).toBe("baz quux");
    expect(text_b.val()).toBe("baz quux");
  });

  it("test_stops_replicating_checkbox_value_after_unregistration", function () {
    replicator.register(chkbox_b[0], 'chkbox');
    replicator.unregister(chkbox_b[0], 'chkbox');
    chkbox_a.prop('checked', true);
    chkbox_a.change();
    expect(chkbox_a.is(':checked')).toBe(true);
    expect(chkbox_b.is(':checked')).toBe(false);
    chkbox_a.prop('checked', false);
    chkbox_b.prop('checked', true);
    chkbox_b.change();
    expect(chkbox_a.is(':checked')).toBe(false);
    expect(chkbox_b.is(':checked')).toBe(true);
  });

  it("test_stops_replicating_text_field_value_after_registration", function () {
    replicator.register(text_b[0], 'text');
    replicator.unregister(text_b[0], 'text');
    text_b.val("foo bar");
    text_a.val("hello world");
    text_a.change();
    expect(text_a.val()).toBe("hello world");
    expect(text_b.val()).toBe("foo bar");
    text_b.val("bar baz");
    text_b.change();
    expect(text_a.val()).toBe("hello world");
    expect(text_b.val()).toBe("bar baz");
  });
});
