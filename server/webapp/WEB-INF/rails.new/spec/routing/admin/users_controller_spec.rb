##########################################################################
# Copyright 2017 ThoughtWorks, Inc.
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
##########################################################################

require 'rails_helper'

describe Admin::UsersController do
  it "should match /admin/users/new to" do
    expect(:get => "/admin/users/new").to route_to({:controller => "admin/users", :action => 'new', :no_layout=>true})
    expect(:get => users_new_path).to route_to({:controller => "admin/users", :action => 'new', :no_layout=>true})
  end

  it "should match /admin/users/operate to" do
    expect(:post => "/admin/users/operate").to route_to({:controller => "admin/users", :action => 'operate'})
    expect(:post => user_operate_path).to route_to({:controller => "admin/users", :action => 'operate'})
  end

  it "should match /admin/users/roles to" do
    expect(:post => "/admin/users/roles").to route_to({:controller => "admin/users", :action => 'roles', :no_layout=>true})
    expect(:post => user_roles_path).to route_to({:controller => "admin/users", :action => 'roles', :no_layout=>true})
  end

  it "should match /users/search to" do
    expect(:post => "/admin/users/search").to route_to({:controller => "admin/users", :action => 'search', :no_layout=>true})
    expect(:post => users_search_path).to route_to({:controller => "admin/users", :action => 'search', :no_layout=>true})
  end

  it "should match /users/create to" do
    expect(:post => "/admin/users/create").to route_to({:controller => "admin/users", :action => 'create', :no_layout=>true})
    expect(:post => users_create_path).to route_to({:controller => "admin/users", :action => 'create', :no_layout=>true})
  end
end
