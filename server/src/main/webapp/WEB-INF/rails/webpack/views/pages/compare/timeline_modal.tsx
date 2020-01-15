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

import classnames from "classnames";
import {ErrorResponse} from "helpers/api_request_builder";
import m from "mithril";
import Stream from "mithril/stream";
import {PipelineHistory, PipelineInstance} from "models/compare/pipeline_instance";
import {PipelineInstanceCRUD} from "models/compare/pipeline_instance_crud";
import {stringOrUndefined} from "models/compare/pipeline_instance_json";
import {ButtonGroup, Cancel, Primary} from "views/components/buttons";
import {FlashMessage, MessageType} from "views/components/flash_message";
import linkStyles from "views/components/link/index.scss";
import {Modal, ModalState, Size} from "views/components/modal";
import {InstanceSelectionWidget} from "./instance_selection_widget";
import styles from "./modal.scss";
import {PipelineInstanceWidget} from "./pipeline_instance_widget";
import {StagesWidget} from "./stages/stages_widget";

export class TimelineModal extends Modal {
  errorMessage: Stream<string>                       = Stream();
  private readonly pipelineName: string;
  private readonly onInstanceSelection: (counter: number) => void;
  private history: Stream<PipelineHistory>           = Stream();
  private selectedInstance: Stream<PipelineInstance> = Stream();
  private service: ApiService;

  constructor(pipelineName: string, onInstanceSelection: (counter: number) => void, service: ApiService = new FetchHistoryService()) {
    super(Size.large);
    this.pipelineName             = pipelineName;
    this.service                  = service;
    this.closeModalOnOverlayClick = false;
    this.modalState               = ModalState.LOADING;
    this.onInstanceSelection      = onInstanceSelection;
    this.fetchHistory();
  }

  body(): m.Children {
    if (this.isLoading()) {
      return;
    }

    if (this.errorMessage()) {
      return <FlashMessage type={MessageType.alert} message={this.errorMessage()}/>;
    }

    const updateSelectedInstance = (instance: PipelineInstance, e: MouseEvent) => {
      e.stopPropagation();
      this.selectedInstance(instance);
    };

    const hasPreviousPage = this.history().previousLink !== undefined;
    const hasNextPage     = this.history().nextLink !== undefined;
    const onPreviousClick = (e: MouseEvent) => {
      e.stopPropagation();
      if (hasPreviousPage) {
        this.fetchHistory(this.history().previousLink);
      }
    };
    const onNextClick     = (e: MouseEvent) => {
      e.stopPropagation();
      if (hasNextPage) {
        this.fetchHistory(this.history().nextLink);
      }
    };

    return <div data-test-id="timeline-modal-body" class={styles.timelineModalContainer}>
      <div data-test-id="left-pane" class={styles.leftPanel}>
        <div class={styles.pipelineRunContainer}>
        {this.history().pipelineInstances.map((instance) => {
          const className = instance === this.selectedInstance() ? styles.selectedInstance : "";
          return <div data-test-id={InstanceSelectionWidget.dataTestId("instance", instance.counter())}
                      class={classnames(styles.pipelineRun, className)}
                      onclick={updateSelectedInstance.bind(this, instance)}>
            <span data-test-id="instance-counter">{instance.counter()}</span>
            <StagesWidget stages={instance.stages()}/>
          </div>;
        })}
        </div>
        <div data-test-id="pagination" class={styles.pagination}>
          <a title="Previous"
             role="button"
             className={classnames(linkStyles.inlineLink, styles.paginationLink,
                                   {[styles.disabled]: !hasPreviousPage})}
             href="#"
             onclick={onPreviousClick}>Previous</a>
          <a title="Next"
             role="button"
             class={classnames(linkStyles.inlineLink, styles.paginationLink,
                               {[styles.disabled]: !hasNextPage})}
             href="#"
             onclick={onNextClick}>Next</a>
        </div>
      </div>
      <div class={styles.rightPanel}>
        <PipelineInstanceWidget instance={this.selectedInstance()}/>
      </div>
    </div>;
  }

  buttons(): m.ChildArray {
    return [<ButtonGroup>
      <Cancel data-test-id="button-cancel" onclick={() => this.close()} disabled={this.isLoading()}>Cancel</Cancel>
      <Primary data-test-id="button-select-instance"
               onclick={this.compare.bind(this)}
               disabled={this.isLoading()}>Select this instance</Primary>
    </ButtonGroup>];
  }

  title(): string {
    return "Select a pipeline to compare";
  }

  private fetchHistory(link?: string) {
    this.service.fetchHistory(this.pipelineName, link,
                              (history) => {
                                this.history(history);
                                this.selectedInstance(this.history().pipelineInstances[0]);
                                this.modalState = ModalState.OK;
                              },
                              (errMsg) => {
                                this.errorMessage(errMsg);
                                this.modalState = ModalState.OK;
                              });
  }

  private compare(e: MouseEvent) {
    e.stopPropagation();
    this.onInstanceSelection(this.selectedInstance().counter());
    this.close();
  }
}

export interface ApiService {
  fetchHistory(pipelineName: string, link: stringOrUndefined,
               onSuccess: (data: PipelineHistory) => void,
               onError: (message: string) => void): void;
}

class FetchHistoryService implements ApiService {
  fetchHistory(pipelineName: string, link: stringOrUndefined,
               onSuccess: (data: PipelineHistory) => void,
               onError: (message: string) => void): void {

    PipelineInstanceCRUD.history(pipelineName, link).then((result) => {
      result.do((successResponse) => onSuccess(successResponse.body),
                (errorResponse: ErrorResponse) => onError(errorResponse.message));
    });
  }

}
