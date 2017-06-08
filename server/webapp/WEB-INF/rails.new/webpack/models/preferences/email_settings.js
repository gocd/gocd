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

  const m      = require("mithril"),
    Stream     = require("mithril/stream"),
    _          = require("lodash"),
    CrudMixins = require("models/mixins/crud_mixins"),
    Errors     = require("models/mixins/errors");

  function splitter(string) {
    return _.compact(_.map(string.split(","), _.trim));
  }

  function EmailSettings(resourceUrl, errors) {
    this.email               = Stream();
    this.enableNotifications = Stream();
    this.checkinAliases      = Stream();
    this.etag                = Stream();

    let savedEmail, savedEnableNotifications, savedCheckinAliases;

    const payload = {
      email:          this.email,
      emailMe:        this.enableNotifications,
      checkinAliases: this.checkinAliases.map(splitter)
    };

    this.toJSON = function serialize() { return payload; };
    this.fromJSON = (data) => {
      savedEmail               = this.email(data.email);
      savedEnableNotifications = this.enableNotifications(data.email_me);
      savedCheckinAliases      = this.checkinAliases(data.checkin_aliases.join(", "));

      return this;
    };

    this.load = () => {
      this.refresh().always(() => m.redraw());
    };

    this.reset = () => {
      this.email(savedEmail);
      this.enableNotifications(savedEnableNotifications);
      this.checkinAliases(savedCheckinAliases);
    };

    this.config = (attrs, updateCallback) => {
      return _.assign(attrs, {action: resourceUrl, method: "PATCH", onsubmit: (e) => {
        e.preventDefault();
        errors(null);

        this.update().then((data) => {
          updateCallback(e, data);
        }, (message) => {
          errors(message.replace(/^Failed to add user\. Validations failed\. /, "")); // Strip the localized message orefix
        }).always(() => m.redraw());
      }});
    };

    // just stub this out; the only thing that needs validation is email, and we rely on the default
    // input[type="email"] pattern for client-side validation
    this.validate = () => new Errors();

    CrudMixins.AllOperations.call(this, ["refresh", "update"], {type: this, resourceUrl, version: "v1"}, {update: {method: "PATCH"}});
  }

  module.exports = EmailSettings;
})();
