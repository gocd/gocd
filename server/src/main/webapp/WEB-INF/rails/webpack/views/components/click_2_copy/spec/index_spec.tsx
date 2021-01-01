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

import { asSelector } from "helpers/css_proxies";
import m from "mithril";
import { asPromise } from "models/base/accessor";
import { TestHelper } from "views/pages/spec/test_helper";
import { Click2Copy, CopySnippet } from "..";
import * as styles from "../index.scss";

const sel = asSelector(styles);

describe("click_2_copy", () => {
  describe("Click2Copy component", () => {
    const helper = new TestHelper();

    afterEach(() => helper.unmount());

    it("copies data provided by the reader()", (done) => {
      const copier = jasmine.createSpy("copy").and.resolveTo(void 0);
      const hasRead = jasmine.createSpy("read:data");

      helper.mount(() => <Click2Copy reader={asPromise(() => "Copy that, Sarge!")} copier={copier}
        onfail={() => fail("should not fail")}
        ondata={hasRead}
        oncopy={() => {
          expect(hasRead).toHaveBeenCalled();
          expect(copier).toHaveBeenCalledWith("Copy that, Sarge!");
          done();
        }} />);

      helper.click(sel.clipButton);
    });

    it("notifies on reader error", (done) => {
      const reader = new Promise<string>((_, r) => r("I'm illiterate."));
      const copier = jasmine.createSpy("copy");
      const hasRead = jasmine.createSpy("read:data");

      helper.mount(() => <Click2Copy reader={reader} copier={copier}
        ondata={hasRead}
        oncopy={() => fail("should not copy")}
        ondatafail={() => {
          expect(hasRead).not.toHaveBeenCalled();
          expect(copier).not.toHaveBeenCalled();
          done();
        }} />);

      helper.click(sel.clipButton);
    });

    it("notifies on copier error", (done) => {
      const copier = jasmine.createSpy("copy").and.rejectWith("");
      const hasRead = jasmine.createSpy("read:data");

      helper.mount(() => <Click2Copy reader={asPromise(() => "Copy that, Sarge!")} copier={copier}
        oncopy={() => fail("should not copy")}
        ondata={hasRead}
        oncopyfail={() => {
          expect(hasRead).toHaveBeenCalled();
          expect(copier).toHaveBeenCalledWith("Copy that, Sarge!");
          done();
        }} />);

      helper.click(sel.clipButton);
    });
  });

  describe("CopySnippet component", () => {
    const helper = new TestHelper();

    afterEach(() => helper.unmount());

    it("copies data provided by the reader()", (done) => {
      const copier = jasmine.createSpy("copy").and.resolveTo(void 0);

      helper.mount(() => <CopySnippet reader={() => "Copy that, Sarge!"} copier={copier}
        oncopy={() => {
          expect(copier).toHaveBeenCalledWith("Copy that, Sarge!");
          done();
        }} />);

      helper.click(sel.clipButton);
    });
  });
});
