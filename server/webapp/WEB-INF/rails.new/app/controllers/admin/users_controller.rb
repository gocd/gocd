##########################GO-LICENSE-START################################
# Copyright 2014 ThoughtWorks, Inc.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
##########################GO-LICENSE-END##################################

module Admin
  class UsersController < AdminController
    include UsersHelper
    include ApplicationHelper

    layout :determine_layout

    def new
    end

    def create
      user_selections = params[:selections] || []
      user_models = user_selections.map { |u| UserSearchModel.new(User.new(u[:name], u[:full_name], u[:email])) }
      user_service.create(user_models, result = HttpLocalizedOperationResult.new)
      if result.isSuccessful()
        users
        render :action => "users"
        return
      else
        render_localized_operation_result(result)
      end
    end

    def search
      @users = user_search_service.search(params[:search_text], result = HttpLocalizedOperationResult.new)
      result.hasMessage() && (@warning_message = result.message(Spring.bean('localizer')))
    end

    def users
      @tab_name = "user-listing"
      @users = user_service.allUsersForDisplay(UserService::SortableColumn.valueOf((params[:column] || "username").upcase), UserService::SortDirection.valueOf(params[:order] || "ASC"))
      @total_enabled_users = user_service.enabledUserCount()
      @total_disabled_users = user_service.disabledUserCount()
    end

    def operate
      operation = (params[:operation] || "").downcase
      if operation !~ /(enable|disable|apply_roles|add_role)/
        redirect_with_flash(l.string("UNKNOWN_OPERATION"), :action => :users, :params => params.slice(:column, :order), :class => "error")
        return
      end

      selected_users = params[:selected] || []
      if selected_users.empty?
        redirect_with_flash(l.string("SELECT_AT_LEAST_ONE_USER"), :action => :users, :params => params.slice(:column, :order), :class => "error")
        return
      end
      do_not_change_admin = TriStateSelection.new(com.thoughtworks.go.domain.config.Admin::GO_SYSTEM_ADMIN, TriStateSelection::Action.nochange)

      if params[:operation] == 'apply_roles'
        admin_selection = params[:admin] ? TriStateSelection.new(com.thoughtworks.go.domain.config.Admin::GO_SYSTEM_ADMIN, params[:admin][com.thoughtworks.go.domain.config.Admin::GO_SYSTEM_ADMIN]) : do_not_change_admin
        user_service.modifyRolesAndUserAdminPrivileges(selected_users, admin_selection, selections, result = HttpLocalizedOperationResult.new)
      elsif params[:operation] == 'add_role'
        user_service.modifyRolesAndUserAdminPrivileges(selected_users, do_not_change_admin, [TriStateSelection.new(params[:new_role], TriStateSelection::Action.add)], result = HttpLocalizedOperationResult.new)
      else
        user_service.send(operation, selected_users, result = HttpLocalizedOperationResult.new)
      end
      result_message, flash_class = result.isSuccessful ? [l.string("USER_#{params[:operation]}_SUCCESSFUL", [params[:selected].length]), "success"] : [result.message(localizer), "error"]
      redirect_with_flash(result_message, :action => :users, :params => params.slice(:column, :order), :class => flash_class)
    end

    def roles
      users = params[:selected] || []
      admin_and_role_selections = user_service.getAdminAndRoleSelections(users)
      @selections = admin_and_role_selections.getRoleSelections()
      @admin_selection = admin_and_role_selections.getAdminSelection()
    end

    def determine_layout
      %w(users).include?(action_name) || %w(operate).include?(action_name) ? "admin" : false
    end
  end
end
