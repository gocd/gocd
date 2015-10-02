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

describe "admin/pipeline_groups/edit.html.erb" do
  include GoUtil, ReflectiveUtil

  before(:each) do
    @cruise_config = BasicCruiseConfig.new
    set(@cruise_config, "md5", "abcd1234")
    @group = PipelineConfigMother.createGroups(["group1"].to_java(java.lang.String)).get(0)
    assign(:group, @group)
    assign(:cruise_config, @cruise_config)
    @group.getAuthorization().getAdminsConfig().add(AdminUser.new(CaseInsensitiveString.new("admin")))
    @group.getAuthorization().getOperationConfig().add(AdminUser.new(CaseInsensitiveString.new("operator")))
    @group.getAuthorization().getViewConfig().add(AdminUser.new(CaseInsensitiveString.new("viewer")))
    in_params(:group_name => "group1")
  end

  it "should render pipeline group edit form" do
    render

    Capybara.string(response.body).find("form[action='/admin/pipeline_group/group1'][method='post']").tap do |form|
      expect(form).to have_selector("input[type='hidden'][name='_method'][value='PUT']")

      expect(form).to have_selector("input[type='hidden'][name='config_md5'][value='abcd1234']")

      expect(form).to have_selector("label[for='group_group']", :text => "Pipeline Group Name*")
      expect(form).to have_selector("input[type='text'][name='group[group]'][value='group1'][id='group_group']")
    end
  end

  it "should render permissions grid for users and roles" do
    @group.getAuthorization().getViewConfig().add(AdminUser.new(CaseInsensitiveString.new("loser")))
    @group.getAuthorization().getOperationConfig().add(AdminUser.new(CaseInsensitiveString.new("loser")))
    @group.getAuthorization().getOperationConfig().add(AdminUser.new(CaseInsensitiveString.new("boozer")))
    @group.getAuthorization().getAdminsConfig().add(AdminUser.new(CaseInsensitiveString.new("admin")))

    @group.getAuthorization().getAdminsConfig().add(AdminRole.new(CaseInsensitiveString.new("gang_of_losers")))
    @group.getAuthorization().getOperationConfig().add(AdminRole.new(CaseInsensitiveString.new("group_of_boozers")))

    render

    Capybara.string(response.body).find("form[action='/admin/pipeline_group/group1'][method='post']").tap do |form|
      match_row(form, "loser", Authorization::UserType::USER, Authorization::PrivilegeState::OFF, Authorization::PrivilegeState::ON, Authorization::PrivilegeState::ON)
      match_row(form, "boozer", Authorization::UserType::USER, Authorization::PrivilegeState::OFF, Authorization::PrivilegeState::ON, Authorization::PrivilegeState::OFF)
      match_row(form, "admin", Authorization::UserType::USER, Authorization::PrivilegeState::ON, Authorization::PrivilegeState::DISABLED, Authorization::PrivilegeState::DISABLED)

      match_row(form, "gang_of_losers", Authorization::UserType::ROLE, Authorization::PrivilegeState::ON, Authorization::PrivilegeState::DISABLED, Authorization::PrivilegeState::DISABLED)
      match_row(form, "group_of_boozers", Authorization::UserType::ROLE, Authorization::PrivilegeState::OFF, Authorization::PrivilegeState::ON, Authorization::PrivilegeState::OFF)
    end
  end

  it "should have tooltips on view, operate and admin checkboxes" do
    render

    expect(response.body).to have_selector("input[type='checkbox'][name='view'][title='Allows users to view pipelines in this group.']")
    expect(response.body).to have_selector("input[type='checkbox'][name='operate'][title='Allows users to operate (trigger) pipelines in this group.']")
    expect(response.body).to have_selector("input[type='checkbox'][name='admin'][title='Allows users to administer this group. This includes the ability to configure pipelines in this group and the ability to grant other users access. This permission automatically gives the user view and operate permissions.']")
  end

  def match_row form, name, type, admin, operate, view
    form.find("tr##{type}_#{name}") do |tr|
      expect(tr).to have_selector("input[type='text'][name='group[#{PipelineConfigs::AUTHORIZATION}][][#{Authorization::PresentationElement::NAME}]'][value='#{name}']")
      expect(tr).to have_selector("input[type='hidden'][name='group[#{PipelineConfigs::AUTHORIZATION}][][#{Authorization::PresentationElement::TYPE}]'][value='#{type}']")

      match_privilege tr, admin, Authorization::PresentationElement::ADMIN_PRIVILEGE
      match_privilege tr, operate, Authorization::PresentationElement::OPERATE_PRIVILEGE
      match_privilege tr, view, Authorization::PresentationElement::VIEW_PRIVILEGE
    end
  end

  def match_privilege tr, privilege_state, privilege_type
    expect(tr).to have_selector("input[type='hidden'][name='group[#{PipelineConfigs::AUTHORIZATION}][][#{Authorization::PRIVILEGES}][][#{privilege_type}]'][value='#{privilege_state}']")
    expect(tr).to have_selector("input[type='checkbox'][name='#{privilege_type}']")
  end
end
