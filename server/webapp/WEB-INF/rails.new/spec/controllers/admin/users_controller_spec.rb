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

require 'spec_helper'

describe Admin::UsersController do
  include MockRegistryModule
  before :each do
    @user_service = double('user_service')
    controller.stub(:user_service).and_return(@user_service)
    controller.stub(:current_user).and_return(@user = Username.new(CaseInsensitiveString.new("root")))
    controller.stub(:current_user_entity_id).and_return(@user_id = 1)
    controller.stub(:set_current_user)
  end

  describe "users" do
    it "should load users and aggregations" do
      @user_service.should_receive(:allUsersForDisplay).with(UserService::SortableColumn::USERNAME, UserService::SortDirection::ASC).and_return(['user', 'loser'])
      @user_service.should_receive(:enabledUserCount).and_return(1)
      @user_service.should_receive(:disabledUserCount).and_return(1)
      get :users
      assert_template layout: :admin
      assigns[:users].should == ['user', 'loser']
      assigns[:total_enabled_users].should == 1
      assigns[:total_disabled_users].should == 1
    end

    it "should honor column and direction requested" do
      @user_service.should_receive(:allUsersForDisplay).with(UserService::SortableColumn::EMAIL, UserService::SortDirection::DESC).and_return(['user', 'loser'])
      @user_service.should_receive(:enabledUserCount).and_return(1)
      @user_service.should_receive(:disabledUserCount).and_return(1)
      get :users, :column => "email", :order => 'DESC'

      assigns[:users].should == ['user', 'loser']
      assigns[:total_enabled_users].should == 1
      assigns[:total_disabled_users].should == 1
    end
  end

  describe "new" do

    it "should match /admin/users/new to" do
      expect(:get => "/admin/users/new").to route_to({:controller => "admin/users", :action => 'new', :no_layout=>true})
      expect(controller.send(:users_new_path)).to eq("/admin/users/new")
    end
  end

  describe "operate" do
    render_views

    it "should match /admin/users/operate to" do
      expect(:post => "/admin/users/operate").to route_to({:controller => "admin/users", :action => 'operate'})
      expect(controller.send(:user_operate_path)).to eq("/admin/users/operate")
    end

    it "should enable users through UserService and redirect to user listing" do
      @user_service.should_receive(:enable).with(users = ["user-1"], an_instance_of(HttpLocalizedOperationResult))
      post :operate, :operation => "Enable", :selected => users
      assert_redirected_with_flash("/admin/users", "Enabled 1 user(s) successfully.", 'success')
    end

    it "should enable users through UserService and redirect to user listing while retaining the column sort order" do
      @user_service.should_receive(:enable).with(users = ["user-1"], an_instance_of(HttpLocalizedOperationResult))
      post :operate, :operation => "Enable", :selected => users, :order => "order", :column => "column"
      assert_redirected_with_flash("/admin/users", "Enabled 1 user(s) successfully.", 'success', ["order=order","column=column"])
    end

    it "should show errors if operation does not succeed" do
      users = [UserModel.new(User.new("user-1", ["Foo", "fOO", "FoO"], "foo@cruise.go", true), ["user", "loser"], false),
                 UserModel.new(User.new("loser-1", ["baR", "bAR", "BaR"], "bar@cruise.com", false), ["loser"], true)]
      @user_service.should_receive(:enable).with(users = ["user-1"], an_instance_of(HttpLocalizedOperationResult)) do |u, r|
        r.badRequest(LocalizedMessage.string("SELECT_AT_LEAST_ONE_USER"))
      end
      post :operate, :operation => "Enable", :selected => users
      assert_redirected_with_flash("/admin/users", "Please select one or more users.", 'error')
    end

    it "should show notify when there are no users selected" do
      post :operate, :operation => "Enable", :selected => []
      assert_redirected_with_flash("/admin/users", "Please select one or more users.", 'error')
      post :operate, :operation => "Enable"
      assert_redirected_with_flash("/admin/users", "Please select one or more users.", 'error')
    end

    it "should disable users through UserService" do
      @user_service.should_receive(:disable).with(users = ["user-1"], an_instance_of(HttpLocalizedOperationResult))
      post :operate, :operation => "Disable", :selected => users
    end

    it "should modify roles through UserService" do
      selections = [TriStateSelection.new("admin", "add")]
      @user_service.should_receive(:modifyRolesAndUserAdminPrivileges).with(users = ["user-1"], TriStateSelection.new(com.thoughtworks.go.domain.config.Admin::GO_SYSTEM_ADMIN, TriStateSelection::Action.remove), selections, an_instance_of(HttpLocalizedOperationResult))
      post :operate, :operation => "apply_roles", :selected => users, :selections => {"admin" => TriStateSelection::Action.add.to_s}, :admin => {com.thoughtworks.go.domain.config.Admin::GO_SYSTEM_ADMIN => TriStateSelection::Action.remove.to_s}
      assert_redirected_with_flash("/admin/users", "Role(s)/Admin-Privilege modified for 1 user(s) successfully.", 'success')
    end

    it "should not modify admin-privileges when not submitted" do
      selections = [TriStateSelection.new("admin", "add")]
      @user_service.should_receive(:modifyRolesAndUserAdminPrivileges).with(users = ["user-1"], TriStateSelection.new(com.thoughtworks.go.domain.config.Admin::GO_SYSTEM_ADMIN, TriStateSelection::Action.nochange), selections, an_instance_of(HttpLocalizedOperationResult))
      post :operate, :operation => "apply_roles", :selected => users, :selections => {"admin" => TriStateSelection::Action.add.to_s}
      assert_redirected_with_flash("/admin/users", "Role(s)/Admin-Privilege modified for 1 user(s) successfully.", 'success')
    end

    it "should add a new role to users through UserService" do
      selections = [TriStateSelection.new("admin", TriStateSelection::Action.add.to_s)]
      @user_service.should_receive(:modifyRolesAndUserAdminPrivileges).with( users = ["user-1"], TriStateSelection.new(com.thoughtworks.go.domain.config.Admin::GO_SYSTEM_ADMIN, TriStateSelection::Action.nochange), selections, an_instance_of(HttpLocalizedOperationResult))
      post :operate, :operation => "add_role", :selected => users, :new_role => "admin"
      assert_redirected_with_flash("/admin/users", "New role assigned to 1 user(s) successfully.", 'success')
    end

    it "should match /admin/users/roles to" do
      expect(:post => "/admin/users/roles").to route_to({:controller => "admin/users", :action => 'roles', :no_layout=>true})
      expect(controller.send(:user_roles_path)).to eq("/admin/users/roles")
    end

    it "should disallow unknown operations" do
      post :operate, :operation => "Something", :selected => []
      assert_redirected_with_flash("/admin/users", "Unknown operation", 'error')
    end

    it "should disallow unknown operations" do
      post :operate
      assert_redirected_with_flash("/admin/users", "Unknown operation", 'error')
    end

  end

  describe :roles do
    it "should load all roles" do
      roles = [ TriStateSelection.new('admin', 'remove') ]
      go_admin = TriStateSelection.new('Go Admin', 'add')
      @user_service.should_receive(:getAdminAndRoleSelections).with(["tom"]).and_return(UserService::AdminAndRoleSelections.new(go_admin, roles))
      post :roles, :selected => ["tom"], :no_layout => true
      assigns[:selections].should == roles
      assigns[:admin_selection].should == go_admin
    end
  end

  describe "search" do
    before do
      @user_search_service = Object.new
      @result = HttpLocalizedOperationResult.new

      HttpLocalizedOperationResult.stub(:new).and_return(@result)
      controller.stub(:user_search_service).and_return(@user_search_service)
    end

    it "should match /users/search to" do
      expect(:post => "/admin/users/search").to route_to({:controller => "admin/users", :action => 'search', :no_layout=>true})
      expect(controller.send(:users_search_path)).to eq("/admin/users/search")
    end

    it "should search for a user" do
      current_user_name = com.thoughtworks.go.server.domain.Username.new(CaseInsensitiveString.new('admin_user'))
      controller.stub(:current_user).and_return(current_user_name)

      search_text = "foo"
      user_search_models = [UserSearchModel.new(User.new("foo", "Mr Foo", "foo@cruise.go")),
                            UserSearchModel.new(User.new("Bar", "Mr Bar", "bar@cruise.com"))]
      @user_search_service.should_receive(:search).with(search_text, @result).and_return(user_search_models)

      post :search, :no_layout => true, :search_text => search_text

      assigns[:users].should ==  user_search_models
    end

    it "should search for a user" do
      current_user_name = com.thoughtworks.go.server.domain.Username.new(CaseInsensitiveString.new('admin_user'))
      controller.stub(:current_user).and_return(current_user_name)

      search_text = "foo"
      user_search_models = [UserSearchModel.new(User.new("foo", "Mr Foo", "foo@cruise.go")),
                            UserSearchModel.new(User.new("Bar", "Mr Bar", "bar@cruise.com"))]
      @user_search_service.should_receive(:search).with(search_text, @result).and_return(user_search_models)

      post :search, :no_layout => true, :search_text => search_text

      assigns[:users].should ==  user_search_models
    end

    it "should warn user if search results in warnings and show the results" do
      @result.conflict(LocalizedMessage.string("LDAP_ERROR"))

      current_user_name = com.thoughtworks.go.server.domain.Username.new(CaseInsensitiveString.new('admin_user'))
      controller.stub(:current_user).and_return(current_user_name)

      search_text = "foo"
      user_search_models = [UserSearchModel.new(User.new("foo", "Mr Foo", "foo@cruise.go")),
                            UserSearchModel.new(User.new("Bar", "Mr Bar", "bar@cruise.com"))]
      @user_search_service.should_receive(:search).with(search_text, @result).and_return(user_search_models)
      post :search, :no_layout => true, :search_text => search_text

      assigns[:users].should ==  user_search_models
      assigns[:warning_message] = "Ldap search failed, please contact the administrator."
    end
  end

  describe "create" do
    it "should match /users/create to" do
      expect(:post => "/admin/users/create").to route_to({:controller => "admin/users", :action => 'create', :no_layout=>true})
      expect(controller.send(:users_create_path)).to eq("/admin/users/create")
    end

    it "should create a new user" do
      user_service = double('user_service')
      controller.stub(:user_service).and_return(user_service)

      params_selections = [{"name"=>"foo", "full_name"=>"Mr Foo", "email"=>"foo@cruise.com"},{"name"=>"Bar", "full_name"=>"Mr Bar", "email"=>"bar@cruise.com"}]

      user_search_models = [UserSearchModel.new(User.new("foo", "Mr Foo", "foo@cruise.com"), UserSourceType::PASSWORD_FILE),
                            UserSearchModel.new(User.new("Bar", "Mr Bar", "bar@cruise.com"), UserSourceType::PASSWORD_FILE)]
      result = Object.new
      HttpLocalizedOperationResult.stub(:new).and_return(result)
      user_service.should_receive(:create).with(user_search_models, result)
      user_service.should_receive(:allUsersForDisplay).with(UserService::SortableColumn::USERNAME, UserService::SortDirection::ASC).and_return(['foo', 'Bar'])
      user_service.should_receive(:enabledUserCount).and_return(1)
      user_service.should_receive(:disabledUserCount).and_return(1)
      result.should_receive(:isSuccessful).and_return(true)

      post :create, :no_layout => true, :selections => params_selections

      assert_template   "users"
    end

    it "should handle no user selections" do
      user_service = Object.new
      controller.stub(:user_service).and_return(user_service)

      result = Object.new
      HttpLocalizedOperationResult.stub(:new).and_return(result)
      result.should_receive(:isSuccessful).and_return(false)

      user_service.should_receive(:create).with([], result)


      result.should_receive(:httpCode).and_return(400)
      result.should_receive(:message).and_return("Failed to add user")

      post :create, :no_layout => true

      response.body.should == "Failed to add user\n"
    end

    it "should render error message when user is not created" do
      user_service = Object.new
      controller.stub(:user_service).and_return(user_service)

      params_selections = []
      user_search_models = []
      result = Object.new
      HttpLocalizedOperationResult.stub(:new).and_return(result)

      user_service.should_receive(:create).with(user_search_models, result)

      result.should_receive(:isSuccessful).and_return(false)

      result.should_receive(:httpCode).and_return(400)
      result.should_receive(:message).and_return("Failed to add user")

      post :create, :no_layout => true, :selections => params_selections

      response.body.should == "Failed to add user\n"
    end

  end

  describe "delete_all" do
    before do
      controller.stub(:user_service).and_return(@user_service = double("user_service"))
    end

    it "should match /users/delete_all to" do
      expect(:delete => "/admin/users/delete_all").to route_to({:controller => "admin/users", :action => 'delete_all', :no_layout=>true})
      expect(controller.send(:users_delete_path)).to eq("/admin/users/delete_all")
    end

    it "should delete all users" do
      @user_service.should_receive(:deleteAll)
      delete :delete_all, :no_layout => true
    end

    it "should render success message" do
      @user_service.stub(:deleteAll)
      delete :delete_all, :no_layout => true
      expect(response.body).to eq("Deleted")
    end
  end

end
