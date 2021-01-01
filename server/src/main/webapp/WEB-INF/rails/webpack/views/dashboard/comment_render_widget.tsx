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
import {renderComment, TrackingTool} from "helpers/render_comment";
import {MithrilViewComponent} from "jsx/mithril-component";
import m from "mithril";

export interface Attrs {
  text: string;
  trackingTool: TrackingTool;
}

export class CommentRenderWidget extends MithrilViewComponent<Attrs> {
  view(vnode: m.Vnode<Attrs>) {
    const text         = vnode.attrs.text;
    const trackingTool = vnode.attrs.trackingTool;

    return (<div class="item comment"><p>{m.trust(renderComment(text, trackingTool))}</p></div>);
  }
}
