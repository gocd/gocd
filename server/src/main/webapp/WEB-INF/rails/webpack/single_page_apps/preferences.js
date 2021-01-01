/*
 * Copyright 2021 ThoughtWorks, Inc.
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
import m from "mithril";
import Stream from "mithril/stream";
import {f} from "helpers/form_helper";
import {EmailSettingsWidget} from "views/preferences/email_settings_widget";
import {NotificationFiltersListWidget} from "views/preferences/notification_filters_list_widget";
import {AddNotificationFilterWidget} from "views/preferences/add_notification_filter_widget";
import {EmailSettings} from "models/preferences/email_settings";
import {NotificationFilters} from "models/preferences/notification_filters";
import {Pipelines} from "models/preferences/pipelines";

;(function () { // eslint-disable-line no-extra-semi
  "use strict";

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
  });

})();
