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

import {asSelector} from "helpers/css_proxies";
import m from "mithril";
import Stream from "mithril/stream";
import {TestHelper} from "views/pages/spec/test_helper";
import {PreviewPane} from "../preview_pane";
import * as defaultStyles from "../styles.scss";

describe("AddPaC: PreviewPane", () => {
  const helper = new TestHelper();
  const sel = asSelector<typeof defaultStyles>(defaultStyles);

  const content = Stream("");
  const mime = Stream("");

  beforeEach(() => {
    content("");
    mime("");
    helper.mount(() => <PreviewPane content={content} mimeType={mime}/>);
  });

  afterEach(() => helper.unmount());

  it("renders message on empty content", () => {
    mime("application/x-yaml");
    m.redraw.sync();

    expect(helper.text(sel.previewPane)).toBe("# Your Pipelines as Code\n# definition will automatically\n# update here");

    mime("application/json");
    m.redraw.sync();

    expect(helper.text(sel.previewPane)).toBe("// Your Pipelines as Code\n// definition will automatically\n// update here");
  });

  it("renders content when available", () => {
    mime("application/x-yaml");
    content(`---\nformat_version: 5\n`);
    m.redraw.sync();

    expect(helper.text(sel.previewPane)).toBe(`---\nformat_version: 5`);
    expect(helper.textAll(sel.token)).toEqual(["---", "format_version", ":", "5"]);
  });
});
