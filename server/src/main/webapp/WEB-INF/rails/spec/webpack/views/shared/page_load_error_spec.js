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
import {TestHelper} from "views/pages/spec/test_helper";
import {PageLoadError} from "views/shared/page_load_error";
import m from "mithril";

describe("Page Load Error Widget", () => {

  const helper        = new TestHelper();

  afterEach(helper.unmount.bind(helper));

  it('should render error message', () => {
    helper.mount(() => m(PageLoadError, {message: "some-message"}));

    expect(helper.root).toContainHtml("<div class=\"alert callout\">" +
      "<h5>some-message</h5>" +
      "<p>Refresh <a href=\"javascript: window.location.reload()\">this page</a> in some time, and if the problem persists, " +
      "check the server logs.</p></div>" +
      "</div>");
  });

});
