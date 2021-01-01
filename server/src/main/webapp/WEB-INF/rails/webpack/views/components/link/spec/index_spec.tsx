/*
 * Copyright 2021 ThoughtWorks, Inc.
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

import m from "mithril";
import {Link} from "views/components/link/index";
import {TestHelper} from "views/pages/spec/test_helper";
import styles from "../index.scss";

describe("Link", () => {

  const helper = new TestHelper();

  afterEach(() => helper.unmount());

  it("should add rel when target is specified", () => {
    helper.mount(() => <Link target="_blank" href="docs.gocd.org"/>);

    const anchor = helper.q("a");
    expect(anchor).toHaveAttr("rel", "noopener noreferrer");
  });

  it("should not add rel when no target is specified", () => {
    helper.mount(() => <Link href="docs.gocd.org"/>);

    const anchor = helper.q("a");
    expect(anchor).not.toHaveAttr("rel");
  });

  it("should show external link icon when specified", () => {
    helper.mount(() => <Link target="_blank" href="docs.gocd.org" externalLinkIcon={true}/>);
    const anchor = helper.q("a");
    expect(anchor).toHaveClass(styles.externalIcon);
  });
});
