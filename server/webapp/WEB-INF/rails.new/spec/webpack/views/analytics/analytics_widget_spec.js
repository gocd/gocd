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
describe("Analytics Widget", () => {
  const m = require('mithril');
  require('jasmine-jquery');

  const AnalyticsWidget = require('views/analytics/analytics_widget');

  let $root, root;

  beforeEach(() => {
    jasmine.Ajax.install();
    [$root, root] = window.createDomElementForTest();
    mount();
  });

  afterEach(() => {
    jasmine.Ajax.uninstall();
    unmount();
    window.destroyDomElementForTest();
  });

  it('should render analytics header', () => {
    expect($root.find('.header-panel')).toBeInDOM();
    expect($root.find('.header-panel')).toContainText("Analytics");
  });

  it('should render global tab', () => {
    expect($root.find('.dashboard-tabs li').get(0)).toContainText("Global");
  });

  it('should render pipelines tab', () => {
    expect($root.find('.dashboard-tabs li').get(1)).toContainText("Pipeline");
  });

  it('should render global chart contents when global tab is selected', () => {
    expect($root.find('.dashboard-tabs li').get(0)).toContainText("Global");
    expect($root.find('.dashboard-tabs li').get(0)).toHaveClass("current");
    expect($root.find('.global')).toBeInDOM();
  });

  it('should render global chart contents when global tab is selected', () => {
    $root.find('.dashboard-tabs li').get(1).click();
    m.redraw();

    expect($root.find('.dashboard-tabs li').get(1)).toContainText("Pipeline");
    expect($root.find('.dashboard-tabs li').get(1)).toHaveClass("current");
    expect($root.find('.pipeline')).toBeInDOM();
  });

  const mount = () => {
    m.mount(root, {
      view() {
        return <AnalyticsWidget metrics={{}} pipelines={[]} plugins={[]}/>;
      }
    });
    m.redraw();
  };

  const unmount = () => {
    m.mount(root, null);
    m.redraw();
  };
});
