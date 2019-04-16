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
import {TestHelper} from "views/pages/spec/test_helper";

describe("Analytics iFrame Widget", () => {
  const m      = require('mithril');
  const Stream = require('mithril/stream');
  require('jasmine-jquery');

  const AnalyticsiFrameWidget = require('views/shared/analytics_iframe_widget');

  const helper = new TestHelper();

  function newModel(loadedData, loadedView) {
    const data   = Stream(),
          view   = Stream(),
          url    = Stream(),
          errors = Stream();

    return {
      url, data, view, errors, load: () => {
        data(loadedData);
        view(loadedView);
      }
    };
  }

  const noop = () => {
  };

  afterEach(helper.unmount.bind(helper));

  it('should load model oncreate', () => {
    let called  = false;
    const model = newModel(null, null);
    model.load  = () => {
      called = true;
    };

    mount(model, noop);
    expect(called).toBe(true);
  });

  it('should load view path from model and create an iframe with sandbox', () => {
    mount(newModel({a: 1}, "/some/path"), noop);
    const iframe = helper.find('iframe');

    expect(iframe.attr('sandbox')).toEqual('allow-scripts');
    expect(iframe.attr('src')).toEqual('/some/path');
  });

  it('should show errors if any', () => {
    const model = newModel(null, null);
    model.errors({
      getResponseHeader: () => "text/plainText",
      responseText:      "here is an error"
    });
    mount(model, noop);
    expect(helper.find(".frame-container")[0].getAttribute("data-error-text")).toBe("here is an error");
  });

  it('should render html error if any', () => {
    const model           = newModel(null, null);
    const htmlErrorString = "<html><body>Boom!</body></html>";
    model.errors({
      getResponseHeader: () => "text/html",
      responseText:      htmlErrorString
    });
    mount(model, noop);
    expect(helper.find("iframe")[0].getAttribute("src")).toBe(`data:text/html;charset=utf-8,${htmlErrorString}`);
  });

  it('should initialize iframe oncreate', () => {
    let actualMessage;

    mount(newModel("model data", null), (_win, data) => {
      actualMessage = JSON.stringify(data);
    });

    const iframe = helper.find('iframe')[0];
    iframe.onload();

    const expectedMessage = JSON.stringify({
      uid:         "some-uid",
      pluginId:    "some-plugin",
      initialData: "model data"
    });
    expect(actualMessage).toBe(expectedMessage);
  });

  function mount(model, init) {
    helper.mount(() => m(AnalyticsiFrameWidget, {model, pluginId: "some-plugin", uid: "some-uid", init}));
  }

});
