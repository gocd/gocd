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

import {MithrilViewComponent} from "jsx/mithril-component";
import _ from "lodash";
import m from "mithril";
import Stream from "mithril/stream";
import {Default, DropdownAttrs, Primary, Secondary} from "views/components/buttons";
import {Size, TextAreaField} from "views/components/forms/input_fields";
import {Modal} from "views/components/modal";
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

class CommentModal extends Modal {
  private readonly originalComment: Stream<string>;
  private readonly comment: Stream<string>;
  private readonly canOperatePipeline: boolean;
  private readonly counterOrLabel: number | string;
  private readonly addOrUpdateComment: (comment: string, counterOrLabel: string | number) => void;
  private mode: Mode;

  constructor(comment: string,
              canOperatePipeline: boolean,
              counterOrLabel: number | string,
              addOrUpdateComment: (comment: string, counterOrLabel: string | number) => void,
              mode = Mode.VIEW) {
    super();
    this.originalComment    = Stream(comment);
    this.comment            = Stream(this.originalComment());
    this.canOperatePipeline = canOperatePipeline;
    this.counterOrLabel     = counterOrLabel;
    this.mode               = mode;
    this.addOrUpdateComment = addOrUpdateComment;
  }

  body(): m.Children {
    if (this.showEditView()) {
      return <TextAreaField
        focus={true}
        label="Enter comment"
        required={false}
        rows={5}
        size={Size.MATCH_PARENT}
        dataTestId="textarea-to-add-or-edit-comment"
        property={this.comment}/>;
    }

    return <span class={styles.pipelineRunComment} data-test-id={"comment-read-only-view"}>{this.originalComment()}</span>;
  }

  title(): string {
    const action = _.isEmpty(this.originalComment()) ? "Add" : this.mode === Mode.EDIT ? "Edit" : "View";
    return `${action} comment`;
  }

  buttons(): m.ChildArray {
    return [
      this.getActionButton(),
      <Default onclick={this.close.bind(this)} dataTestId="close-comment-dropdown-button">Close</Default>
    ];
  }

  private showEditView() {
    return this.mode === Mode.EDIT || _.isEmpty(this.originalComment());
  }

  private getActionButton() {
    if (this.showEditView()) {
      return [
        <Primary dataTestId="save-comment-button" onclick={this.updateComment.bind(this)}>Save</Primary>,
        <Primary dataTestId="save-comment-and-close-button" onclick={this.updateAndClose.bind(this)}>Save &
          Close</Primary>
      ];
    }

    return <Secondary dataTestId="edit-comment-button"
                      disabled={!this.canOperatePipeline}
                      title={this.canOperatePipeline ? "Edit comment." : "Requires pipeline operate permission."}
                      onclick={() => {
                        this.mode = Mode.EDIT;
                        m.redraw.sync();
                      }}>Edit</Secondary>;
  }

  private updateAndClose() {
    this.updateComment();
    this.close();
  }

  private updateComment() {
    if (this.originalComment() !== this.comment()) {
      this.addOrUpdateComment(this.comment(), this.counterOrLabel);
    }
    this.originalComment(this.comment());
    this.mode = Mode.VIEW;
    if (_.isEmpty(this.originalComment())) {
      this.close();
    }
  }
}

export class CommentWidget extends MithrilViewComponent<Attrs> {
  view(vnode: m.Vnode<Attrs>): m.Children {
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
              onclick={CommentWidget.showModal.bind(this, vnode)}>
      {buttonText} Comment
    </a>;
  }

  private static showModal(vnode: m.Vnode<Attrs>) {
    new CommentModal(vnode.attrs.comment(),
      vnode.attrs.canOperatePipeline,
      vnode.attrs.counterOrLabel,
      vnode.attrs.addOrUpdateComment,
      vnode.attrs.startPollin)
      .render();
  }
}
