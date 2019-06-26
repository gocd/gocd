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

  it("displays error message when contents are empty", () => {
    const fl = asSelector<typeof flashCss>(flashCss);
    helper.mount(() => <CRResult cache={new MockCache({content: void 0})} repo="my-repo" vm={new EventAware()}/>);

    expect(helper.q(sel.tree)).not.toBeInDOM();
    expect(helper.q(sel.treeDatum)).not.toBeInDOM();
    expect(helper.q(sel.loading)).not.toBeInDOM();
    expect(helper.q(fl.alert)).toBeInDOM();
    expect(helper.text(fl.alert)).toBe("This repository does not define any pipelines or environments.");
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
      "Groups, pipelines, and environments defined by this repository:",
      "env-1",
      "env-2",
      "group-1",
      "pipeline-1",
      "pipeline-2",
      "group-2",
      "pipeline-3",
      "pipeline-4",
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
  return DefinedStructures.fromJSON({
    environments: [{ name: "env-1"}, { name: "env-2" }],
    groups: [
      {
        name: "group-1", pipelines: [
          { name: "pipeline-1" },
          { name: "pipeline-2" },
        ]
      },
      {
        name: "group-2", pipelines: [
          { name: "pipeline-3" },
          { name: "pipeline-4" },
        ]
      }
    ]
  });
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
    this.contents = () => options.content || new DefinedStructures([], []);
    this.ready = () => void 0 === options.ready ? true : options.ready;
  }

  failed(): boolean {
    return !!this.failureReason();
  }
}
