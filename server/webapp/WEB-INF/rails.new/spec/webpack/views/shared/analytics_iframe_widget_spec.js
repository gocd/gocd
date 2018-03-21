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
describe("Analytics iFrame Widget", () => {
  const m      = require('mithril');
  const Stream = require('mithril/stream');
  require('jasmine-jquery');

  const AnalyticsiFrameWidget = require('views/shared/analytics_iframe_widget');

  function newModel(loadedData, loadedView) {
    const data = Stream(),
          view = Stream(),
          url  = Stream(),
        errors = Stream();

    return {url, data, view, errors, load: () => {
      data(loadedData);
      view(loadedView);
    }};
  }

  const noop = () => {};

  let $root, root;

  beforeEach(() => {
    [$root, root] = window.createDomElementForTest();
  });

  afterEach(() => {
    unmount();
    window.destroyDomElementForTest();
  });

  it('should load model oncreate', () => {
    let called = false;
    const model = newModel(null, null);
    model.load = () => { called = true; };

    mount(model, noop);
    expect(called).toBe(true);
  });

  it('should load view path from model and create an iframe with sandbox', () => {
    mount(newModel({a: 1}, "/some/path"), noop);
    const iframe = $root.find('iframe');

    expect(iframe.attr('sandbox')).toEqual('allow-scripts');
    expect(iframe.attr('src')).toEqual('/some/path');
  });

  it('should show errors if any', () => {
    const model = newModel(null, null);
    model.errors("here is an error");
    mount(model, noop);
    expect($root.find(".frame-container")[0].getAttribute("data-error-text")).toBe("here is an error");

  });

  it('should initialize iframe oncreate', () => {
    let actualMessage;

    mount(newModel("model data", null), (_win, data) => {
      actualMessage = JSON.stringify(data);
    });

    const iframe = $root.find('iframe')[0];
    iframe.onload();

    const expectedMessage = JSON.stringify({
      uid: "some-uid",
      pluginId: "some-plugin",
      initialData: "model data"
    });
    expect(actualMessage).toBe(expectedMessage);
  });

  const mount = (model, init) => {
    m.mount(root,
      {
        view() {
          return m(AnalyticsiFrameWidget, {model, pluginId: "some-plugin", uid: "some-uid", init});
        }
      }
    );
    m.redraw();
  };

  const unmount = () => {
    m.mount(root, null);
    m.redraw();
  };
});
