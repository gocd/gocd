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

import {MithrilViewComponent} from "jsx/mithril-component";
import m from "mithril";
import {ButtonIcon, Primary} from "views/components/buttons";
import {Delete, Edit, IconGroup} from "views/components/icons";
import {Table} from "views/components/table";
import {NotificationsAttrs} from "views/pages/new_preferences";
import styles from "./index.scss";

export class NotificationsWidget extends MithrilViewComponent<NotificationsAttrs> {

  view(vnode: m.Vnode<NotificationsAttrs, this>) {
    return <div data-test-id="notifications-widget" class={styles.notificationWrapper}>
      <div className={styles.formHeader}>
        <h3>Current Notification Filters</h3>
        <div className={styles.formButton}>
          <Primary icon={ButtonIcon.ADD}
                   dataTestId={"notification-filter-add"}
                   onclick={vnode.attrs.onAddFilter.bind(this)}>Add Notification Filter</Primary>
        </div>
      </div>

      <Table headers={['Pipeline', 'Stage', 'Event', 'Check-ins Matcher', '']}
             data={this.getTableData(vnode)}/>
    </div>;
  }

  private getTableData(vnode: m.Vnode<NotificationsAttrs, this>) {
    return vnode.attrs.notificationVMs().entity().map(
      (filter) => {
        return [
          filter.pipeline(),
          filter.stage(),
          filter.event().toString(),
          filter.matchCommits() ? 'Mine' : 'All',
          <IconGroup>
            <Edit data-test-id="notification-filter-edit"
                  title={'Edit notification filter'}
                  onclick={vnode.attrs.onEditFilter.bind(this, filter)}/>
            <Delete data-test-id="notification-filter-delete"
                    title={'Delete notification filter'}
                    onclick={vnode.attrs.onDeleteFilter.bind(this, filter)}/>
          </IconGroup>
        ];
      }
    );
  }
}
