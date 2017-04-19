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
    $      = require("jquery"),
    mr     = require("helpers/mrequest");

  function splitter(string) {
    return _.compact(_.map(string.split(","), $.trim));
  }

  const API_VERSION = "v1";

  function EmailSettings(apiUrl, errors) {
    const email           = Stream(),
      enableNotifications = Stream(),
      checkinAliases      = Stream();

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

    function load() {
      $.ajax({
        method:   "GET",
        url:      apiUrl,
        dataType: "json",
        headers:  {
          Accept: mr.versionHeader(API_VERSION)
        }
      }).done(updateUserBindings).always(() => m.redraw());
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
      return _.assign(attrs, {action: apiUrl, method: "PATCH", onsubmit: function save(e) {
        e.preventDefault();
        errors(null);

        $.ajax({
          method:   "PATCH",
          url:      apiUrl,
          dataType: "json",
          headers:  {
            "Accept":       mr.versionHeader(API_VERSION),
            "Content-Type": "application/json"
          },
          data:     JSON.stringify(payload)
        }).done((data) => {
          updateUserBindings(data);

          if ("function" === typeof updateCallback) {
            updateCallback(e, data);
          }
        }).fail(({responseJSON}) => {
          errors(responseJSON.message.replace(/^Failed to add user\. Validations failed\. /, "")); // Strip the localized message orefix
        }).always(() => m.redraw());
      }});
    }

    return {
      load,
      reset,
      config,
      get email() { return email; },
      get enableNotifications() { return enableNotifications; },
      get checkinAliases() { return checkinAliases; }
    };
  }

  module.exports = EmailSettings;
})();
