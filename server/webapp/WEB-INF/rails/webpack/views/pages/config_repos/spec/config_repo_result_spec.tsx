/*
 * Copyright 2019 ThoughtWorks, Inc.
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
import * as m from "mithril";
import {ObjectCache} from "models/base/cache";
import {DefinedStructures} from "models/config_repos/defined_pipelines";
import {EventAware} from "models/mixins/event_aware";
import * as flashCss from "views/components/flash_message/index.scss";
import {TestHelper} from "views/pages/spec/test_helper";
import {CRResult} from "../config_repo_result";
import * as css from "../defined_structs.scss";

describe("<CRResult/>", () => {
  const sel = asSelector<typeof css>(css);
  const helper = new TestHelper();

  afterEach(() => helper.unmount());

  it("displays error message when content cannot be loaded", () => {
    const fl = asSelector<typeof flashCss>(flashCss);
    helper.mount(() => <CRResult cache={new MockCache({failureReason: "outta cache."})} repo="my-repo" vm={new EventAware()}/>);

    expect(helper.q(sel.tree)).not.toBeInDOM();
    expect(helper.q(sel.treeDatum)).not.toBeInDOM();
    expect(helper.q(sel.loading)).not.toBeInDOM();
    expect(helper.q(fl.alert)).toBeInDOM();
    expect(helper.text(fl.alert)).toBe("Failed to load pipelines defined in this repository: outta cache.");
  });

  it("displays loading message when data has not been fetched", () => {
    helper.mount(() => <CRResult cache={new MockCache({ready: false})} repo="my-repo" vm={new EventAware()}/>);

    expect(helper.q(sel.tree)).not.toBeInDOM();
    expect(helper.q(sel.treeDatum)).not.toBeInDOM();
    expect(helper.q(sel.loading)).toBeInDOM();
    expect(helper.text(sel.loading)).toBe("Loading pipelines defined by repository…");
  });

  it("renders the defined pipelines tree", () => {
    helper.mount(() => <CRResult cache={new MockCache({content: testData()})} repo="my-repo" vm={new EventAware()}/>);

    expect(helper.q(sel.tree)).toBeInDOM();
    expect(helper.q(sel.loading)).not.toBeInDOM();
    expect(helper.textAll(sel.treeDatum)).toEqual([ // (in order of traversal)
      "group-1",
        "pipeline-1",
          "p1-s1", "p1-s2",
        "pipeline-2",
          "p2-s1", "p2-s2",
      "group-2",
        "pipeline-3",
          "p3-s1", "p3-s2",
        "pipeline-4",
          "p4-s1", "p4-s2",
    ]);
  });

  it("fetches data on the expand event", () => {
    const cache = new MockCache({ready: false});
    const vm = new EventAware();
    helper.mount(() => <CRResult cache={cache} repo="my-repo" vm={vm}/>);

    expect(cache.prime).not.toHaveBeenCalled();

    vm.notify("expand");

    expect(cache.prime).toHaveBeenCalled();
  });

  it("invalidates and updates data on refresh event", () => {
    const cache = new MockCache({});
    const vm = new EventAware();
    helper.mount(() => <CRResult cache={cache} repo="my-repo" vm={vm}/>);

    expect(cache.invalidate).not.toHaveBeenCalled();
    expect(cache.prime).not.toHaveBeenCalled();

    vm.notify("refresh");

    expect(cache.invalidate).toHaveBeenCalled();
    expect(cache.prime).toHaveBeenCalled();
  });
});

function testData(): DefinedStructures {
  return DefinedStructures.fromJSON([
    {
      name: "group-1", pipelines: [
        { name: "pipeline-1", stages: [{ name: "p1-s1" }, { name: "p1-s2" }] },
        { name: "pipeline-2", stages: [{ name: "p2-s1" }, { name: "p2-s2" }] },
      ]
    },
    {
      name: "group-2", pipelines: [
        { name: "pipeline-3", stages: [{ name: "p3-s1" }, { name: "p3-s2" }] },
        { name: "pipeline-4", stages: [{ name: "p4-s1" }, { name: "p4-s2" }] },
      ]
    }
  ]);
}

class MockCache implements ObjectCache<DefinedStructures> {
  ready: () => boolean;
  contents: () => DefinedStructures;
  failureReason: () => string | undefined;
  prime: (onSuccess: () => void, onError?: () => void) => void = jasmine.createSpy();
  invalidate: () => void = jasmine.createSpy();

  constructor(options: {
    content?: DefinedStructures,
    failureReason?: string,
    ready?: boolean
  }) {
    this.failureReason = () => options.failureReason;
    this.contents = () => options.content || [];
    this.ready = () => void 0 === options.ready ? true : options.ready;
  }

  failed(): boolean {
    return !!this.failureReason();
  }
}
