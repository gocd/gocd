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
import {ErrorResponse} from "helpers/api_request_builder";
import {MithrilViewComponent} from "jsx/mithril-component";
import m from "mithril";
import Stream from "mithril/stream";
import {PipelineInstance, PipelineInstances} from "models/compare/pipeline_instance";
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
}

export class InstanceSelectionWidget extends MithrilViewComponent<InstanceAttrs> {

  static dataTestId(...parts: StringOrNumber[]) {
    return s.slugify(parts.join("-").trim().toLowerCase());
  }

  view(vnode: m.Vnode<InstanceAttrs, this>): m.Children | void | null {
    return <div
      data-test-id={InstanceSelectionWidget.dataTestId("instance", "selection", "widget", vnode.attrs.instance.counter())}
      class={styles.instanceWrapper}>
      <SelectInstanceWidget show={Stream(false)} apiService={new PipelineInstanceService()} {...vnode.attrs}/>
      {this.getStagesOrWarning(vnode)}
    </div>;
  }

  private getStagesOrWarning(vnode: m.Vnode<InstanceAttrs, this>) {
    if (vnode.attrs.instance.isBisect()) {
      return <div data-test-id="warning" class={styles.warning}><Warning iconOnly={true}/>This pipeline instance cannot
        be used to perform a comparison because it was triggered with a non-sequential material revision.</div>;
    }
    return <div>
      <StagesWidget stages={vnode.attrs.instance.stages()}/>
      <span data-test-id="triggered-by" className={styles.label}>
        Triggered by {vnode.attrs.instance.buildCause().getApprover()} on {PipelineInstanceWidget.getTimeToDisplay(vnode.attrs.instance.scheduledDate())}
      </span>
    </div>;
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
