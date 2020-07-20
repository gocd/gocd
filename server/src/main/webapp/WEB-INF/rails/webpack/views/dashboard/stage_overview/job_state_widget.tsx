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
import {MithrilComponent} from "../../../jsx/mithril-component";
import {JobJSON} from "./models/types";

export interface Attrs {
  job: JobJSON;
}

export interface State {
  getState: (job: JobJSON) => string;
}

export class JobStateWidget extends MithrilComponent<Attrs, State> {
  oninit(vnode: m.Vnode<Attrs, State>) {
    vnode.state.getState = (job: JobJSON) => {
      switch (job.state) {
        case "Scheduled":
          return "Waiting for an agent";
        case "Assigned":
        case "Preparing":
        case "Building":
          return "Building";
        case "Completing":
          return "Uploading artifacts";
        case"Completed":
          return `${job.result}`;
      }
      return "Waiting for agent";
    };
  }

  view(vnode: m.Vnode<Attrs, State>): m.Children | void | null {
    return <div data-test-id="job-state-container">
      {vnode.state.getState(vnode.attrs.job)}
    </div>;
  }

}
