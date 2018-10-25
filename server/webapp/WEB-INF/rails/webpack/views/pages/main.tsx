/*
 * Copyright 2018 ThoughtWorks, Inc.
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

import * as m from 'mithril';
import {MithrilComponent} from "../../jsx/mithril-component";
import {ModalManager} from "../components/modal/modal_manager";
import {Attrs as SiteFooterAttrs, SiteFooter} from "./partials/site_footer";
import {Attrs as SiteHeaderAttrs, SiteHeader} from "./partials/site_header";

const styles     = require('./main.scss');

interface Attrs {
  headerData: SiteHeaderAttrs;
  footerData: SiteFooterAttrs;
}

export class MainPage extends MithrilComponent<Attrs> {
  oninit() {
    ModalManager.onPageLoad();
  }

  view(vnode: m.Vnode<Attrs>) {
    return (
      <div class={styles.page}>
        <div class={styles.pagewrap}>
          <SiteHeader {...vnode.attrs.headerData}/>
          <main className={styles.mainContainer}>
            {vnode.children}
          </main>
        </div>
        <footer class={styles.sitefooter}>
          <SiteFooter {...vnode.attrs.footerData} />
        </footer>
      </div>
    );

  }
}

