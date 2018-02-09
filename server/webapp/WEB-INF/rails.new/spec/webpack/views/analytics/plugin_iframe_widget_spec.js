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
describe("Plugin iFrame Widget", () => {
  const m      = require('mithril');
  const Stream = require('mithril/stream');
  require('jasmine-jquery');

  const PluginiFrameWidget = require('views/analytics/plugin_iframe_widget');

  let $root, root;
  beforeEach(() => {
    [$root, root] = window.createDomElementForTest();
  });
  afterEach(window.destroyDomElementForTest);

  afterEach(() => {
    unmount();
  });

  it('should load view path from model and create an iframe with sandbox', () => {
    const model = (function () {
      const data = Stream();
      const view = Stream();

      const load = () => {
        data({a: 1});
        view('/some/path');
      };

      return {view, data, load};
    }());

    mount(model, 'some-plugin', 'some-uid');

    expect($root.find('iframe[src="/some/path"]').attr('sandbox')).toEqual('allow-scripts');
  });

  const mount = (model, pluginId, uid) => {
    m.mount(root, {
      view() {
        return m(PluginiFrameWidget, {
          model,
          pluginId,
          uid
        });
      }
    });
    m.redraw();
  };

  const unmount = () => {
    m.mount(root, null);
    m.redraw();
  };
});
