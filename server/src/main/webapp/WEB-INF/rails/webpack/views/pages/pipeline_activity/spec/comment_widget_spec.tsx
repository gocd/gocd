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

import m from "mithril";
import Stream from "mithril/stream";
import {TestHelper} from "../../spec/test_helper";
import {CommentWidget} from "../comment_widget";

describe("CommentWidget", () => {
  const helper = new TestHelper();
  let addOrUpdateComment: (comment: Stream<string>, counterOrLabel: string | number) => void;

  beforeEach(() => {
    addOrUpdateComment = jasmine.createSpy("addOrUpdateComment");
  });

  afterEach(helper.unmount.bind(helper));

  it("should not render add comment button when pipeline has no run(0 is number)", () => {
    const comment = Stream<string>();
    mount(comment, 0);

    expect(helper.byTestId("add-comment-button")).not.toBeInDOM();
    expect(helper.byTestId("edit-comment-button")).not.toBeInDOM();
    expect(helper.byTestId("comment-container")).not.toBeInDOM();
  });

  it("should not render add comment button when pipeline has no run(0 is string)", () => {
    const comment = Stream<string>();
    mount(comment, "0");

    expect(helper.byTestId("add-comment-button")).not.toBeInDOM();
    expect(helper.byTestId("edit-comment-button")).not.toBeInDOM();
    expect(helper.byTestId("comment-container")).not.toBeInDOM();
  });

  it("should show add comment button when pipeline instance has no comment", () => {
    const comment = Stream<string>();
    mount(comment, 1);

    expect(helper.byTestId("add-comment-button")).toBeInDOM();
    expect(helper.byTestId("edit-comment-button")).not.toBeInDOM();
    expect(helper.byTestId("comment-container")).not.toBeInDOM();
    expect(addOrUpdateComment).not.toHaveBeenCalled();
  });

  it("should call addOrUpdateComment(..) when add comment button is clicked", () => {
    const comment = Stream<string>();
    mount(comment, 1);

    helper.clickByTestId("add-comment-button");

    expect(addOrUpdateComment).toHaveBeenCalledWith(comment, 1);
  });

  it("should render comment when pipeline instance has one", () => {
    const comment = Stream<string>("This is comment");
    mount(comment, 1);

    expect(helper.byTestId("add-comment-button")).not.toBeInDOM();
    expect(helper.byTestId("edit-comment-button")).toBeInDOM();
    expect(helper.byTestId("comment-container")).toBeInDOM();
    expect(helper.byTestId("comment-container")).toHaveText("This is comment");
    expect(addOrUpdateComment).not.toHaveBeenCalled();
  });

  it("should render edit comment button when pipeline instance has comment", () => {
    const comment = Stream<string>("This is comment");
    mount(comment, 1);

    expect(helper.byTestId("add-comment-button")).not.toBeInDOM();
    expect(helper.byTestId("edit-comment-button")).toBeInDOM();
    expect(addOrUpdateComment).not.toHaveBeenCalled();
  });

  it("should call addOrUpdateComment(..) when edit comment button is clicked", () => {
    const comment = Stream<string>("This is comment");
    mount(comment, 2);

    helper.clickByTestId("edit-comment-button");

    expect(addOrUpdateComment).toHaveBeenCalledWith(comment, 2);
  });

  function mount(comment: Stream<string>, counterOrLabel: number | string) {
    helper.mount(() => <CommentWidget comment={comment}
                                      counterOrLabel={counterOrLabel}
                                      addOrUpdateComment={addOrUpdateComment}/>);
  }
});
