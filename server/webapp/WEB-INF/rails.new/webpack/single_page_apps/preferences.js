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
;(function () { // eslint-disable-line no-extra-semi
  "use strict";

  const m      = require("mithril");
  const Stream = require("mithril/stream");
  const $      = require("jquery");

  const f                             = require("helpers/form_helper");
  const EmailSettingsWidget           = require("views/preferences/email_settings_widget");
  const NotificationFiltersListWidget = require("views/preferences/notification_filters_list_widget");
  const AddNotificationFilterWidget   = require("views/preferences/add_notification_filter_widget");

  const EmailSettings       = require("models/preferences/email_settings");
  const NotificationFilters = require("models/preferences/notification_filters");
  const Pipelines           = require("models/preferences/pipelines");
  const VersionUpdater      = require('models/shared/version_updater');

  function dataAttr(node, name) {
    return node.getAttribute(`data-${name}`);
  }

  document.addEventListener("DOMContentLoaded", () => {

    const main        = document.getElementById("notification-prefs");
    const validations = document.getElementById("validations");
    const pipelines   = JSON.parse(dataAttr(main, "pipelines"));

    const userUrl     = dataAttr(main, "user-url");
    const filtersUrl  = dataAttr(main, "filters-url");
    const smtpEnabled = JSON.parse(dataAttr(main, "smtp-configured"));

    const errorsModel        = Stream();
    const emailSettingsModel = new EmailSettings(userUrl, errorsModel);
    const filtersModel       = new NotificationFilters(filtersUrl, errorsModel);
    const pipelinesModel     = new Pipelines(pipelines);

    const ErrorMessageWidget = {
      view(vnode) {
        if (vnode.attrs.errors()) {
          return m(f.alert, vnode.attrs.errors());
        }
      }
    };

    m.mount(validations, {
      view() {
        return m(ErrorMessageWidget, {errors: errorsModel});
      }
    });

    m.mount(main, {
      view() {
        return [
          m(EmailSettingsWidget, {model: emailSettingsModel, smtpEnabled}),
          m("div", {class: "filter-controls"},
            m("h2", "Create Notification Filter"),
            m(AddNotificationFilterWidget, {filtersModel, pipelinesModel}),
            m("h2", "Current Notification Filters"),
            m(NotificationFiltersListWidget, {model: filtersModel})
          )
        ];
      }
    });

    // boilerplate to init menus and check for updates
    $(document).foundation();
    new VersionUpdater().update();
  });

})();
