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

  function EmailSettings(model) {
    const editMode = Stream(false);

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
            m(LockableCheckbox, {name: "email_me", label: "Enable notifications", value: model.enableNotifications, unlocked: editMode}),
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
    oninit(vnode) {
      vnode.state.name = vnode.attrs.name;
      vnode.state.value = vnode.attrs.value;
      vnode.state.label = vnode.attrs.label;
      vnode.state.unlocked = vnode.attrs.unlocked;
    },

    view(vnode) {
      return m("label",
        m("input", {name: vnode.state.name, type: "checkbox", disabled: !vnode.state.unlocked(), checked: vnode.state.value(), onchange: m.withAttr("checked", vnode.state.value)}),
        m("span", vnode.state.label)
      );
    }
  };

  const LockableInput = {
    oninit(vnode) {
      vnode.state.name = vnode.attrs.name;
      vnode.state.value = vnode.attrs.value;
      vnode.state.label = vnode.attrs.label;
      vnode.state.placeholder = vnode.attrs.placeholder;
      vnode.state.unlocked = vnode.attrs.unlocked;
    },

    view(vnode) {
      if (vnode.state.unlocked()) {
        return m("label",
          m("span", vnode.state.label),
          m("input", {name: vnode.state.name, type: "text", value: vnode.state.value(), oninput: m.withAttr("value", vnode.state.value)})
        );
      }

      return m("label",
        m("span", vnode.state.label),
        m("span", {class: "value"}, vnode.state.value() || vnode.state.placeholder)
      );
    }
  };

  module.exports = EmailSettings;
})();
