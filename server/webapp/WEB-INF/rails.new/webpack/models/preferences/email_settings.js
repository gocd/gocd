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
  const CrudMixins = require("models/mixins/crud_mixins");

  const API_VERSION = "v1";

  function trim(string) {
    return string.replace(/^\s+|\s+$/g, "");
  }

  function splitter(string) {
    return _.compact(_.map(string.split(","), trim));
  }

  function EmailSettings(apiUrl, errors) {
    const email = Stream();
    const enableNotifications = Stream();
    const checkinAliases = Stream();

    const etag = Stream(); // make CrudMixins happy

    const payload = { // keys will be automatically snake-cased by CrudMixins
      email,
      emailMe: enableNotifications,
      checkinAliases: checkinAliases.map(splitter)
    };

    CrudMixins.Refresh.call(this, {
      resourceUrl: apiUrl,
      version:     API_VERSION,
      type:        {
        fromJSON: (data) => { return _.assign(data, {etag}); }
      }
    });

    CrudMixins.Update.call(this, {
      resourceUrl: apiUrl,
      version:     API_VERSION,
      type:        {
        fromJSON: (data) => { return _.assign(data, {etag}); }
      },
      method:      "PATCH"
    });

    const fetchUser  = this.refresh.bind(this);
    const updateUser = this.update.bind(this);

    let savedEmail;
    let savedEnableNotifications;
    let savedCheckinAliases;

    function toJSON() {
      return payload;
    }

    function load() {
      fetchUser().then(updateUserBindings).always(() => m.redraw());
    }

    function reset() {
      email(savedEmail);
      enableNotifications(savedEnableNotifications);
      checkinAliases(savedCheckinAliases);
    }

    function updateUserBindings(data) {
      savedEmail = email(data.email);
      savedEnableNotifications = enableNotifications(data.email_me);
      savedCheckinAliases = checkinAliases(data.checkin_aliases.join(", "));
    }

    function config(attrs, updateCallback) {
      return _.assign(attrs, {
        action:   apiUrl,
        method:   "PATCH",
        onsubmit: function persistSettings(e) {
          e.preventDefault();
          errors(null);

          updateUser().then((data) => {
            updateUserBindings(data);
            if ("function" === typeof updateCallback) {
              updateCallback(e, data);
            }
          }, (message) => {
            // Strip the localized message orefix
            errors(message.replace(/^Failed to add user\. Validations failed\. /, ""));
          }).always(() => m.redraw());
        }
      });
    }

    _.assign(this, {
      load,
      reset,
      config,
      email,
      enableNotifications,
      checkinAliases,
      etag,
      toJSON
    });
  }

  module.exports = EmailSettings;
})();
