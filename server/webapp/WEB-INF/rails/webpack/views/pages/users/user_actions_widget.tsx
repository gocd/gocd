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

import {bind} from "classnames/bind";
import {MithrilComponent, MithrilViewComponent} from "jsx/mithril-component";
import * as m from "mithril";
import {Stream} from "mithril/stream";
import * as stream from "mithril/stream";
import {UserFilters} from "models/users/user_filters";
import {ButtonGroup, ButtonIcon, Dropdown, Link, Secondary} from "views/components/buttons";
import {CheckboxField, SearchField} from "views/components/forms/input_fields";
import {Attrs} from "views/pages/users/users_widget";
import * as styles from "./index.scss";

const classnames = bind(styles);

export interface State {
  toggleFiltersView: () => void;
  showFilters: Stream<boolean>;
}

export interface FiltersViewAttrs {
  showFilters: Stream<boolean>;
  userFilters: UserFilters;
}

export class FiltersView extends MithrilViewComponent<FiltersViewAttrs> {
  view(vnode: m.Vnode<FiltersViewAttrs>) {
    return <div data-test-id="filters-view"
                data-test-visible={`${vnode.attrs.showFilters()}`}
                className={classnames({hidden: !vnode.attrs.showFilters()},
                                      styles.filterView,
                                      styles.filterDropdownContent)}>
      <header className={classnames(styles.filterHeader)}>
        <h4 data-test-id="filter-by-heading" className={classnames(styles.filterByHeading)}> Filter By </h4>
        <Link data-test-id="reset-filter-btn"
              onclick={vnode.attrs.userFilters.resetFilters.bind(vnode.attrs.userFilters)}>
          Reset Filters
        </Link>
      </header>
      <div className={classnames(styles.filtersBody)}>
        <div className={classnames(styles.filterItems)}>
          <h4 className={classnames(styles.filterItemsHead)} data-test-id="filter-by-privileges-heading">Privilages</h4>
          <div data-test-id="filter-by-privileges">
            <CheckboxField label="Super Administrators"
                           property={vnode.attrs.userFilters.superAdmins}/>
            <CheckboxField label="Normal Users"
                           property={vnode.attrs.userFilters.normalUsers}/>
          </div>
        </div>
        <div className={classnames(styles.filterItems)}>
          <h4 className={classnames(styles.filterItemsHead)} data-test-id="filter-by-users-state-heading">User
            state</h4>
          <div data-test-id="filter-by-privileges">
            <CheckboxField label="Enabled"
                           property={vnode.attrs.userFilters.enabledUsers}/>
            <CheckboxField label="Disabled"
                           property={vnode.attrs.userFilters.disabledUsers}/>
          </div>
        </div>
      </div>
    </div>;
  }
}

export class UsersActionsWidget extends MithrilComponent<Attrs, State> {
  oninit(vnode: m.Vnode<Attrs, State>) {
    vnode.state.showFilters = stream(false);

    vnode.state.toggleFiltersView = () => {
      vnode.state.showFilters(!vnode.state.showFilters());
    };
  }

  view(vnode: m.Vnode<Attrs, State>) {
    return <div className={classnames(styles.userManagementHeader)}>
      <div className={classnames(styles.userActions)}>
        <ButtonGroup>
          <Secondary onclick={vnode.attrs.onEnable.bind(vnode.attrs, vnode.attrs.users())}
                     disabled={!vnode.attrs.users().anyUserSelected()}>Enable</Secondary>
          <Secondary onclick={vnode.attrs.onDisable.bind(vnode.attrs, vnode.attrs.users())}
                     disabled={!vnode.attrs.users().anyUserSelected()}>Disable</Secondary>
          <Secondary onclick={vnode.attrs.onDelete.bind(vnode.attrs, vnode.attrs.users())}
                     disabled={!vnode.attrs.users().anyUserSelected()}>Delete</Secondary>
        </ButtonGroup>
      </div>
      <div className={classnames(styles.userCount)}>
        <ul data-test-id="users-count" className={classnames(styles.userCountList)}>
          <li>Total: <span data-test-id="all-user-count" className={classnames(styles.countAll)}>{vnode.attrs.users()
                                                                                                       .totalUsersCount()}</span>
          </li>
          <li>Enabled: <span data-test-id="enabled-user-count"
                             className={classnames(styles.countEnabled)}>{vnode.attrs.users()
                                                                               .enabledUsersCount()}</span></li>
          <li>Disabled: <span data-test-id="disabled-user-count"
                              className={classnames(styles.countDisabled)}>{vnode.attrs.users()
                                                                                 .disabledUsersCount()}</span></li>
        </ul>
      </div>
      <div className={classnames(styles.userFilters)}>
        <SearchField property={vnode.attrs.userFilter().searchText} dataTestId={"search-box"}/>
        <div className={classnames(styles.filterDropdown)}>
          <Dropdown data-test-id="filters-btn" onclick={vnode.state.toggleFiltersView}
                    icon={ButtonIcon.FILTER}>Filters</Dropdown>
          <FiltersView showFilters={vnode.state.showFilters} userFilters={vnode.attrs.userFilter()}/>
        </div>
      </div>
    </div>;
  }
}
