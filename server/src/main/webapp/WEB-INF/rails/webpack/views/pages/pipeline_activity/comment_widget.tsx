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

import _ from "lodash";
import m from "mithril";
import Stream from "mithril/stream";
import {Default, Dropdown, DropdownAttrs, Secondary} from "views/components/buttons";
import {Size, TextAreaField} from "views/components/forms/input_fields";
import * as Icons from "views/components/icons";
import styles from "./index.scss";

interface Attrs extends DropdownAttrs {
  canOperatePipeline: boolean;
  comment: Stream<string>;
  counterOrLabel: number | string;
  showCommentFor: Stream<string>;
  addOrUpdateComment: (comment: string, counterOrLabel: string | number) => void;
}

enum Mode {
  EDIT, VIEW
}

export class CommentWidget extends Dropdown<Attrs> {
  private mode: Mode    = Mode.VIEW;
  private commentHolder = Stream<string>();

  oninit(vnode: m.Vnode<Attrs>) {
    super.oninit(vnode);
    this.commentHolder(vnode.attrs.comment());
  }

  toggleDropdown(vnode: m.Vnode<Attrs>, e: MouseEvent) {
    super.toggleDropdown(vnode, e);
    this.mode = Mode.VIEW;
    if (vnode.attrs.show()) {
      vnode.attrs.showCommentFor(`${vnode.attrs.counterOrLabel}`);
    } else {
      vnode.attrs.showCommentFor("");
    }
  }

  protected doRenderButton(vnode: m.Vnode<Attrs>): m.Children {
    if (vnode.attrs.counterOrLabel === "0" || vnode.attrs.counterOrLabel === 0) {
      return;
    }

    const noComment = _.isEmpty(vnode.attrs.comment());
    if (noComment && !vnode.attrs.canOperatePipeline) {
      return;
    }

    const buttonText = noComment ? "Add" : "View";
    return <a disabled={!vnode.attrs.canOperatePipeline}
              class={styles.buildCauseButton}
              title={noComment ? "Add comment on this pipeline run." : "View comment on this pipeline run."}
              data-test-id={`${buttonText.toLowerCase()}-comment-button`}
              onclick={this.toggleDropdown.bind(this, vnode)}>
      {buttonText} Comment
    </a>;
  }

  protected doRenderDropdownContent(vnode: m.Vnode<Attrs>) {
    if (!vnode.attrs.show()) {
      return;
    }
    return <div class={styles.commentWrapper} data-test-id="comment-dropdown">
      {this.getCommentView(vnode)}
      <div class={styles.commentActions} data-test-id="comment-actions">
        <Default onclick={this.toggleDropdown.bind(this, vnode)} dataTestId="close-comment-dropdown-button">
          <Icons.Close iconOnly={true}/> Close
        </Default>
        {this.getActionButton(vnode)}
      </div>
    </div>;
  }

  private getActionButton(vnode: m.Vnode<Attrs>) {
    if (this.showEditView(vnode)) {
      return <Secondary dataTestId="save-comment-button"
                        onclick={this.updateComment.bind(this, vnode)}>
        <Icons.Check iconOnly={true}/> Save
      </Secondary>;
    }

    return <Secondary dataTestId="edit-comment-button"
                      disabled={!vnode.attrs.canOperatePipeline}
                      title={vnode.attrs.canOperatePipeline ? "Edit comment." : "Requires pipeline operate permission."}
                      onclick={() => this.mode = Mode.EDIT}>
      <Icons.Edit iconOnly={true}/> EDIT
    </Secondary>;
  }

  private updateComment(vnode: m.Vnode<Attrs>) {
    if (vnode.attrs.comment() !== this.commentHolder()) {
      vnode.attrs.comment(this.commentHolder());
      vnode.attrs.addOrUpdateComment(this.commentHolder(), vnode.attrs.counterOrLabel);
    }

    this.mode = Mode.VIEW;
    if (_.isEmpty(this.commentHolder())) {
      vnode.attrs.show(false);
      vnode.attrs.showCommentFor("");
    }
  }

  private getCommentView(vnode: m.Vnode<Attrs>) {
    if (this.showEditView(vnode)) {
      return <TextAreaField
        label="Enter comment"
        required={false}
        rows={5}
        size={Size.MATCH_PARENT}
        dataTestId="textarea-to-add-or-edit-comment"
        property={this.commentHolder}/>;
    }

    return <span class={styles.comment} data-test-id={"comment-read-only-view"}>{vnode.attrs.comment()}</span>;
  }

  private showEditView(vnode: m.Vnode<Attrs>) {
    return this.mode === Mode.EDIT || _.isEmpty(vnode.attrs.comment());
  }
}
