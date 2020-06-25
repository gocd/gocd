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

import {NotificationEvent, NotificationFilter} from "../notification_filters";

describe('NotificationFilterSpec', () => {
  it('should validate presence of pipeline', () => {
    const filter = new NotificationFilter(1, "", "stage", NotificationEvent.All, false);

    expect(filter.isValid()).toBeFalse();
    expect(filter.errors().count()).toBe(1);
    expect(filter.errors().errorsForDisplay('pipeline')).toBe('Pipeline must be present.');
  });

  it('should validate presence of stage', () => {
    const filter = new NotificationFilter(1, "pipeline", "", NotificationEvent.All, false);

    expect(filter.isValid()).toBeFalse();
    expect(filter.errors().count()).toBe(1);
    expect(filter.errors().errorsForDisplay('stage')).toBe('Stage must be present.');
  });
});
