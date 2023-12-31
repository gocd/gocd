/*
 * Copyright 2024 Thoughtworks, Inc.
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
class TimerObserver {
  timers = [];

  constructor(name) {
    this.name = name;
  }

  notify(jsonArray) {
    for (let i = 0; i < jsonArray.length; i++) {
      if (!jsonArray[i]) return;
      if (this.name && this.name != jsonArray[i].building_info.name) {
        continue;
      }
      // relies on trimpath-template via String.prototype.process hack
      $('#build-detail-summary').html($('#build-summary-template').val().process({build:jsonArray[i].building_info}));
    }
  }
}
