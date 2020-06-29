/*
 * Copyright 2020 ThoughtWorks, Inc.
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
import {PipelineGroups} from "models/internal_pipeline_structure/pipeline_structure";
import data from "models/new-environments/spec/test_data";
import {CurrentUser} from "models/new_preferences/current_user";
import {NotificationFilter, NotificationFilters} from "models/new_preferences/notification_filters";
import {CurrentUserVM, NotificationFilterVMs} from "models/new_preferences/preferences";
import {TestHelper} from "views/pages/spec/test_helper";
import {PreferencesWidget, Sections} from "../preferences_widget";
import {userJSON} from "./email_settings_widget_spec";

describe('PreferencesWidgetSpec', () => {
  const helper = new TestHelper();
  const route  = jasmine.createSpy('route');
  let notifications: NotificationFilterVMs;
  let groups: PipelineGroups;
  let user: CurrentUserVM;

  beforeEach(() => {
    notifications = new NotificationFilterVMs(new NotificationFilters(NotificationFilter.default()));
    groups        = PipelineGroups.fromJSON(data.pipeline_groups_json().groups);
    user          = new CurrentUserVM(CurrentUser.fromJSON(userJSON()));
  });
  afterEach((done) => helper.unmount(done));

  it('should have links for email settings and notifications', () => {
    mount(Sections.MY_NOTIFICATIONS);

    expect(helper.byTestId("my-notifications")).toBeInDOM();
    expect(helper.byTestId("email-settings")).toBeInDOM();
  });

  it("should render my notifications widget", () => {
    mount(Sections.MY_NOTIFICATIONS);

    expect(helper.byTestId("notifications-widget")).toBeInDOM();
  });

  it("should render email settings widget", () => {
    mount(Sections.EMAIL_SETTINGS);

    expect(helper.byTestId("email-settings-widget")).toBeInDOM();
  });

  it("should render notifications on click of notification link", () => {
    mount(Sections.EMAIL_SETTINGS);

    helper.clickByTestId("my-notifications");

    expect(route).toHaveBeenCalledWith(Sections.MY_NOTIFICATIONS, user);
  });

  it("should render email settings widget on click of email settings link", () => {
    mount(Sections.MY_NOTIFICATIONS);

    helper.clickByTestId("email-settings");

    expect(route).toHaveBeenCalledWith(Sections.EMAIL_SETTINGS, notifications);
  });

  function mount(activeConfiguration: Sections) {
    helper.mount(() => <PreferencesWidget onFilterSave={jasmine.createSpy('onSave')}
                                          onAddFilter={jasmine.createSpy('onAdd')}
                                          onEditFilter={jasmine.createSpy('onEdit')}
                                          onDeleteFilter={jasmine.createSpy('onDelete')}
                                          onCancel={jasmine.createSpy('onCancel')}
                                          onSaveEmailSettings={jasmine.createSpy('onEmailSave')}
                                          route={route}
                                          activeConfiguration={activeConfiguration}
                                          notificationVMs={Stream(notifications)}
                                          pipelineGroups={Stream(groups)}
                                          currentUserVM={Stream(user)}/>);
  }
});
