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
import {ErrorResponse} from "helpers/api_request_builder";
import {SparkRoutes} from "helpers/spark_routes";
import {MithrilViewComponent} from "jsx/mithril-component";
import m from "mithril";
import Stream from "mithril/stream";
import {PipelineInstance, PipelineInstances, Stage} from "models/compare/pipeline_instance";
import {PipelineInstanceCRUD} from "models/compare/pipeline_instance_crud";
import s from "underscore.string";
import {Warning} from "views/components/icons";
import styles from "./index.scss";
import {PipelineInstanceWidget} from "./pipeline_instance_widget";
import {SelectInstanceWidget} from "./select_instance_widget";
import {StagesWidget} from "./stages/stages_widget";

type StringOrNumber = string | number;

export interface InstanceAttrs {
  instance: PipelineInstance;
  onInstanceChange: (counter: number) => void;
  dataTestId?: string;
}

export class InstanceSelectionWidget extends MithrilViewComponent<InstanceAttrs> {
  readonly showSuggestions: Stream<boolean> = Stream();

  static dataTestId(...parts: StringOrNumber[]) {
    return s.slugify(parts.join("-").trim().toLowerCase());
  }

  view(vnode: m.Vnode<InstanceAttrs, this>): m.Children | void | null {
    const dataTestId = vnode.attrs.dataTestId ? vnode.attrs.dataTestId : InstanceSelectionWidget.dataTestId("instance", "selection", "widget", vnode.attrs.instance.counter());
    return <div
      data-test-id={dataTestId}
      class={styles.instanceWrapper}>
      <SelectInstanceWidget show={this.showSuggestions} apiService={new PipelineInstanceService()} {...vnode.attrs}/>
      {this.getStagesOrWarning(vnode)}
    </div>;
  }

  private getStagesOrWarning(vnode: m.Vnode<InstanceAttrs, this>) {
    let bisectWarningMsg;
    if (vnode.attrs.instance.isBisect()) {
      bisectWarningMsg = <div data-test-id="warning" class={styles.warning}>
        <Warning iconOnly={true}/>
        This pipeline instance was triggered with a non-sequential material revision.
      </div>;
    }
    return <div>
      <StagesWidget stages={vnode.attrs.instance.stages()} onClick={this.onStageClick.bind(this, vnode)}/>
      <span data-test-id="triggered-by" className={styles.label}>
        Triggered by {vnode.attrs.instance.buildCause().getApprover()} on {PipelineInstanceWidget.getTimeToDisplay(vnode.attrs.instance.scheduledDate())}
      </span>
      {bisectWarningMsg}
    </div>;
  }

  private onStageClick(vnode: m.Vnode<InstanceAttrs, this>, stage: Stage) {
    window.location.href = SparkRoutes.getStageDetailsPageUrl(vnode.attrs.instance.name(), vnode.attrs.instance.counter(), stage.name(), stage.counter());
  }
}

export interface ApiService {
  getMatchingInstances(pipelineName: string, pattern: string,
                       onSuccess: (data: PipelineInstances) => void,
                       onError: (message: string) => void): void;
}

class PipelineInstanceService implements ApiService {
  getMatchingInstances(pipelineName: string, pattern: string,
                       onSuccess: (data: PipelineInstances) => void,
                       onError: (message: string) => void): void {

    PipelineInstanceCRUD.matchingInstances(pipelineName, pattern).then((result) => {
      result.do((successResponse) => onSuccess(successResponse.body),
                (errorResponse: ErrorResponse) => onError(errorResponse.message));
    });
  }

}
