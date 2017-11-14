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

require 'rails_helper'

describe Admin::UsersController do
  include MockRegistryModule
  include ExtraSpecAssertions
  before :each do
    @user_service = double('user_service')
    allow(controller).to receive(:user_service).and_return(@user_service)
    allow(controller).to receive(:current_user).and_return(@user = Username.new(CaseInsensitiveString.new("root")))
    allow(controller).to receive(:current_user_entity_id).and_return(@user_id = 1)
    allow(controller).to receive(:set_current_user)
  end

  describe "users" do
    it "should load users and aggregations" do
      expect(@user_service).to receive(:allUsersForDisplay).with(UserService::SortableColumn::USERNAME, UserService::SortDirection::ASC).and_return(['user', 'loser'])
      expect(@user_service).to receive(:enabledUserCount).and_return(1)
      expect(@user_service).to receive(:disabledUserCount).and_return(1)
      get :users
      assert_template layout: :admin
      expect(assigns[:users]).to eq(['user', 'loser'])
      expect(assigns[:total_enabled_users]).to eq(1)
      expect(assigns[:total_disabled_users]).to eq(1)
    end

    it "should honor column and direction requested" do
      expect(@user_service).to receive(:allUsersForDisplay).with(UserService::SortableColumn::EMAIL, UserService::SortDirection::DESC).and_return(['user', 'loser'])
      expect(@user_service).to receive(:enabledUserCount).and_return(1)
      expect(@user_service).to receive(:disabledUserCount).and_return(1)
      get :users, params: { :column => "email", :order => 'DESC' }

      expect(assigns[:users]).to eq(['user', 'loser'])
      expect(assigns[:total_enabled_users]).to eq(1)
      expect(assigns[:total_disabled_users]).to eq(1)
    end
  end

  describe "operate" do
    render_views

    it "should enable users through UserService and redirect to user listing" do
      expect(@user_service).to receive(:enable).with(users = ["user-1"], an_instance_of(HttpLocalizedOperationResult))
      post :operate, params: { :operation => "Enable", :selected => users }
      assert_redirected_with_flash("/admin/users", "Enabled 1 user(s) successfully.", 'success')
    end

    it "should enable users through UserService and redirect to user listing while retaining the column sort order" do
      expect(@user_service).to receive(:enable).with(users = ["user-1"], an_instance_of(HttpLocalizedOperationResult))
      post :operate, params: { :operation => "Enable", :selected => users, :order => "order", :column => "column" }
      assert_redirected_with_flash("/admin/users", "Enabled 1 user(s) successfully.", 'success', ["order=order","column=column"])
    end

    it "should show errors if operation does not succeed" do
      users = [UserModel.new(User.new("user-1", ["Foo", "fOO", "FoO"], "foo@cruise.go", true), ["user", "loser"], false),
                 UserModel.new(User.new("loser-1", ["baR", "bAR", "BaR"], "bar@cruise.com", false), ["loser"], true)]
      expect(@user_service).to receive(:enable).with(users = ["user-1"], an_instance_of(HttpLocalizedOperationResult)) do |u, r|
        r.badRequest(LocalizedMessage.string("SELECT_AT_LEAST_ONE_USER"))
      end
      post :operate, params: { :operation => "Enable", :selected => users }
      assert_redirected_with_flash("/admin/users", "Please select one or more users.", 'error')
    end

    it "should show notify when there are no users selected" do
      post :operate, params: { :operation => "Enable", :selected => [] }
      assert_redirected_with_flash("/admin/users", "Please select one or more users.", 'error')
      post :operate, params: { :operation => "Enable" }
      assert_redirected_with_flash("/admin/users", "Please select one or more users.", 'error')
    end

    it "should disable users through UserService" do
      expect(@user_service).to receive(:disable).with(users = ["user-1"], an_instance_of(HttpLocalizedOperationResult))
      post :operate, params: { :operation => "Disable", :selected => users }
    end

    it "should modify roles through UserService" do
      selections = [TriStateSelection.new("admin", "add")]
      expect(@user_service).to receive(:modifyRolesAndUserAdminPrivileges).with(users = ["user-1"], TriStateSelection.new(com.thoughtworks.go.domain.config.Admin::GO_SYSTEM_ADMIN, TriStateSelection::Action.remove), selections, an_instance_of(HttpLocalizedOperationResult))
      post :operate, params: { :operation => "apply_roles", :selected => users, :selections => {"admin" => TriStateSelection::Action.add.to_s}, :admin => {com.thoughtworks.go.domain.config.Admin::GO_SYSTEM_ADMIN => TriStateSelection::Action.remove.to_s} }
      assert_redirected_with_flash("/admin/users", "Role(s)/Admin-Privilege modified for 1 user(s) successfully.", 'success')
    end

    it "should not modify admin-privileges when not submitted" do
      selections = [TriStateSelection.new("admin", "add")]
      expect(@user_service).to receive(:modifyRolesAndUserAdminPrivileges).with(users = ["user-1"], TriStateSelection.new(com.thoughtworks.go.domain.config.Admin::GO_SYSTEM_ADMIN, TriStateSelection::Action.nochange), selections, an_instance_of(HttpLocalizedOperationResult))
      post :operate, params: { :operation => "apply_roles", :selected => users, :selections => {"admin" => TriStateSelection::Action.add.to_s} }
      assert_redirected_with_flash("/admin/users", "Role(s)/Admin-Privilege modified for 1 user(s) successfully.", 'success')
    end

    it "should add a new role to users through UserService" do
      selections = [TriStateSelection.new("admin", TriStateSelection::Action.add.to_s)]
      expect(@user_service).to receive(:modifyRolesAndUserAdminPrivileges).with( users = ["user-1"], TriStateSelection.new(com.thoughtworks.go.domain.config.Admin::GO_SYSTEM_ADMIN, TriStateSelection::Action.nochange), selections, an_instance_of(HttpLocalizedOperationResult))
      post :operate, params: { :operation => "add_role", :selected => users, :new_role => "admin" }
      assert_redirected_with_flash("/admin/users", "New role assigned to 1 user(s) successfully.", 'success')
    end

    it "should disallow unknown operations" do
      post :operate, params: { :operation => "Something", :selected => [] }
      assert_redirected_with_flash("/admin/users", "Unknown operation", 'error')
    end

    it "should disallow unknown operations" do
      post :operate
      assert_redirected_with_flash("/admin/users", "Unknown operation", 'error')
    end

  end

  describe "roles" do
    it "should load all roles" do
      roles = [ TriStateSelection.new('admin', 'remove') ]
      go_admin = TriStateSelection.new('Go Admin', 'add')
      expect(@user_service).to receive(:getAdminAndRoleSelections).with(["tom"]).and_return(UserService::AdminAndRoleSelections.new(go_admin, roles))
      post :roles, params: { :selected => ["tom"], :no_layout => true }
      expect(assigns[:selections]).to eq(roles)
      expect(assigns[:admin_selection]).to eq(go_admin)
    end
  end

  describe "search" do
    before do
      @user_search_service = double('user_search_service')
      @result = double(HttpLocalizedOperationResult)

      allow(HttpLocalizedOperationResult).to receive(:new).and_return(@result)
      allow(controller).to receive(:user_search_service).and_return(@user_search_service)
      allow(@result).to receive(:hasMessage).with(no_args).and_return(false)
    end

    it "should search for a user" do
      current_user_name = com.thoughtworks.go.server.domain.Username.new(CaseInsensitiveString.new('admin_user'))
      allow(controller).to receive(:current_user).and_return(current_user_name)

      search_text = "foo"
      user_search_models = [
        UserSearchModel.new(User.new("foo", "Mr Foo", "foo@cruise.go"), UserSourceType::PLUGIN),
        UserSearchModel.new(User.new("Bar", "Mr Bar", "bar@cruise.com"), UserSourceType::PLUGIN)
      ]
      expect(@user_search_service).to receive(:search).with(search_text, @result).and_return(user_search_models)

      post :search, params: { :no_layout => true, :search_text => search_text }

      expect(assigns[:users]).to eq(user_search_models)
    end

    it "should search for a user" do
      current_user_name = com.thoughtworks.go.server.domain.Username.new(CaseInsensitiveString.new('admin_user'))
      allow(controller).to receive(:current_user).and_return(current_user_name)

      search_text = "foo"
      user_search_models = [
        UserSearchModel.new(User.new("foo", "Mr Foo", "foo@cruise.go"), UserSourceType::PLUGIN),
        UserSearchModel.new(User.new("Bar", "Mr Bar", "bar@cruise.com"), UserSourceType::PLUGIN)
      ]
      expect(@user_search_service).to receive(:search).with(search_text, @result).and_return(user_search_models)

      post :search, params: { :no_layout => true, :search_text => search_text }

      expect(assigns[:users]).to eq(user_search_models)
    end

    it "should warn user if search results in warnings and show the results" do
      allow(@result).to receive(:hasMessage).with(no_args).and_return(true)
      allow(@result).to receive(:message).with(any_args).and_return("some ldap error")

      current_user_name = com.thoughtworks.go.server.domain.Username.new(CaseInsensitiveString.new('admin_user'))
      allow(controller).to receive(:current_user).and_return(current_user_name)

      search_text = "foo"
      user_search_models = [
        UserSearchModel.new(User.new("foo", "Mr Foo", "foo@cruise.go"), UserSourceType::PLUGIN),
        UserSearchModel.new(User.new("Bar", "Mr Bar", "bar@cruise.com"), UserSourceType::PLUGIN)
      ]
      expect(@user_search_service).to receive(:search).with(search_text, @result).and_return(user_search_models)
      post :search, params: { :no_layout => true, :search_text => search_text }

      expect(assigns[:users]).to eq(user_search_models)
      expect(assigns[:warning_message]).to eq("some ldap error")
    end
  end

  describe "create" do
    it "should create a new user" do
      user_service = double('user_service')
      allow(controller).to receive(:user_service).and_return(user_service)

      params_selections = [{"name"=>"foo", "full_name"=>"Mr Foo", "email"=>"foo@cruise.com"},{"name"=>"Bar", "full_name"=>"Mr Bar", "email"=>"bar@cruise.com"}]

      user_search_models = [UserSearchModel.new(User.new("foo", "Mr Foo", "foo@cruise.com"), UserSourceType::PLUGIN),
                            UserSearchModel.new(User.new("Bar", "Mr Bar", "bar@cruise.com"), UserSourceType::PLUGIN)]
      result = double(HttpLocalizedOperationResult)
      allow(HttpLocalizedOperationResult).to receive(:new).and_return(result)
      expect(user_service).to receive(:create).with(user_search_models, result)
      expect(user_service).to receive(:allUsersForDisplay).with(UserService::SortableColumn::USERNAME, UserService::SortDirection::ASC).and_return(['foo', 'Bar'])
      expect(user_service).to receive(:enabledUserCount).and_return(1)
      expect(user_service).to receive(:disabledUserCount).and_return(1)
      expect(result).to receive(:isSuccessful).and_return(true)

      post :create, params: { :no_layout => true, :selections => params_selections }

      assert_template   "users"
    end

    it "should handle no user selections" do
      user_service = double('user_service')
      allow(controller).to receive(:user_service).and_return(user_service)

      result = double(HttpLocalizedOperationResult)
      allow(HttpLocalizedOperationResult).to receive(:new).and_return(result)
      expect(result).to receive(:isSuccessful).and_return(false)

      expect(user_service).to receive(:create).with([], result)


      expect(result).to receive(:httpCode).and_return(400)
      expect(result).to receive(:message).and_return("Failed to add user")

      post :create, params: { :no_layout => true }

      expect(response.body).to eq("Failed to add user\n")
    end

    it "should render error message when user is not created" do
      user_service = double('user_service')
      allow(controller).to receive(:user_service).and_return(user_service)

      params_selections = []
      user_search_models = []
      result = double(HttpLocalizedOperationResult)
      allow(HttpLocalizedOperationResult).to receive(:new).and_return(result)

      expect(user_service).to receive(:create).with(user_search_models, result)

      expect(result).to receive(:isSuccessful).and_return(false)

      expect(result).to receive(:httpCode).and_return(400)
      expect(result).to receive(:message).and_return("Failed to add user")

      post :create, params: { :no_layout => true, :selections => params_selections }

      expect(response.body).to eq("Failed to add user\n")
    end

  end

end
