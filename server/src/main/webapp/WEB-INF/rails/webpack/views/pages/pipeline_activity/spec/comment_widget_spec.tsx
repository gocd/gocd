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
import Stream from "mithril/stream";
import {ModalManager} from "views/components/modal/modal_manager";
import {TestHelper} from "../../spec/test_helper";
import {CommentWidget} from "../comment_widget";

describe("CommentWidget", () => {
  const helper = new TestHelper();
  let showCommentFor: Stream<string>,
      show: Stream<boolean>,
      addOrUpdateComment: (comment: string, counterOrLabel: string | number) => void,
      stopPolling: () => void,
      startPolling: () => void;

  beforeEach(() => {
    showCommentFor     = Stream("");
    show               = Stream<boolean>(false);
    addOrUpdateComment = jasmine.createSpy("addOrUpdateComment");
    stopPolling        = jasmine.createSpy("stopPolling");
    startPolling       = jasmine.createSpy("startPolling");
  });

  afterEach(() => {
    ModalManager.closeAll();
    helper.unmount();
  });

  describe("Zero pipeline runs", () => {
    it("should not render add comment button(0 is number)", () => {
      const comment = Stream<string>();
      mount(comment, 0);

      expect(helper.byTestId("add-comment-button")).not.toBeInDOM();
      expect(helper.byTestId("edit-comment-button")).not.toBeInDOM();
      expect(helper.byTestId("comment-container")).not.toBeInDOM();
    });

    it("should not render add comment button(0 is string)", () => {
      const comment = Stream<string>();
      mount(comment, "0");

      expect(helper.byTestId("add-comment-button")).not.toBeInDOM();
      expect(helper.byTestId("edit-comment-button")).not.toBeInDOM();
      expect(helper.byTestId("comment-container")).not.toBeInDOM();
    });
  });

  describe("Pipeline run without comment", () => {
    const comment = Stream<string>();
    beforeEach(() => comment(""));

    it("should render add comment link ", () => {
      mount(comment, 1);

      expect(helper.byTestId("add-comment-button")).toBeInDOM();
      expect(helper.byTestId("add-comment-button")).toHaveText("Add Comment");
      expect(helper.byTestId("add-comment-button")).toHaveAttr("title", "Add comment on this pipeline run.");
    });

    it("should not render add comment button when user does not have operate permission on pipeline", () => {
      mount(comment, 1, false);

      expect(helper.byTestId("add-comment-button")).not.toBeInDOM();
    });

    it("should render add comment button when user has operate permission on pipeline", () => {
      mount(comment, 1, true);

      expect(helper.byTestId("add-comment-button")).toBeInDOM();
    });

    it("should render modal in edit mode", () => {
      mount(comment, 1);

      helper.clickByTestId("add-comment-button");

      const modal = helper.modal();
      expect(helper.byTestId("modal-body", modal)).toBeInDOM();
      expect(helper.byTestId("textarea-to-add-or-edit-comment", modal)).toBeInDOM();
      expect(helper.byTestId("textarea-to-add-or-edit-comment", modal)).toHaveText("");
      expect(helper.byTestId("save-comment-button", modal)).toBeInDOM();
      expect(helper.byTestId("close-comment-dropdown-button", modal)).toBeInDOM();

      expect(helper.byTestId("comment-read-only-view", modal)).not.toBeInDOM();
      expect(helper.byTestId("edit-comment-button", modal)).not.toBeInDOM();
    });

    it("should save the comment on click of save button", () => {
      mount(comment, 1);
      helper.clickByTestId("add-comment-button");
      const modal = helper.modal();

      const newComment = "This is dummy comment";
      helper.oninput(helper.byTestId("textarea-to-add-or-edit-comment", modal), newComment);
      helper.clickByTestId("save-comment-button", modal);

      expect(addOrUpdateComment).toHaveBeenCalledWith(newComment, 1);
    });

    it("should switch to read only mode once comment is updated", () => {
      mount(comment, 1);
      helper.clickByTestId("add-comment-button");
      const modal = helper.modal();

      const newComment = "This is dummy comment";
      helper.oninput(helper.byTestId("textarea-to-add-or-edit-comment", modal), newComment);
      helper.clickByTestId("save-comment-button", modal);

      expect(addOrUpdateComment).toHaveBeenCalledWith(newComment, 1);
      expect(helper.byTestId("modal-body", modal)).toBeInDOM();
      expect(helper.byTestId("comment-read-only-view", modal)).toBeInDOM();
      expect(helper.byTestId("comment-read-only-view", modal)).toHaveText(newComment);
      expect(helper.byTestId("textarea-to-add-or-edit-comment", modal)).not.toBeInDOM();
    });

    it("should close the modal on click of close button", () => {
      mount(comment, 1);
      helper.clickByTestId("add-comment-button");
      const modal = helper.modal();
      expect(helper.byTestId("modal-body", modal)).toBeInDOM();

      helper.clickByTestId("close-comment-dropdown-button", modal);

      expect(helper.byTestId("modal-body", modal)).not.toBeInDOM();
    });

    it("should always close the modal on click of save and close button", () => {
      mount(comment, 1);
      helper.clickByTestId("add-comment-button");
      const modal = helper.modal();
      expect(helper.byTestId("modal-body", modal)).toBeInDOM();

      helper.clickByTestId("save-comment-and-close-button", modal);

      expect(helper.byTestId("modal-body", modal)).not.toBeInDOM();
    });
  });

  describe("Pipeline run with existing comment", () => {
    const comment = Stream<string>();
    beforeEach(() => comment("This is existing comment"));

    it("should render view comment link", () => {
      mount(comment, 1);

      expect(helper.byTestId("view-comment-button")).toBeInDOM();
      expect(helper.byTestId("view-comment-button")).toHaveText("View Comment");
      expect(helper.byTestId("view-comment-button")).toHaveAttr("title", "View comment on this pipeline run.");
    });

    it("should disable edit button when user does not have operate permission on pipeline", () => {
      mount(comment, 1, false);
      helper.clickByTestId("view-comment-button");
      const modal = helper.modal();

      expect(helper.byTestId("edit-comment-button", modal)).toBeDisabled();
      expect(helper.byTestId("edit-comment-button", modal)).toHaveAttr("title", "Requires pipeline operate permission.");
    });

    it("should enable edit comment button when user has operate permission on pipeline", () => {
      mount(comment, 1, true);
      helper.clickByTestId("view-comment-button");
      const modal = helper.modal();

      expect(helper.byTestId("edit-comment-button", modal)).not.toBeDisabled();
      expect(helper.byTestId("edit-comment-button", modal)).toHaveAttr("title", "Edit comment.");
    });

    it("should render modal in view mode", () => {
      mount(comment, 1);

      helper.clickByTestId("view-comment-button");

      const modal = helper.modal();
      expect(helper.byTestId("modal-body", modal)).toBeInDOM();
      expect(helper.byTestId("comment-read-only-view", modal)).toBeInDOM();
      expect(helper.byTestId("comment-read-only-view", modal)).toHaveText(comment());

      expect(helper.byTestId("edit-comment-button", modal)).toBeInDOM();
      expect(helper.byTestId("close-comment-dropdown-button", modal)).toBeInDOM();

      expect(helper.byTestId("save-comment-button", modal)).not.toBeInDOM();
      expect(helper.byTestId("textarea-to-add-or-edit-comment", modal)).not.toBeInDOM();
    });

    it("should render textarea to edit comment on click of the edit button", () => {
      mount(comment, 1);
      helper.clickByTestId("view-comment-button");
      const modal = helper.modal();

      helper.clickByTestId("edit-comment-button", modal);

      expect(helper.byTestId("textarea-to-add-or-edit-comment", modal)).toBeInDOM();
      m.redraw.sync();
      expect(helper.byTestId("textarea-to-add-or-edit-comment", modal)).toHaveValue(comment());

      expect(helper.byTestId("close-comment-dropdown-button", modal)).toBeInDOM();
      expect(helper.byTestId("save-comment-button", modal)).toBeInDOM();

      expect(helper.byTestId("edit-comment-button", modal)).not.toBeInDOM();
      expect(helper.byTestId("comment-read-only-view", modal)).not.toBeInDOM();
    });

    it("should update the comment on click of the save button", () => {
      mount(comment, 1);
      helper.clickByTestId("view-comment-button");
      const modal = helper.modal();
      helper.clickByTestId("edit-comment-button", modal);

      const updatedComment = "This is updated comment";
      helper.oninput(helper.byTestId("textarea-to-add-or-edit-comment", modal), updatedComment);
      helper.clickByTestId("save-comment-button", modal);

      expect(addOrUpdateComment).toHaveBeenCalledWith(updatedComment, 1);
    });

    it("should close the dialog once comment is edited", () => {
      mount(comment, 1);
      helper.clickByTestId("view-comment-button");
      const modal = helper.modal();
      helper.clickByTestId("edit-comment-button", modal);

      const updatedComment = "This is updated comment";
      helper.oninput(helper.byTestId("textarea-to-add-or-edit-comment", modal), updatedComment);
      helper.clickByTestId("save-comment-button", modal);

      expect(helper.byTestId("modal-body", modal)).toBeInDOM();
      expect(helper.byTestId("comment-read-only-view", modal)).toBeInDOM();
      expect(helper.byTestId("comment-read-only-view", modal)).toHaveText(updatedComment);
      expect(helper.byTestId("textarea-to-add-or-edit-comment", modal)).not.toBeInDOM();
      expect(addOrUpdateComment).toHaveBeenCalledWith(updatedComment, 1);
    });

    it("should switch to view mode without updating the comment on click of save when content is unchanged", () => {
      mount(comment, 1);
      helper.clickByTestId("view-comment-button");
      const modal = helper.modal();
      helper.clickByTestId("edit-comment-button", modal);

      const sameContent = comment();
      helper.oninput(helper.byTestId("textarea-to-add-or-edit-comment", modal), sameContent);
      helper.clickByTestId("save-comment-button", modal);

      expect(helper.byTestId("modal-body", modal)).toBeInDOM();
      expect(helper.byTestId("comment-read-only-view", modal)).toBeInDOM();
      expect(helper.byTestId("comment-read-only-view", modal)).toHaveText(sameContent);
      expect(helper.byTestId("textarea-to-add-or-edit-comment", modal)).not.toBeInDOM();
      expect(addOrUpdateComment).not.toHaveBeenCalledWith(sameContent, 1);
    });

    it("should close the dropdown when comment is removed", () => {
      mount(comment, 1);
      helper.clickByTestId("view-comment-button");
      const modal = helper.modal();
      helper.clickByTestId("edit-comment-button", modal);

      helper.oninput(helper.byTestId("textarea-to-add-or-edit-comment", modal), "");
      helper.clickByTestId("save-comment-button", modal);

      expect(helper.byTestId("modal-body", modal)).not.toBeInDOM();
    });
  });

  function mount(comment: Stream<string>, counterOrLabel: number | string, canOperatePipeline = true) {
    helper.mount(() => <CommentWidget comment={comment}
                                      counterOrLabel={counterOrLabel}
                                      canOperatePipeline={canOperatePipeline}
                                      addOrUpdateComment={addOrUpdateComment}
                                      showCommentFor={showCommentFor}
                                      startPolling={startPolling}
                                      stopPolling={stopPolling}
                                      show={show}/>);
  }
});
