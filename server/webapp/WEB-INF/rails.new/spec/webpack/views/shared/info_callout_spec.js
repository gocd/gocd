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

describe("Info Callout Widget", () => {

  const m                 = require('mithril');
  const InfoCalloutWidget = require('views/shared/info_callout');

  const unmount = () => {
    m.mount(root, null);
    m.redraw();
  };

  let $root, root;

  beforeEach(() => {
    [$root, root] = window.createDomElementForTest();
  });

  afterEach(() => {
    unmount();
    window.destroyDomElementForTest();
  });

  it('should render info message', () => {
    m.mount(root,
      {
        view() {
          return m(InfoCalloutWidget, {message: "some-message"});
        }
      }
    );
    m.redraw();

    expect($root).toContainHtml("<div class=\"row expanded\"><div class=\"callout info\">some-message</div></div>");
  });

});
