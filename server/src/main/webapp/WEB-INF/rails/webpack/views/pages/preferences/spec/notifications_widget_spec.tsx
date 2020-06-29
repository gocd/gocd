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
import {NotificationFilter, NotificationFilters} from "models/new_preferences/notification_filters";
import {NotificationFilterVMs} from "models/new_preferences/preferences";
import {TestHelper} from "views/pages/spec/test_helper";
import {NotificationsWidget} from "../notifications_widget";

describe('NotificationsWidgetSpec', () => {
  const helper      = new TestHelper();
  const onAddSpy    = jasmine.createSpy('onAdd');
  const onEditSpy   = jasmine.createSpy('onEdit');
  const onDeleteSpy = jasmine.createSpy('onDelete');
  let notifications: NotificationFilterVMs;
  let groups: PipelineGroups;

  beforeEach(() => {
    notifications = new NotificationFilterVMs(new NotificationFilters(NotificationFilter.default()));
    groups        = PipelineGroups.fromJSON(data.pipeline_groups_json().groups);
  });
  afterEach((done) => helper.unmount(done));

  it('should render notifications and add button', () => {
    mount();

    expect(helper.byTestId('notification-filter-add')).toBeInDOM();
    expect(helper.q('thead').textContent).toBe('PipelineStageEventCheck-ins Matcher');
    expect(helper.qa('tbody tr')[0].textContent).toBe('[Any Pipeline][Any Stage]AllMine');
  });

  it('should sent a call to the callbacks', () => {
    mount();

    helper.clickByTestId('notification-filter-add');
    expect(onAddSpy).toHaveBeenCalled();

    helper.clickByTestId('notification-filter-edit');
    expect(onEditSpy).toHaveBeenCalled();

    helper.clickByTestId('notification-filter-delete');
    expect(onDeleteSpy).toHaveBeenCalled();
  });

  function mount() {
    helper.mount(() => <NotificationsWidget notificationVMs={Stream(notifications)}
                                            pipelineGroups={Stream(groups)}
                                            onAddFilter={onAddSpy}
                                            onEditFilter={onEditSpy}
                                            onDeleteFilter={onDeleteSpy}/>);
  }
});
