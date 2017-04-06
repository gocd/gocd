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

;(function() { // eslint-disable-line no-extra-semi
  "use strict";

  const m = require("mithril");
  const Stream = require("mithril/stream");

  function EmailSettingsWidget(model, smtpEnabled=false) {
    const editMode = Stream(false);
    const allowCheckbox = editMode.map((value) => smtpEnabled && value);

    function enterEditMode(e) {
      if (e) {
        e.preventDefault();
      }

      editMode(true);
      model.reset();
    }

    function exitEditMode(e) {
      if (e) {
        e.preventDefault();
      }

      editMode(false);
      model.reset();
    }

    return {
      oninit() {
        model.load();
      },

      view() {
        return m("form", model.config({class: "email-settings"}, exitEditMode),
          m("legend", "Email Settings"),
          m("fieldset", [
            m(LockableInput, {name: "email", label: "Email", value: model.email, unlocked: editMode, placeholder: "Email not set"}),
            m(LockableCheckbox, {name: "email_me", label: "Enable email notification", value: model.enableNotifications, unlocked: allowCheckbox}),
            m(LockableInput, {name: "checkin_aliases", label: "My check-in aliases", value: model.checkinAliases, unlocked: editMode, placeholder: "No matchers defined"}),
          ]),
          m("fieldset",
            editMode() ? [
              m("input", {type: "submit", value: "Save", class: "primary"}),
              m("input", {type: "reset", value: "Cancel", onclick: exitEditMode})
            ] : m("input", {type: "button", value: "Edit", onclick: enterEditMode})
          )
        );
      }
    };
  }

  const LockableCheckbox = {
    view(vnode) {
      return m("label",
        m("input", {name: vnode.attrs.name, type: "checkbox", disabled: !vnode.attrs.unlocked(), checked: vnode.attrs.value(), onchange: m.withAttr("checked", vnode.attrs.value)}),
        m("span", vnode.attrs.label)
      );
    }
  };

  const LockableInput = {
    view(vnode) {
      if (vnode.attrs.unlocked()) {
        return m("label",
          m("span", vnode.attrs.label),
          m("input", {name: vnode.attrs.name, type: "text", value: vnode.attrs.value(), oninput: m.withAttr("value", vnode.attrs.value)})
        );
      }

      return m("label",
        m("span", vnode.attrs.label),
        m("span", {class: "value"}, vnode.attrs.value() || vnode.attrs.placeholder)
      );
    }
  };

  module.exports = EmailSettingsWidget;
})();
