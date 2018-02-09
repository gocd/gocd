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
describe("Analytics Dashboard Header", () => {
  const m      = require('mithril');
  require('jasmine-jquery');

  const AnalyticsDashboardHeader = require('views/analytics/header');

  let $root, root;
  beforeEach(() => {
    [$root, root] = window.createDomElementForTest();
  });
  afterEach(window.destroyDomElementForTest);

  afterEach(() => {
    unmount();
  });

  it('should have show a big header', () => {
    mount();
    expect($root.find(".header-panel h1")).toHaveText('Analytics');
  });

  const mount = () => {
    m.mount(root, {
      view() {
        return m(AnalyticsDashboardHeader, {});
      }
    });
    m.redraw();
  };

  const unmount = () => {
    m.mount(root, null);
    m.redraw();
  };
});
