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

import {bind} from "classnames/bind";
import {MithrilViewComponent} from "jsx/mithril-component";
import m from "mithril";
import Stream from "mithril/stream";
import {GoCDRole, Roles} from "models/roles/roles";
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
import {DeleteOperation, DisableOperation, EnableOperation, OperationState} from "views/pages/page_operations";
import styles from "./index.scss";

const classnames = bind(styles);

export interface HasRoleSelection {
  rolesSelection: Stream<Map<GoCDRole, TriStateCheckbox>>;
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

interface AjaxOperationMonitor {
  operationState: Stream<OperationState>;
}

export type Attrs = RolesViewAttrs & FiltersViewAttrs & EnableOperation<Users> & DisableOperation<Users> &
  DeleteOperation<Users> & AjaxOperationMonitor;

class FiltersView extends Dropdown<FiltersViewAttrs> {
  doRenderDropdownContent(vnode: m.Vnode<DropdownAttrs & FiltersViewAttrs>) {
    return (
      <div data-test-id="filters-view"
           data-test-visible={`${vnode.attrs.showFilters()}`}
           class={classnames({[styles.hidden]: !vnode.attrs.showFilters()},
                                 styles.filterDropdownContent)}>
        <header class={classnames(styles.filterHeader)}>
          <h4 data-test-id="filter-by-heading" class={classnames(styles.filterByHeading)}> Filter By </h4>
          <Link data-test-id="reset-filter-btn"
                onclick={vnode.attrs.userFilters().resetFilters.bind(vnode.attrs.userFilters())}>
            Reset Filters
          </Link>
        </header>
        <div class={classnames(styles.filtersBody)}>
          <div class={classnames(styles.filterItems)}>
            <h4 class={classnames(styles.filterItemsHead)}
                data-test-id="filter-by-privileges-heading">Privileges</h4>
            <Form compactForm={true} data-test-id="filter-by-privileges">
              <CheckboxField label="System Administrators"
                             property={vnode.attrs.userFilters().superAdmins}/>
              <CheckboxField label="Normal Users"
                             property={vnode.attrs.userFilters().normalUsers}/>
            </Form>
          </div>
          <div class={classnames(styles.filterItems)}>
            <h4 class={classnames(styles.filterItemsHead)} data-test-id="filter-by-users-state-heading">
              User state
            </h4>
            <Form compactForm={true} data-test-id="filter-by-states">
              <CheckboxField label="Enabled"
                             property={vnode.attrs.userFilters().enabledUsers}/>
              <CheckboxField label="Disabled"
                             property={vnode.attrs.userFilters().disabledUsers}/>
            </Form>
          </div>

          <div class={classnames(styles.filterItems)}>
            <h4 class={classnames(styles.filterItemsHead)} data-test-id="filter-by-role-heading">Roles</h4>
            <div data-test-id="filter-by-roles" class={styles.filterByRoles}>
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

class RolesDropdown extends Dropdown<RolesViewAttrs & AjaxOperationMonitor> {

  toggleDropdown(vnode: m.Vnode<DropdownAttrs & RolesViewAttrs & AjaxOperationMonitor>, e: MouseEvent) {
    super.toggleDropdown(vnode, e);
    vnode.attrs.initializeRolesDropdownAttrs();
  }

  doRenderDropdownContent(vnode: m.Vnode<DropdownAttrs & RolesViewAttrs & AjaxOperationMonitor>): m.Children {
    const classNames = classnames({
                                    [styles.hidden]: !vnode.attrs.show(),
                                  }, styles.rolesDropdownContent);
    const checkboxes = vnode.attrs.roles().map((role) => {
      const triStateCheckbox = vnode.attrs.rolesSelection().get(role as GoCDRole);
      if (!triStateCheckbox) {
        return;
      }

      return <TriStateCheckboxField label={role.name()} property={Stream(triStateCheckbox)}/>;
    });

    return (
      <div class={classNames}>
        <div class={styles.rolesList}>
          <Form compactForm={true}>
            {checkboxes}
          </Form>
        </div>
        <Form compactForm={true}>
          <QuickAddField property={vnode.attrs.roleNameToAdd} placeholder="Add role"
                         ajaxOperationMonitor={vnode.attrs.operationState}
                         onclick={vnode.attrs.onRolesAdd.bind(this, vnode.attrs.roleNameToAdd(), vnode.attrs.users())}
                         buttonDisableReason={"Please type the role name to add."}
          />
        </Form>
        <Primary ajaxOperationMonitor={vnode.attrs.operationState}
                 onclick={vnode.attrs.onRolesUpdate.bind(this,
                                                         vnode.attrs.rolesSelection(),
                                                         vnode.attrs.users())}>Apply</Primary>
      </div>
    );
  }

  protected doRenderButton(vnode: m.Vnode<DropdownAttrs & RolesViewAttrs & AjaxOperationMonitor>) {
    return <Secondary dropdown={true}
                      disabled={!vnode.attrs.users().anyUserSelected() || vnode.attrs.operationState() === OperationState.IN_PROGRESS}
                      onclick={(e) => this.toggleDropdown(vnode, e)}>
      Roles
    </Secondary>;
  }
}

export class UsersActionsWidget extends MithrilViewComponent<Attrs> {
  view(vnode: m.Vnode<Attrs>) {
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

    return <div class={classnames(styles.userManagementHeader)}>
      <div class={classnames(styles.userActionsAndCounts)}>
        <div class={classnames(styles.userActions)}>
          <ButtonGroup>
            <Secondary ajaxOperation={vnode.attrs.onEnable.bind(vnode.attrs, vnode.attrs.users())}
                       ajaxOperationMonitor={vnode.attrs.operationState}
                       disabled={!vnode.attrs.users().anyUserSelected()}>Enable</Secondary>
            <Secondary ajaxOperation={vnode.attrs.onDisable.bind(vnode.attrs, vnode.attrs.users())}
                       ajaxOperationMonitor={vnode.attrs.operationState}
                       disabled={!vnode.attrs.users().anyUserSelected()}>Disable</Secondary>
            <Secondary onclick={vnode.attrs.onDelete.bind(vnode.attrs, vnode.attrs.users())}
                       ajaxOperationMonitor={vnode.attrs.operationState}
                       disabled={!vnode.attrs.users().anyUserSelected()}>Delete</Secondary>
            <RolesDropdown {...vnode.attrs} show={vnode.attrs.showRoles}/>
          </ButtonGroup>
        </div>
        <Counts counts={counts} dataTestId="users"/>
      </div>
      <div class={classnames(styles.userFilters)}>
        <SearchField property={vnode.attrs.userFilters().searchText} dataTestId={"search-box"}/>
        <FiltersView {...vnode.attrs} show={vnode.attrs.showFilters}/>
      </div>
    </div>;
  }
}
