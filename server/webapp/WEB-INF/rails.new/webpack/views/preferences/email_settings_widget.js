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

  const m  = require("mithril"),
    Stream = require("mithril/stream"),
    _      = require("lodash"),
    f      = require("helpers/form_helper");

  function EmailSettingsFormEditableState(model, smtpEnabled) {
    const readonly = Stream(true),
      rejectToggle = readonly.map((value) => !smtpEnabled || value);

    function enterEditMode(e) {
      if (e) {
        e.preventDefault();
      }

      readonly(false);
      model.reset();
    }

    function exitEditMode(e) {
      if (e) {
        e.preventDefault();
      }

      readonly(true);
      model.reset();
    }

    this.readonly      = readonly;
    this.rejectToggle  = rejectToggle;
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
      const model     = vnode.attrs.model,
        readonly      = this.editableState.readonly(),
        rejectToggle  = this.editableState.rejectToggle(),
        enterEditMode = this.editableState.enterEditMode,
        exitEditMode  = this.editableState.exitEditMode;

      return m("form", model.config({class: "email-settings"}, exitEditMode),
        m("legend", "Email Settings"),
        m("fieldset", [
          m(LockableInput, {name: "email", label: "Email", type: "email", model, attrName: "email", readonly, placeholder: "Email not set"}),
          m(f.checkbox, {name: "email_me", label: "Enable email notification", model, attrName: "enableNotifications", disabled: rejectToggle}),
          m(LockableInput, {name: "checkin_aliases", label: "My check-in aliases", model, attrName: "checkinAliases", readonly, placeholder: "No matchers defined"}),
        ]),
        m("fieldset",
          readonly ? m("input", {type: "button", value: "Edit", onclick: enterEditMode}) : [
            m("input", {type: "submit", value: "Save", class: "primary"}),
            m("input", {type: "reset", value: "Cancel", onclick: exitEditMode})
          ]
        )
      );
    }
  };

  const LockableInput = {
    view(vnode) {
      const args = _.assign({}, vnode.attrs),
        value    = args.model[args.attrName],
        readonly = args.readonly;

      args.autocomplete = "on"; // it's nice to remember past entries for things like "email"

      return readonly ? m("label",
        m("span", args.label),
        m("span", {class: "value"}, value() || args.placeholder)
      ) : m(f.input, args);
    }
  };

  module.exports = EmailSettingsWidget;
})();
