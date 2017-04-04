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
;(function () {
  "use strict";

  const m = require("mithril");
  const Stream = require("mithril/stream");

  const EmailSettingsView = require('views/preferences/email_settings');
  const FiltersListView   = require('views/preferences/filters_list');

  const EmailSettingsModel = require('models/preferences/email_settings_model');
  const FiltersModel       = require('models/preferences/filters_model');

  $(function ready() {

    function dataAttr(node, name) {
      return node.getAttribute("data-" + name);
    }

    const main        = document.getElementById("notification-prefs");
    const validations = document.getElementById("validations");

    const userUrl     = dataAttr(main, "user-url");
    const filtersUrl  = dataAttr(main, "filters-url");

    const errorsModel = Stream();
    const emailSettingsModel = new EmailSettingsModel(userUrl, errorsModel);
    const filtersModel       = new FiltersModel(filtersUrl, errorsModel);

    const EmailSettings = new EmailSettingsView(emailSettingsModel);
    const Filters       = new FiltersListView(filtersModel);

    const ErrorMessage = {
      oninit(vnode) {
        vnode.state.errors = vnode.attrs.errors;
      },

      view(vnode) {
        if (vnode.state.errors()) {
          return m("div", {class: "error"}, m("i", {class: "fa fa-exclamation-circle"}), vnode.state.errors());
        }
      }
    };

    m.mount(validations, {
      view() {
        return m(ErrorMessage, {errors: errorsModel});
      }
    });

    m.mount(main, {
      view() {
        return [
          m(EmailSettings),
          m("div", {class: "filter-controls"},
            m("h2", "Current Notification Filters"),
            m(Filters)
          )
        ];
      }
    });
  });

})();
