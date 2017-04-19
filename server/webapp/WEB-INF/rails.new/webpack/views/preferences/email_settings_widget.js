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
  const _ = require("lodash");

  function yank(object, key, defaultValue) {
    let value = object[key];

    if ("undefined" === typeof value) {
      value = defaultValue;
    }

    delete object[key];
    return value;
  }

  function EmailSettingsFormEditableState(model, smtpEnabled) {
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

    this.editMode      = editMode;
    this.allowCheckbox = allowCheckbox;
    this.enterEditMode = enterEditMode;
    this.exitEditMode  = exitEditMode;
  }

  const EmailSettingsWidget = {
    oninit(vnode) {
      const model = vnode.attrs.model;
      this.editableState = new EmailSettingsFormEditableState(model, vnode.attrs.smtpEnabled);
      model.load();
    },

    view(vnode) {
      const model      = vnode.attrs.model,
        editMode       = this.editableState.editMode,
        allowCheckbox  = this.editableState.allowCheckbox,
        enterEditMode  = this.editableState.enterEditMode,
        exitEditMode   = this.editableState.exitEditMode;

      return m("form", model.config({class: "email-settings"}, exitEditMode),
        m("legend", "Email Settings"),
        m("fieldset", [
          m(LockableInput, {name: "email", label: "Email", type: "email", value: model.email, unlocked: editMode, placeholder: "Email not set"}),
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

  const LockableCheckbox = {
    view(vnode) {
      const args     = _.assign({}, vnode.attrs);
      const label    = yank(args, "label");
      const value    = yank(args, "value");
      const unlocked = yank(args, "unlocked");
      const attrs    = _.assign({type: "checkbox", disabled: !unlocked(), checked: value(), onchange: m.withAttr("checked", value)}, args);

      return m("label",
        m("input", attrs),
        m("span", label)
      );
    }
  };

  const LockableInput = {
    view(vnode) {
      const args        = _.assign({}, vnode.attrs);
      const label       = yank(args, "label");
      const value       = yank(args, "value");
      const placeholder = yank(args, "placeholder");
      const unlocked    = yank(args, "unlocked");

      const dontBeAnnoying = {
        autocapitalize: "off",
        autocorrect: "off",
        spellcheck: false,
      };

      if (unlocked()) {
        const attrs = _.assign({type: "text", value: value(), oninput: m.withAttr("value", value)}, dontBeAnnoying, args);
        return m("label",
          m("span", label),
          m("input", attrs)
        );
      }

      return m("label",
        m("span", label),
        m("span", {class: "value"}, value() || placeholder)
      );
    }
  };

  module.exports = EmailSettingsWidget;
})();
