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
describe("Analytics Dashboard Tabs Model", () => {

  const Tabs = require('models/analytics/tabs');
  const Tab = require('models/analytics/tab');

  const tabs = new Tabs(() => {});
  const m1 = new Tab("x", null, []);
  const m2 = new Tab("y", null, []);

  it('should use the first tab as the default active', () => {
    tabs.push(m1);
    tabs.push(m2);

    expect(tabs.current()).toBe(m1);

  });

  it('should change active tab', () => {
    tabs.push(m1);
    tabs.push(m2);

    expect(tabs.current()).toBe(m1);
    tabs.setActiveTab(m2);
    expect(tabs.current()).toBe(m2);
  });
});
