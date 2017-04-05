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

  function EmailSettingsModel(apiUrl, errors) {
    const email = Stream();
    const enableNotifications = Stream();
    const checkinAliases = Stream();

    function trim(string) {
      return string.replace(/^\s+|\s+$/g, "");
    }

    function splitter(string) {
      return _.compact(_.map(string.split(","), trim));
    }

    /* eslint-disable camelcase, object-shorthand */
    const payload = {
      email:           email,
      email_me:        enableNotifications,
      checkin_aliases: checkinAliases.map(splitter)
    };
    /* eslint-enable camelcase, object-shorthand */

    let savedEmail;
    let savedEnableNotifications;
    let savedCheckinAliases;

    function fetchUser() {
      m.request({
        method:  "GET",
        url:     apiUrl,
        type:    "json",
        headers: {
          Accept: "application/vnd.go.cd.v1+json"
        }
      }).then((data) => {
        updateUserBindings(data);
      });
    }

    function updateUser(callback) {
      return function onUpdate(e) {
        e.preventDefault();
        errors(null);

        m.request({
          method:  "PATCH",
          url:     apiUrl,
          type:    "json",
          headers: {
            "Accept":       "application/vnd.go.cd.v1+json",
            "Content-Type": "application/json"
          },
          data: payload
        }).then((data) => {
          updateUserBindings(data);
          callback();
        }, (error) => {
          // Strip the localized message orefix
          errors(error.message.replace(/^Failed to add user\. Validations failed\. /, ""));
        });
      };
    }

    function resetUserFields() {
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
      return _.assign(attrs, {action: apiUrl, method: "PATCH", onsubmit: updateUser(updateCallback)});
    }

    return {
      load: fetchUser,
      reset: resetUserFields,
      update: updateUser,
      config: config, // eslint-disable-line object-shorthand
      get email() { return email; },
      get enableNotifications() { return enableNotifications; },
      get checkinAliases() { return checkinAliases; }
    };
  }

  module.exports = EmailSettingsModel;
})();
