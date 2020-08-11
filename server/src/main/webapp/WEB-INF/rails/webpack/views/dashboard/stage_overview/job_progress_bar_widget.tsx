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

import {bind} from "classnames/bind";
import m from "mithril";
import Stream from "mithril/stream";
import {MithrilComponent} from "../../../jsx/mithril-component";
import * as styles from "./index.scss";
import {JobDuration, JobDurationStrategyHelper} from "./models/job_duration_stratergy_helper";
import {StageInstance} from "./models/stage_instance";
import {JobJSON} from "./models/types";

const classnames = bind(styles);

export interface Attrs {
  job: JobJSON;
  lastPassedStageInstance: Stream<StageInstance | undefined>;
}

export class JobProgressBarWidget extends MithrilComponent<Attrs> {
  oncreate(vnode: m.Vnode<Attrs, {}>) {
    // @ts-ignore
    vnode.dom.children[0].onmouseover = () => {
      // @ts-ignore
      vnode.dom.children[1].style.visibility = 'visible';

      // stage overview job table body is a scrollable div and the tooltips are rendered absolute WRT to the job progress bar.
      // when the table body is scrolled, the tooltip is not re-positioned to considered the scrolled div.
      // find the amount of pixels the body is scrolled and reposition the tooltip considering scrolled offset.
      // 8 px is the default margin top required for the tooltip caret.
      const scrolledTop = document.getElementById("scrollable-jobs-table-body")?.scrollTop || 0;
      // @ts-ignore
      vnode.dom.children[1].style.marginTop = (8 - scrolledTop) + 'px';
    };

    // @ts-ignore
    vnode.dom.children[0].onmouseout = () => {
      // @ts-ignore
      vnode.dom.children[1].style.visibility = 'hidden';
    };
  }

  view(vnode: m.Vnode<Attrs, {}>): m.Children | void | null {
    const jobDuration: JobDuration = JobDurationStrategyHelper.getDuration(vnode.attrs.job, vnode.attrs.lastPassedStageInstance());
    const isJobInProgress = jobDuration.isJobInProgress;

    const tooltipItems = {
      'Total Time':   jobDuration.totalTimeForDisplay,
      'Scheduled At': jobDuration.startTimeForDisplay,
      'Completed At': jobDuration.endTimeForDisplay,
    };

    const stateTransitionItems = {
      'Waiting':             jobDuration.waitTimeForDisplay,
      'Preparing':           jobDuration.preparingTimeForDisplay,
      'Building':            jobDuration.buildTimeForDisplay,
      'Uploading Artifacts': jobDuration.uploadingArtifactTimeForDisplay
    };

    return (<div>
      <div data-test-id="progress-bar-container-div" class={styles.progressBarContainer}>
        <div class={`${styles.waiting}`} style={`width: ${jobDuration.waitTimePercentage}%`}/>
        <div class={`${styles.preparing}`} style={`width: ${jobDuration.preparingTimePercentage}%`}/>
        <div className={`${styles.building}`} style={`width: ${jobDuration.buildTimePercentage}%`}/>
        <div className={`${styles.uploadingArtifacts}`} style={`width: ${jobDuration.uploadingArtifactTimePercentage}%`}/>
        <div className={`${styles.unknown}`} style={`width: ${jobDuration.unknownTimePercentage}%`}/>
      </div>

      <div data-test-id="progress-bar-tooltip" class={styles.progressBarTooltip}>
        {
          Object.keys(stateTransitionItems).map((key, index) => {
            const value = (stateTransitionItems as any)[key];
            const isUnknownProperty = isUnknown(value, isJobInProgress);
            const valueForDisplay = isUnknownProperty ? "unknown" : value;
            const isLastItem = Object.keys(stateTransitionItems).length - 1 === index;
            const isNextUnknownProperty = isLastItem ? isJobInProgress : isUnknown((stateTransitionItems as any)[Object.keys(stateTransitionItems)[index + 1]], isJobInProgress);

            let stateTransitionLine: m.Child;
            if (!isLastItem) {
              stateTransitionLine = <div class={styles.transitionLine}/>;
            }

            let transitionCircleClass = `${styles.transitionCircle} ${styles.completed} ${(styles as any)[key.replace(' ', '').toLowerCase()]}`;
            if (isUnknownProperty) {
              transitionCircleClass = `${styles.transitionCircle} ${(styles as any)[key.replace(' ', '').toLowerCase()]}`;
            } else if (isNextUnknownProperty) {
              transitionCircleClass = styles.inProgress;
            }

            return [
              <div class={`${styles.tooltipKeyValuePair} ${styles.stateTransitionItem}`}>
                <span className={transitionCircleClass}/>
                <div className={styles.tooltipKey}>{key} :</div>
                <div className={classnames({[styles.unknownProperty]: isUnknownProperty})}>{valueForDisplay}</div>
              </div>,
              stateTransitionLine
            ];
          })
        }
        {
          Object.keys(tooltipItems).map(key => {
            const value = (tooltipItems as any)[key];
            const isUnknownProperty = isUnknown(value, isJobInProgress);
            const valueForDisplay = isUnknownProperty ? "unknown" : value;
            return (
              <div class={`${styles.tooltipKeyValuePair} ${styles.tooltipItem}`}>
                <div className={styles.tooltipKey}>{key} :</div>
                <div className={classnames({[styles.unknownProperty]: isUnknownProperty})}>{valueForDisplay}</div>
              </div>
            );
          })
        }
      </div>
    </div>);
  }
}

function isUnknown(val: string, isJobInProgress: boolean) {
  return isJobInProgress && (val.toLowerCase() === "00s" || val.toLowerCase() === "unknown");
}
