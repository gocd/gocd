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

import {ApiResult, ErrorResponse} from "helpers/api_request_builder";
import {MithrilComponent} from "jsx/mithril-component";
import m from "mithril";
import Stream from "mithril/stream";
import {Reset} from "views/components/buttons";
import {FlashMessageModelWithTimeout, MessageType} from "views/components/flash_message";
import {Size, TextAreaField} from "views/components/forms/input_fields";
import * as Icons from "views/components/icons";
import {Link} from "views/components/link";
import {PipelineStatus} from "views/pages/pipeline_activity/common/models/pipeline_status";
import {ConfirmationDialog} from "views/pages/pipeline_activity/confirmation_modal";
import styles from "../index.scss";

interface Attrs {
  pipelineName: string;
  shouldShowPauseUnpause: boolean;
  shouldShowPipelineSettings: boolean;
  flashMessage: FlashMessageModelWithTimeout;
  onPipelinePause?: (result: ApiResult<any>) => any;
  onPipelineUnpause?: (result: ApiResult<any>) => any;
  pipelineStatus?: PipelineStatus;
}

interface State {
  pipelineStatus: Stream<PipelineStatus>;
}

export class PipelinePauseHeader extends MithrilComponent<Attrs, State> {
  oninit(vnode: m.Vnode<Attrs, State>) {
    vnode.state.pipelineStatus = Stream();
    if (vnode.attrs.pipelineStatus) {
      vnode.state.pipelineStatus(vnode.attrs.pipelineStatus);
    } else {
      PipelineStatus.fetch(vnode.attrs.pipelineName).then(vnode.state.pipelineStatus);
    }
  }

  view(vnode: m.Vnode<Attrs, State>): m.Children {
    return <div class={styles.pageHeader}>
      <div class={styles.pipelineInfo}>
        <span class={styles.label} data-test-id="page-header-pipeline-label">Pipeline</span>
        <span class={styles.value}
              title={vnode.attrs.pipelineName}
              data-test-id="page-header-pipeline-name">{vnode.attrs.pipelineName}</span>
      </div>

      {this.getPipelinePauseUnpauseButton(vnode)}
      {this.pauseMessageText(vnode)}
      {this.pipelineSettingsLink(vnode)}
    </div>;
  }

  private pipelineSettingsLink(vnode: m.Vnode<Attrs, State>) {
    if (vnode.attrs.shouldShowPipelineSettings) {
      return <div class={styles.iconContainer} data-test-id="page-header-pipeline-settings">
        <Link href={`/go/admin/pipelines/${vnode.attrs.pipelineName}/general`}>
          <Icons.Settings iconOnly={true}/>
        </Link>
      </div>;
    }
  }

  private getPipelinePauseUnpauseButton(vnode: m.Vnode<Attrs, State>) {
    if (!vnode.attrs.shouldShowPauseUnpause || !vnode.state.pipelineStatus()) {
      return;
    }

    const pipelineStatus = vnode.state.pipelineStatus()!;
    if (pipelineStatus.isPaused()) {
      return <div class={styles.iconContainer}>
        <Reset small={true} data-test-id="page-header-unpause-btn" onclick={() => {
          pipelineStatus.unpause().then((result) => {
            result.do(() => {
              vnode.state.pipelineStatus().isPaused(false);
              vnode.state.pipelineStatus().pausedBy(undefined);
              vnode.state.pipelineStatus().pausedCause(undefined);
              vnode.attrs.flashMessage.setMessage(MessageType.success, "Pipeline unpaused Successfully!");

              if (vnode.attrs.onPipelineUnpause) {
                vnode.attrs.onPipelineUnpause(result);
              }
            }, this.onUpdateFailure.bind(this, vnode));
          });
        }}>UNPAUSE</Reset>
      </div>;
    }

    return <div class={styles.iconContainer}>
      <Reset small={true}
             data-test-id="page-header-pause-btn"
             onclick={this.renderPausePipelineConfirmation.bind(this, vnode)}>
        PAUSE
      </Reset>
    </div>;
  }

  private renderPausePipelineConfirmation(vnode: m.Vnode<Attrs, State>) {
    const body = <TextAreaField required={false} size={Size.MATCH_PARENT}
                                property={vnode.state.pipelineStatus()!.pausedCause}
                                rows={5}
                                dataTestId="pause-pipeline-textarea"
                                label="Specify the reason why you want to stop scheduling on this pipeline"/>;

    new ConfirmationDialog("Pause pipeline", body, () => {
      return vnode.state.pipelineStatus().pause().then((result) => {
        result.do(() => {
          const user = document.body.getAttribute("data-user-display-name") as string;
          vnode.state.pipelineStatus().isPaused(true);
          vnode.state.pipelineStatus().pausedBy(user);
          vnode.attrs.flashMessage.setMessage(MessageType.success, "Pipeline paused Successfully!");

          if (vnode.attrs.onPipelinePause) {
            vnode.attrs.onPipelinePause(result);
          }
        }, this.onUpdateFailure.bind(this, vnode));
      });
    }).render();
  }

  private onUpdateFailure(vnode: m.Vnode<Attrs, State>, errorResponse: ErrorResponse) {
    const parsed  = errorResponse.body ? JSON.parse(errorResponse.body!) : {};
    const message = parsed.message ? parsed.message : errorResponse.message;
    vnode.attrs.flashMessage.setMessage(MessageType.alert, message);
  }

  private pauseMessageText(vnode: m.Vnode<Attrs, State>) {
    if (!vnode.state.pipelineStatus() || !vnode.state.pipelineStatus().isPaused()) {
      return;
    }

    let pauseMessage     = "Scheduling is paused";
    const pipelineStatus = vnode.state.pipelineStatus()!;
    if (pipelineStatus.pausedBy()) {
      pauseMessage += ` by ${pipelineStatus.pausedBy()}`;
    }
    if (pipelineStatus.pausedCause()) {
      pauseMessage += ` (${pipelineStatus.pausedCause()})`;
    }

    return <div class={styles.pauseMessage} title={pauseMessage}
                data-test-id="pipeline-pause-message">{pauseMessage}</div>;
  }
}
