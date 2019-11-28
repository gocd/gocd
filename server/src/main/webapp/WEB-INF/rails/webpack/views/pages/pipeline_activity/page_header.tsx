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

import m from "mithril"
import {MithrilViewComponent} from "jsx/mithril-component";
import styles from "./index.scss";
import {Reset} from "views/components/buttons";
import * as Icons from "views/components/icons";
import {PipelineActivity} from "models/pipeline_activity/pipeline_activity";
import {Link} from "views/components/link";

interface Attrs {
  pipelineActivity: PipelineActivity;
  isAdmin: boolean;
  isGroupAdmin: boolean;
  unpausePipeline: () => void;
  pausePipeline: () => void;
}

export class PipelineActivityHeader extends MithrilViewComponent<Attrs> {
  view(vnode: m.Vnode<Attrs>): m.Children {
    return <div class={styles.pageHeader}>
      <div class={styles.pipelineInfo}>
        <span class={styles.label} data-test-id="page-header-pipeline-label">Pipeline</span>
        <span class={styles.value}
              data-test-id="page-header-pipeline-name">{vnode.attrs.pipelineActivity.pipelineName()}</span>
      </div>
      {PipelineActivityHeader.getPipelinePauseUnpauseButton(vnode)}
      <div class={styles.pauseMessage}>
        {PipelineActivityHeader.pauseStatusText(vnode.attrs.pipelineActivity)}
      </div>
      {PipelineActivityHeader.pipelineSettingsLink(vnode)}
    </div>;
  }

  private static pipelineSettingsLink(vnode: m.Vnode<Attrs>) {
    if (vnode.attrs.isAdmin || vnode.attrs.isGroupAdmin) {
      return <div class={styles.iconContainer} data-test-id="page-header-pipeline-settings">
        <Link target={"_blank"} href={`/go/admin/pipelines/${vnode.attrs.pipelineActivity.pipelineName()}/general`}>
          <Icons.Settings iconOnly={true}/>
        </Link>
      </div>;
    }
  }

  private static getPipelinePauseUnpauseButton(vnode: m.Vnode<Attrs>) {
    if (!vnode.attrs.pipelineActivity.canPause()) {
      return;
    }

    if (vnode.attrs.pipelineActivity.paused()) {
      return <div class={styles.iconContainer}>
        <Reset small={true} onclick={() => vnode.attrs.unpausePipeline()}
               data-test-id="page-header-unpause-btn">UNPAUSE</Reset>
      </div>;
    }

    return <div class={styles.iconContainer}>
      <Reset small={true} onclick={() => vnode.attrs.pausePipeline()} data-test-id="page-header-pause-btn">PAUSE</Reset>
    </div>;
  }

  private static pauseStatusText(pipelineActivity: PipelineActivity) {
    let text = '';
    if (pipelineActivity.paused()) {
      text = 'Scheduling is paused';
      if (pipelineActivity.pauseBy()) text += ' by ' + pipelineActivity.pauseBy();
      if (pipelineActivity.pauseCause()) text += ' (' + pipelineActivity.pauseCause() + ')';
    }
    return text;
  }
}
