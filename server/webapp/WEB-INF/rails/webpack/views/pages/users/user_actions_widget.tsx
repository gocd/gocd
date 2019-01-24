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
import {MithrilViewComponent} from "jsx/mithril-component";
import * as m from "mithril";
import {Stream} from "mithril/stream";
import * as stream from "mithril/stream";
import {GoCDRole, Roles} from "models/roles/roles_new";
import {TriStateCheckbox} from "models/tri_state_checkbox";
import {UserFilters} from "models/users/user_filters";
import {Users} from "models/users/users";
import {
  ButtonGroup,
  ButtonIcon,
  Default,
  Dropdown,
  DropdownAttrs,
  Link,
  Primary,
  Secondary
} from "views/components/buttons";
import {Counts, CountsAttr} from "views/components/counts";
import {Form} from "views/components/forms/form";
import {
  CheckboxField,
  QuickAddField,
  SearchField,
  TriStateCheckboxField
} from "views/components/forms/input_fields";
import {DeleteOperation, DisableOperation, EnableOperation} from "views/pages/page_operations";
import * as styles from "./index.scss";

const classnames = bind(styles);

export interface HasRoleSelection {
  rolesSelection: Stream<Map<GoCDRole, TriStateCheckbox>>;
}

interface MakeAdminOperation<T> {
  onMakeAdmin: (obj: T, e: MouseEvent) => void;
}

interface RemoveAdminOperation<T> {
  onRemoveAdmin: (obj: T, e: MouseEvent) => void;
}

export interface RolesViewAttrs extends HasRoleSelection {
  initializeRolesDropdownAttrs: () => void;
  onRolesUpdate: (rolesSelection: Map<GoCDRole, TriStateCheckbox>, users: Users) => void;
  onRolesAdd: (roleName: string, users: Users) => void;
  roles: Stream<Roles>;
  users: () => Users;
  roleNameToAdd: Stream<string>;
  showRoles: Stream<boolean>;
}

export interface FiltersViewAttrs {
  showFilters: Stream<boolean>;
  userFilters: Stream<UserFilters>;
  roles: Stream<Roles>;
}

export interface State extends RolesViewAttrs, FiltersViewAttrs, EnableOperation<Users>, DisableOperation<Users>, DeleteOperation<Users>, MakeAdminOperation<Users>, RemoveAdminOperation<Users> {
  noAdminsConfigured: Stream<boolean>;
}

class FiltersView extends Dropdown<FiltersViewAttrs> {
  doRenderDropdownContent(vnode: m.Vnode<DropdownAttrs & FiltersViewAttrs>) {
    return (
      <div data-test-id="filters-view"
           data-test-visible={`${vnode.attrs.showFilters()}`}
           className={classnames({[styles.hidden]: !vnode.attrs.showFilters()},
                                 styles.filterDropdownContent)}>
        <header className={classnames(styles.filterHeader)}>
          <h4 data-test-id="filter-by-heading" className={classnames(styles.filterByHeading)}> Filter By </h4>
          <Link data-test-id="reset-filter-btn"
                onclick={vnode.attrs.userFilters().resetFilters.bind(vnode.attrs.userFilters())}>
            Reset Filters
          </Link>
        </header>
        <div className={classnames(styles.filtersBody)}>
          <div className={classnames(styles.filterItems)}>
            <h4 className={classnames(styles.filterItemsHead)}
                data-test-id="filter-by-privileges-heading">Privileges</h4>
            <Form compactForm={true} data-test-id="filter-by-privileges">
              <CheckboxField label="System Administrators"
                             property={vnode.attrs.userFilters().superAdmins}/>
              <CheckboxField label="Normal Users"
                             property={vnode.attrs.userFilters().normalUsers}/>
            </Form>
          </div>
          <div className={classnames(styles.filterItems)}>
            <h4 className={classnames(styles.filterItemsHead)} data-test-id="filter-by-users-state-heading">
              User state
            </h4>
            <Form compactForm={true} data-test-id="filter-by-states">
              <CheckboxField label="Enabled"
                             property={vnode.attrs.userFilters().enabledUsers}/>
              <CheckboxField label="Disabled"
                             property={vnode.attrs.userFilters().disabledUsers}/>
            </Form>
          </div>

          <div className={classnames(styles.filterItems)}>
            <h4 className={classnames(styles.filterItemsHead)} data-test-id="filter-by-role-heading">Roles</h4>
            <div data-test-id="filter-by-roles" className={styles.filterByRoles}>
              <Form compactForm={true}>
                {this.renderRoles(vnode)}
              </Form>
            </div>
          </div>
        </div>
      </div>)
      ;
  }

  protected classNames(): string {
    return classnames(styles.filterDropdown);
  }

  protected doRenderButton(vnode: m.Vnode<DropdownAttrs & FiltersViewAttrs>): m.Children {
    let filtersCount = "";

    if (vnode.attrs.userFilters().anyFiltersApplied()) {
      filtersCount = "(" + vnode.attrs.userFilters().filtersCount() + ")";
    }

    return (
      <Default dropdown={true} data-test-id="filters-btn"
               onclick={(e) => {
                 this.toggleDropdown(vnode, e);
               }}
               icon={ButtonIcon.FILTER}>Filters <span>{filtersCount}</span></Default>
    );
  }

  private renderRoles(vnode: m.Vnode<DropdownAttrs & FiltersViewAttrs>) {
    return vnode.attrs.roles().map((role) => {
      const usersFilters = vnode.attrs.userFilters();
      return <CheckboxField label={role.name()}
                            property={usersFilters.roleSelectionFor(role.name())}/>;
    });
  }

}

class RolesDropdown extends Dropdown<RolesViewAttrs> {

  toggleDropdown(vnode: m.Vnode<DropdownAttrs & RolesViewAttrs>, e: MouseEvent) {
    super.toggleDropdown(vnode, e);
    vnode.attrs.initializeRolesDropdownAttrs();
  }

  doRenderDropdownContent(vnode: m.Vnode<DropdownAttrs & RolesViewAttrs>): m.Children {
    const classNames = classnames({
                                    [styles.hidden]: !vnode.attrs.show(),
                                  }, styles.rolesDropdownContent);
    const checkboxes = vnode.attrs.roles().map((role) => {
      const triStateCheckbox = vnode.attrs.rolesSelection().get(role as GoCDRole);
      if (!triStateCheckbox) {
        return;
      }

      return <TriStateCheckboxField label={role.name()} property={stream(triStateCheckbox)}/>;
    });

    return (
      <div className={classNames}>
        <div className={styles.rolesList}>
          <Form compactForm={true}>
            {checkboxes}
          </Form>
        </div>
        <Form compactForm={true}>
          <QuickAddField property={vnode.attrs.roleNameToAdd} placeholder="Add role"
                         onclick={vnode.attrs.onRolesAdd.bind(this, vnode.attrs.roleNameToAdd(), vnode.attrs.users())}
                         buttonDisableReason={"Please type the role name to add."}
          />
        </Form>
        <Primary onclick={vnode.attrs.onRolesUpdate.bind(this,
                                                         vnode.attrs.rolesSelection(),
                                                         vnode.attrs.users())}>Apply</Primary>
      </div>
    );
  }

  protected doRenderButton(vnode: m.Vnode<DropdownAttrs & RolesViewAttrs>) {
    return <Secondary dropdown={true}
                      disabled={!vnode.attrs.users().anyUserSelected()}
                      onclick={(e) => {
                        this.toggleDropdown(vnode, e);
                      }}>
      Roles
    </Secondary>;
  }
}

export class UsersActionsWidget extends MithrilViewComponent<State> {
  view(vnode: m.Vnode<State>) {
    const counts = [
      {
        count: vnode.attrs.users().totalUsersCount(),
        label: "Total"
      },
      {
        count: vnode.attrs.users().enabledUsersCount(),
        label: "Enabled",
        color: "green"
      },
      {
        count: vnode.attrs.users().disabledUsersCount(),
        label: "Disabled",
        color: "red"
      }
    ] as CountsAttr[];

    return <div className={classnames(styles.userManagementHeader)}>
      <div className={classnames(styles.userActionsAndCounts)}>
        <div className={classnames(styles.userActions)}>
          <ButtonGroup>
            <Secondary onclick={vnode.attrs.onEnable.bind(vnode.attrs, vnode.attrs.users())}
                       disabled={!vnode.attrs.users().anyUserSelected()}>Enable</Secondary>
            <Secondary onclick={vnode.attrs.onDisable.bind(vnode.attrs, vnode.attrs.users())}
                       disabled={!vnode.attrs.users().anyUserSelected()}>Disable</Secondary>
            <Secondary onclick={vnode.attrs.onDelete.bind(vnode.attrs, vnode.attrs.users())}
                       disabled={!vnode.attrs.users().anyUserSelected()}>Delete</Secondary>
            <RolesDropdown {...vnode.attrs} show={vnode.attrs.showRoles}/>
          </ButtonGroup>
        </div>
        <Counts counts={counts} dataTestId="users"/>
      </div>
      <div className={classnames(styles.userFilters)}>
        <SearchField property={vnode.attrs.userFilters().searchText} dataTestId={"search-box"}/>
        <FiltersView {...vnode.attrs} show={vnode.attrs.showFilters}/>
      </div>
    </div>;
  }
}
