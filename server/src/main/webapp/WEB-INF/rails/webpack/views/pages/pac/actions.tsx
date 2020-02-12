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

import {ApiResult} from "helpers/api_request_builder";
import {LocationHandler, WindowLocation} from "helpers/location_handler";
import {SparkRoutes} from "helpers/spark_routes";
import {MithrilViewComponent} from "jsx/mithril-component";
import m from "mithril";
import Stream from "mithril/stream";
import {ConfigReposCRUD} from "models/config_repos/config_repos_crud";
import {ConfigRepo} from "models/config_repos/types";
import * as Buttons from "views/components/buttons";
import css from "./styles.scss";

interface Attrs {
  configRepo: Stream<ConfigRepo>;
  loc?: LocationHandler;
  disabled?: boolean;
}

export class PacActions extends MithrilViewComponent<Attrs> {
  private location: LocationHandler = new WindowLocation();
  private globalError: Stream<string> = Stream();

  oninit(vnode: m.Vnode<Attrs, {}>) {
    if (vnode.attrs.loc) {
      this.location = vnode.attrs.loc;
    }
  }

  view(vnode: m.Vnode<Attrs>): m.Children | void | null {
    return (
      <div class={css.actions}>
        <Buttons.Cancel css={css} onclick={this.onCancel.bind(this)} small={false}>Cancel</Buttons.Cancel>

        <div class={css.finishBtnWrapper}>
          <div class={css.errorResponse}>{this.globalError()}</div>

          <Buttons.Primary css={css} onclick={this.onSave.bind(this, vnode.attrs.configRepo())} small={false} disabled={!!vnode.attrs.disabled}>Finish</Buttons.Primary>
        </div>
      </div>
    );
  }

  onCancel(event: Event): void {
    event.stopPropagation();
    this.location.go("/go/pipelines");
  }

  onSave(configRepo: ConfigRepo, event: Event): void {
    event.stopPropagation();
    event.preventDefault();
    this.clearErrors();

    if (configRepo.isValid()) {
      ConfigReposCRUD.create(configRepo).then((result: ApiResult<any>) => {
        result.do((s) => {
          this.location.go(SparkRoutes.ConfigRepoViewPath(configRepo.id()));
        }, (err) => {
          this.globalError(JSON.parse(err.body!).message);
        });

      });
    } else {
      this.globalError("Please fix the validation errors above before proceeding.");
    }
  }

  clearErrors() {
    this.globalError = Stream();
  }
}
