/*
 * Copyright 2018 ThoughtWorks, Inc.
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

const $               = require('jquery');
const m               = require('mithril');
const DashboardVM     = require('views/dashboard/models/dashboard_view_model');
const Dashboard       = require('models/dashboard/dashboard');
const DashboardWidget = require('views/dashboard/dashboard_widget');

require('foundation-sites');

const dashboardViewModel = new DashboardVM();

$(() => {
  const dashboardElem          = $('#dashboard');
  const isQuickEditPageEnabled = JSON.parse(dashboardElem.attr('data-is-quick-edit-page-enabled'));
  $(document).foundation();

  const onSuccess = (dashboard) => {
    const component = {
      view() {
        return m(DashboardWidget, {
          dashboard,
          isQuickEditPageEnabled,
          vm: dashboardViewModel
        });
      }
    };

    m.mount($("#dashboard").get(0), component);
  };

  Dashboard.get().then(onSuccess);
});
