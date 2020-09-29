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

import m from "mithril";
import Stream from "mithril/stream";
import * as Icons from "views/components/icons";
import {ErrorResponse} from "../../../helpers/api_request_builder";
import {MithrilComponent} from "../../../jsx/mithril-component";
import {FlashMessage, FlashMessageModelWithTimeout, MessageType} from "../../components/flash_message";
import {SelectField, SelectFieldOptions} from "../../components/forms/input_fields";
import {Link} from "../../components/link";
import * as styles from "./index.scss";
import {StageInstance} from "./models/stage_instance";
import {StageOverviewViewModel} from "./models/stage_overview_view_model";
import {StageState} from "./models/types";

interface StageHeaderAttrs {
  stageName: string;
  stageCounter: string | number;
  pipelineName: string;
  pipelineCounter: string | number;
  stageInstanceFromDashboard: any;
  canAdminister: boolean;
  templateName: string | undefined | null;
  pollingInterval?: number;
  flashMessage: FlashMessageModelWithTimeout;
  stageInstance: Stream<StageInstance>;
  inProgressStageFromPipeline: Stream<any | undefined>;
  stageOverviewVM: Stream<StageOverviewViewModel>;
  userSelectedStageCounter: Stream<string | number>;
}

interface StageHeaderState {
  isSettingsHover: Stream<boolean>;
  onStageCounterDropdownChange: (vnode: m.Vnode<StageHeaderAttrs, StageHeaderState>) => any;
  isLoading: Stream<boolean>;
}

export class StageHeaderWidget extends MithrilComponent<StageHeaderAttrs, StageHeaderState> {
  oninit(vnode: m.Vnode<StageHeaderAttrs, StageHeaderState>) {
    vnode.state.isSettingsHover = Stream<boolean>(false);
    vnode.state.isLoading = Stream<boolean>(false);

    vnode.state.onStageCounterDropdownChange = (vnode: m.Vnode<StageHeaderAttrs, StageHeaderState>) => {
      vnode.state.isLoading(true);

      // pass in the stage state as passed/failed, this value is just to denote whether the current stage instance has completed.
      StageOverviewViewModel.initialize(vnode.attrs.pipelineName, vnode.attrs.pipelineCounter, vnode.attrs.stageName, vnode.attrs.userSelectedStageCounter(), StageState.Passed, vnode.attrs.pollingInterval)
        .then((result) => {
          let stageResult = result.stageInstance().result();
          if (stageResult === "Unknown") {
            stageResult = "Building";
          }
          vnode.attrs.stageOverviewVM().stopRepeater();
          vnode.attrs.stageInstanceFromDashboard.status = stageResult;
          vnode.attrs.stageOverviewVM(result);
        }).finally(() => {
        vnode.state.isLoading(false);
      });
    };
  }

  view(vnode: m.Vnode<StageHeaderAttrs, StageHeaderState>): m.Children | void | null {
    const stageDetailsPageLink = `/go/pipelines/${vnode.attrs.pipelineName}/${vnode.attrs.pipelineCounter}/${vnode.attrs.stageName}/${vnode.attrs.userSelectedStageCounter()}`;

    let canceledBy: m.Child, dummyContainer;
    if (vnode.attrs.stageInstance().isCancelled()) {
      canceledBy = (<div class={styles.cancelledByWrapper} data-test-id="cancelled-by-wrapper">
        <div data-test-id="cancelled-by-container" className={styles.triggeredByContainer}>
          Cancelled by <span className={styles.triggeredByAndOn}>{vnode.attrs.stageInstance().cancelledBy()}</span>
        </div>
        <div data-test-id="cancelled-on-container" className={styles.triggeredByContainer}>
          on <span title={vnode.attrs.stageInstance().cancelledOnServerTime()}
                   className={styles.triggeredByAndOn}>{vnode.attrs.stageInstance().cancelledOn()}</span> Local Time
        </div>
      </div>);

      dummyContainer = <div className={styles.dummyContainerAboveStageDetailsLink}/>;
    }

    let optionalFlashMessage: m.Child;
    if (vnode.attrs.flashMessage.hasMessage()) {
      optionalFlashMessage = (
        <FlashMessage dataTestId="stage-overview-flash-message" message={vnode.attrs.flashMessage.message}
                      type={vnode.attrs.flashMessage.type}/>);
    }

    const stageSettingsUrl = vnode.attrs.templateName
      ? `/go/admin/templates/${vnode.attrs.templateName}/edit#!${vnode.attrs.templateName}/${vnode.attrs.stageName}/stage_settings`
      : `/go/admin/pipelines/${vnode.attrs.pipelineName}/edit#!${vnode.attrs.pipelineName}/${vnode.attrs.stageName}/stage_settings`;

    let stageSettings: m.Child;
    if (vnode.attrs.canAdminister) {
      stageSettings = (<div className={styles.stageSettings}>
        <Icons.Settings iconOnly={true} data-test-url={stageSettingsUrl} onclick={() => window.open(stageSettingsUrl)}/>
      </div>);
    } else {
      const disabledEditMessage = `You dont have permissions to edit the stage.`;
      stageSettings = (<div className={styles.stageSettings}>
        <Icons.Settings iconOnly={true} disabled={true} onmouseover={() => vnode.state.isSettingsHover(true)}
                        onmouseout={() => vnode.state.isSettingsHover(false)}/>
        <span
          class={`${styles.tooltipMessage} ${!vnode.state.isSettingsHover() && styles.hidden}`}>{disabledEditMessage}</span>
      </div>);
    }

    let spinner: m.Children;
    if (vnode.state.isLoading()) {
      spinner = <Icons.Spinner iconOnly={true}/>;
    }

    let stageCounterView: m.Children;
    if ((+vnode.attrs.stageCounter) > 1) {

      const items = [];
      for (let i = 1; i <= (+vnode.attrs.stageCounter); i++) {
        items.push(`${i}`);
      }

      stageCounterView = (<div class={styles.stageCounterDropdownWrapper} data-test-id="stage-counter-dropdown">
        <SelectField property={vnode.attrs.userSelectedStageCounter as Stream<string>}
                     onchange={vnode.state.onStageCounterDropdownChange.bind(vnode.state, vnode)}>
          <SelectFieldOptions selected={`${vnode.attrs.userSelectedStageCounter()}`} items={items}/>
        </SelectField>
        {spinner}
      </div>);
    } else {
      stageCounterView = (
        <div data-test-id="stage-counter" className={styles.stageName}>{vnode.attrs.stageCounter}</div>);
    }

    return <div data-test-id="stage-overview-header" class={styles.stageHeaderContainer}>
      {optionalFlashMessage}
      <div data-test-id="stage-name-and-operations-container" class={styles.flexBox}>
        <div class={styles.stageNameContainer}>
          <div data-test-id="pipeline-name-container">
            <div className={styles.stageNameTitle}>Pipeline</div>
            <div className={styles.stageName}>{vnode.attrs.pipelineName}</div>
          </div>
          <Icons.AngleDoubleRight iconOnly={true}/>
          <div data-test-id="pipeline-instance-container">
            <div className={styles.stageNameTitle}>Instance</div>
            <div className={styles.stageName}>{vnode.attrs.pipelineCounter}</div>
          </div>
          <Icons.AngleDoubleRight iconOnly={true}/>
          <div data-test-id="stage-name-container">
            <div className={styles.stageNameTitle}>Stage</div>
            <div className={styles.stageName}>{vnode.attrs.stageName}</div>
          </div>
          <Icons.AngleDoubleRight iconOnly={true}/>
          <div data-test-id="stage-instance-container">
            <div className={styles.stageNameTitle}>Instance</div>
            {stageCounterView}
          </div>
        </div>
        <div data-test-id="stage-operations-container" class={styles.stageOperationButtonGroup}>
          <StageTriggerOrCancelButtonWidget stageInstance={vnode.attrs.stageInstance}
                                            userSelectedStageCounter={vnode.attrs.userSelectedStageCounter}
                                            inProgressStageFromPipeline={vnode.attrs.inProgressStageFromPipeline}
                                            flashMessage={vnode.attrs.flashMessage}
                                            stageInstanceFromDashboard={vnode.attrs.stageInstanceFromDashboard}/>
          {stageSettings}
        </div>
      </div>

      <div data-test-id="stage-trigger-and-timing-container"
           class={`${styles.flexBox} ${styles.stageTriggerAndTimingContainer}`}>
        <div data-test-id="stage-trigger-by-container">
          {canceledBy}
          <div data-test-id="triggered-by-container" class={styles.triggeredByContainer}>
            Triggered by <span class={styles.triggeredByAndOn}>{vnode.attrs.stageInstance().triggeredBy()}</span>
          </div>
          <div data-test-id="triggered-on-container" className={styles.triggeredByContainer}>
            on <span title={vnode.attrs.stageInstance().triggeredOnServerTime()}
                     className={styles.triggeredByAndOn}>{vnode.attrs.stageInstance().triggeredOn()}</span> Local Time
            <span className={styles.durationSeparator}>|</span>
            Duration: <span className={styles.triggeredByAndOn}>{vnode.attrs.stageInstance().stageDuration()}</span>
          </div>
        </div>
        <div data-test-id="stage-details-page-link" class={styles.stageDetailsPageLink}>
          {dummyContainer}
          <Link href={stageDetailsPageLink} externalLinkIcon={true} target={"_blank"}>Go to Stage Details Page</Link>
        </div>
      </div>
    </div>;
  }
}

interface StageTriggerOrCancelButtonAttrs {
  stageInstance: Stream<StageInstance>;
  stageInstanceFromDashboard: any;
  flashMessage: FlashMessageModelWithTimeout;
  inProgressStageFromPipeline: Stream<any | undefined>;
  userSelectedStageCounter: Stream<string | number>;
}

interface StageTriggerOrCancelButtonState {
  getResultHandler: (attrs: StageTriggerOrCancelButtonAttrs, shouldIncrement: boolean) => any;
  isTriggerHover: Stream<boolean>;
}

class StageTriggerOrCancelButtonWidget extends MithrilComponent<StageTriggerOrCancelButtonAttrs, StageTriggerOrCancelButtonState> {
  oninit(vnode: m.Vnode<StageTriggerOrCancelButtonAttrs, StageTriggerOrCancelButtonState>) {
    vnode.state.isTriggerHover = Stream<boolean>(false);
    vnode.state.getResultHandler = (attrs, shouldIncrement: boolean) => {
      return (result: any) => {
        result.do((successResponse: any) => {
          attrs.flashMessage.setMessage(MessageType.success, JSON.parse(successResponse.body).message);
          if (shouldIncrement) {
            vnode.attrs.userSelectedStageCounter(`${(+vnode.attrs.userSelectedStageCounter()) + 1}`);
          }
        }, (errorResponse: ErrorResponse) => {
          attrs.flashMessage.setMessage(MessageType.alert, JSON.parse(errorResponse.body!).message);
        });
      };
    };
  }

  view(vnode: m.Vnode<StageTriggerOrCancelButtonAttrs, StageTriggerOrCancelButtonState>): m.Children | void | null {
    let disabled = !vnode.attrs.stageInstanceFromDashboard.canOperate;
    let disabledClass = (vnode.state.isTriggerHover() && disabled) ? '' : styles.hidden;

    if (vnode.attrs.stageInstance().isCompleted()) {
      let disabledMessage = `You dont have permissions to rerun the stage.`;

      if (vnode.attrs.inProgressStageFromPipeline()) {
        disabled = true;
        disabledClass = (vnode.state.isTriggerHover() && disabled) ? '' : styles.hidden;
        disabledMessage = `Can not rerun current stage. Stage '${vnode.attrs.inProgressStageFromPipeline().name}' from the pipeline is still in progress.`;
      }

      return <div class={styles.cancelOrRerunStageIcon} data-test-id="rerun-stage">
        <Icons.Repeat iconOnly={true} disabled={disabled}
                      title="Rerun stage"
                      onmouseover={() => vnode.state.isTriggerHover(true)}
                      onmouseout={() => vnode.state.isTriggerHover(false)}
                      onclick={() => {
                        vnode.attrs.stageInstance().runStage().then(vnode.state.getResultHandler(vnode.attrs, true));
                      }}/>
        <span className={`${styles.tooltipMessage} ${disabledClass}`}>{disabledMessage}</span>
      </div>;
    }

    const disabledMessage = `You dont have permissions to cancel the stage.`;
    return <div class={styles.cancelOrRerunStageIcon} data-test-id="cancel-stage">
      <Icons.CancelStage iconOnly={true} disabled={disabled}
                         title="Cancel stage"
                         onmouseover={() => vnode.state.isTriggerHover(true)}
                         onmouseout={() => vnode.state.isTriggerHover(false)}
                         onclick={() => {
                           vnode.attrs.stageInstance().cancelStage().then(vnode.state.getResultHandler(vnode.attrs, false));
                         }}/>
      <span className={`${styles.tooltipMessage} ${disabledClass}`}>{disabledMessage}</span>
    </div>;
  }
}
