/*
 * Copyright 2022 ThoughtWorks, Inc.
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

import {RestyleAttrs, RestyleComponent} from "jsx/mithril-component";
import m from "mithril";
import Stream from "mithril/stream";
import {Errors} from "models/mixins/errors";
import * as defaultStyles from "views/pages/pipelines/server_errors.scss";

type Styles = typeof defaultStyles;

interface Attrs extends RestyleAttrs<Styles> {
  details: Stream<Errors>;
  message: Stream<string>;
}

export class ServerErrors extends RestyleComponent<Styles, Attrs> {
  css: Styles = defaultStyles;

  view(vnode: m.Vnode<Attrs>) {
    return <div class={this.css.errorResponse}>
      {this.message(vnode)}
      {this.details(vnode)}
    </div>;
  }

  message(vnode: m.Vnode<Attrs>) {
    const unmatched = vnode.attrs.details();
    return <span>
      { (unmatched && unmatched.keys().length) ? vnode.attrs.message() + ": " : vnode.attrs.message() }
    </span>;
  }

  details(vnode: m.Vnode<Attrs>) {
    const unmatched = vnode.attrs.details();
    if (unmatched && unmatched.keys().length) {
      return <ol>
        { unmatched.keys().map((key) => <li>{`${key}: ${unmatched.errorsForDisplay(key)}`}</li>) }
      </ol>;
    }
  }
}
