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
describe("field_state_replicator", function(){
  const replicator = new FieldStateReplicator();
  let chkbox_a;
  let chkbox_b;
  let text_a;
  let text_b;

  beforeEach(function(){
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
    chkbox_a = jQuery('#form_a_chkbox');
    chkbox_b = jQuery('#form_b_chkbox');
    text_a = jQuery('#form_a_textfield');
    text_b = jQuery('#form_b_textfield');
  });

  beforeEach(function() {
    text_a.val("");
    chkbox_a.prop('checked', false);
    text_b.val("foo bar");
    chkbox_b.prop('checked', false);
    replicator.register(chkbox_a[0], 'chkbox');
    replicator.register(text_a[0], 'text');
  });

  afterEach(function() {
    replicator.unregister(chkbox_a[0], 'chkbox');
    replicator.unregister(chkbox_b[0], 'chkbox');
    replicator.unregister(text_a[0], 'text');
    replicator.unregister(text_b[0], 'text');
  });

  it("test_replicates_checkbox_value_after_registration", function() {
    assertFalse("Form b checkbox must start out unchecked", chkbox_b.is(':checked'));
    replicator.register(chkbox_b[0], 'chkbox');
    chkbox_a.prop('checked', true);
    chkbox_a.change();
    assertTrue("Form a checkbox state change must stick", chkbox_a.is(':checked'));
    assertTrue("Form b checkbox state must reflect that of form a", chkbox_b.is(':checked'));
    chkbox_b.prop('checked', false);
    chkbox_b.change();
    assertFalse("Form b checkbox state must stick", chkbox_b.is(':checked'));
    assertFalse("Form a checkbox state must reflect that of form b", chkbox_a.is(':checked'));
  });

  it("test_replicates_text_field_value_after_registration", function() {
    assertEquals("Form b field must start out with given value", "foo bar", text_b.val());
    replicator.register(text_b[0], 'text');
    text_a.val("baz quux");
    text_a.change();
    assertEquals("Form a textfield state must stick", "baz quux", text_a.val());
    assertEquals("Form b textfield state must reflect that of form a", "baz quux", text_b.val());
    text_b.val("bar baz");
    text_b.change();
    assertEquals("Form b textfield state must sick", "bar baz", text_b.val());
    assertEquals("Form a textfield state must reflect that of form b", "bar baz", text_a.val());
  });

  it("test_replicates_checkbox_value_on_registration", function() {
    chkbox_a.prop('checked', true);
    assertTrue("Form a checkbox must start out checked", chkbox_a.is(':checked'));
    assertFalse("Form b checkbox must start out unchecked", chkbox_b.is(':checked'));
    replicator.register(chkbox_b[0], 'chkbox');
    assertTrue("Form a checkbox state change must stick", chkbox_a.is(':checked'));
    assertTrue("Form b checkbox state must reflect that of form a", chkbox_b.is(':checked'));
  });

  it("test_replicates_text_field_value_on_registration", function() {
    text_a.val("baz quux");
    assertEquals("Form a textfield must start out with given value", "baz quux", text_a.val());
    assertEquals("Form b field must start out with given default", "foo bar", text_b.val());
    replicator.register(text_b[0], 'text');
    assertEquals("Form a textfield state must stick", "baz quux", text_a.val());
    assertEquals("Form b textfield state must reflect that of form a", "baz quux", text_b.val());
  });

  it("test_stops_replicating_checkbox_value_after_unregistration", function() {
    replicator.register(chkbox_b[0], 'chkbox');
    replicator.unregister(chkbox_b[0], 'chkbox');
    chkbox_a.prop('checked', true);
    chkbox_a.change();
    assertTrue("Form a checkbox state must stick", chkbox_a.is(':checked'));
    assertFalse("Form b checkbox state must remain unaltered", chkbox_b.is(':checked'));
    chkbox_a.prop('checked', false);
    chkbox_b.prop('checked', true);
    chkbox_b.change();
    assertFalse("Form a checkbox state must remain unaleterd", chkbox_a.is(':checked'));
    assertTrue("Form b checkbox state must stick", chkbox_b.is(':checked'));
  });

  it("test_stops_replicating_text_field_value_after_registration", function() {
    replicator.register(text_b[0], 'text');
    replicator.unregister(text_b[0], 'text');
    text_b.val("foo bar");
    text_a.val("hello world");
    text_a.change();
    assertEquals("Form a field state must stick", "hello world", text_a.val());
    assertEquals("Form b field state must remain unaltered", "foo bar", text_b.val());
    text_b.val("bar baz");
    text_b.change();
    assertEquals("Form a field state must remain unaleterd", "hello world", text_a.val());
    assertEquals("Form b field state must stick", "bar baz", text_b.val());
  });
});
