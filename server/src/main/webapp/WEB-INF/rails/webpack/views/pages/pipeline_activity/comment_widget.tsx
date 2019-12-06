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

import {MithrilViewComponent} from "jsx/mithril-component";
import _ from "lodash";
import m from "mithril";
import Stream from "mithril/stream";
import {Secondary} from "views/components/buttons";
import styles from "./index.scss";

interface Attrs {
  comment: Stream<string>;
  counterOrLabel: number | string;
  addOrUpdateComment: (comment: Stream<string>, counterOrLabel: string | number) => void;
}

export class CommentWidget extends MithrilViewComponent<Attrs> {
  view(vnode: m.Vnode<Attrs, this>): m.Children {
    if (vnode.attrs.counterOrLabel === "0" || vnode.attrs.counterOrLabel === 0) {
      return;
    }

    if (_.isEmpty(vnode.attrs.comment())) {
      return <div class={styles.commentWrapper}>
        <Secondary small={true}
                   dataTestId={"add-comment-button"}
                   onclick={() => vnode.attrs.addOrUpdateComment(vnode.attrs.comment, vnode.attrs.counterOrLabel)}>
          Add Comment
        </Secondary>
      </div>;
    }

    return <div class={styles.commentWrapper}>
      <div class={styles.comment} data-test-id={"comment-container"}>{vnode.attrs.comment()}</div>
      <Secondary small={true}
                 dataTestId={"edit-comment-button"}
                 onclick={() => vnode.attrs.addOrUpdateComment(vnode.attrs.comment, vnode.attrs.counterOrLabel)}>
        Edit Comment
      </Secondary>
    </div>;
  }
}
