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
import {stubAllMethods, TestHelper} from "views/pages/spec/test_helper";
import {Anchor, AnchorVM, ScrollManager} from "../anchor";

describe("Anchor", () => {
  const helper = new TestHelper();

  afterEach(() => helper.unmount());

  it("fires onnavigate() when sm.shouldScroll() is true", () => {
    const sm: ScrollManager = stubAllMethods(["getTarget", "setTarget", "shouldScroll", "scrollToEl"]);
    (sm.shouldScroll as jasmine.Spy).and.returnValue(true);

    const onnavigate: () => void = jasmine.createSpy("onnavigate()");

    helper.mount(() => <Anchor id="foo" sm={sm} onnavigate={onnavigate}/>);

    expect(sm.shouldScroll).toHaveBeenCalledWith("foo");
    expect(sm.scrollToEl).toHaveBeenCalled();
    expect(onnavigate).toHaveBeenCalled();
  });

  it("does not fire onnavigate() when sm.shouldScroll() is false", () => {
    const sm: ScrollManager = stubAllMethods(["getTarget", "setTarget", "shouldScroll", "scrollToEl"]);
    (sm.shouldScroll as jasmine.Spy).and.returnValue(false);

    const onnavigate: () => void = jasmine.createSpy("onnavigate()");

    helper.mount(() => <Anchor id="foo" sm={sm} onnavigate={onnavigate}/>);

    expect(sm.shouldScroll).toHaveBeenCalledWith("foo");
    expect(sm.scrollToEl).not.toHaveBeenCalled();
    expect(onnavigate).not.toHaveBeenCalled();
  });
});

describe("AnchorVM", () => {
  it("getTarget() reports the target after setTarget() (yes, this is boring)", () => {
    const vm = new AnchorVM();
    expect(vm.getTarget()).toBe("");

    vm.setTarget("foo");
    expect(vm.getTarget()).toBe("foo");
  });

  it("shouldScroll() only reports true the first time it is called when the target matches", () => {
    const vm = new AnchorVM();

    vm.setTarget("foo");
    expect(vm.getTarget()).toBe("foo");

    expect(vm.shouldScroll("blah")).toBe(false); // doesn't match ID
    expect(vm.shouldScroll("foo")).toBe(true); // matches!

    // this should fail the second time so as to prevent multiple scrollToEl() calls when used with <Anchor/>
    expect(vm.shouldScroll("foo")).toBe(false); // fails because it's a repeated call
    expect(vm.getTarget()).toBe("foo"); // but the ID still matches
  });

  it('hasTarget() should report true only if something is specified', () => {
    const vm = new AnchorVM();
    expect(vm.hasTarget()).toBe(false);

    vm.setTarget("foo");
    expect(vm.hasTarget()).toBe(true);
  });
});
