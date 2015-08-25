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

describe "admin/stages/permissions.html.erb" do
  include GoUtil
  include FormUI

  before :each do
    view.stub(:url_for).and_return("go_url")

    assign(:pipeline, @pipeline = PipelineConfigMother.createPipelineConfigWithStages("pipeline-name", ["dev", "acceptance"].to_java(:string)))
    assign(:stage, @stage = @pipeline.get(0))
    assign(:pipeline_group, @group = PipelineConfigMother::groupWithOperatePermission(@pipeline, ["admin", "badger"].to_java(java.lang.String)))

    assign(:cruise_config, cruise_config = GoConfigMother.defaultCruiseConfig())
    set(cruise_config, "md5", "abc")
    cruise_config.addPipeline("group-1", @pipeline)

    in_params(:pipeline_name => "pipeline", :stage_name => "stage", :action => "permissions", :controller => "admin/stages")
  end

  it "should have title Permissions" do
    render

    expect(response.body).to have_selector("h3", :text => "Permissions")
  end

  it "should show only-pipeline-group-operators message when pipeline group has permissions configured" do
    render

    expect(response.body).to have_selector("div.information", :text => "The pipeline group that this pipeline belongs to has permissions configured. You can add only those users and roles that have permissions to operate on this pipeline group.")
  end

  it "should not show only-pipeline-group-operators message when pipeline group has no permissions configured" do
    @group.setAuthorization(Authorization.new())

    render

    expect(response.body).not_to have_selector("div.information", :text => /pipeline group that this pipeline belongs to has permissions configured/)
  end

  it "should show inherit or specify locally radio buttons" do
    render

    Capybara.string(response.body).find('form').tap do |form|
      expect(form).to have_selector("input[type='hidden'][name='config_md5'][value='abc']") #Check the md5
      form.find(".form_item.security_mode") do |form_item|
        expect(form_item).to have_selector("span", :text => "For this stage:")
        expect(form_item).to have_selector("label[for='inherit_permissions']", :text => "Inherit from the pipeline group")
        expect(form_item).to have_selector("input[id='inherit_permissions'][name='stage[securityMode]'][type='radio'][value='inherit'][checked='checked']")
        expect(form_item).to have_selector("label[for='define_permissions']", :text => "Specify locally")
        expect(form_item).to have_selector("input[id='define_permissions'][name='stage[securityMode]'][type='radio'][value='define']")
      end
    end
  end

  it "should select inherit and show a message when both group and stage has no security" do
    @group.setAuthorization(Authorization.new())

    render

    Capybara.string(response.body).find('form').tap do |form|
      form.all(".form_item.security_mode") do |form_items| #check that inherit radio is selected
        expect(form_items[0]).to have_selector("span", :text => "For this stage:")
        expect(form_items[0]).to have_selector("label[for='inherit_permissions']", :text => "Inherit from the pipeline group")
        expect(form_items[0]).to have_selector("input[id='inherit_permissions'][name='stage[securityMode]'][type='radio'][value='inherit'][checked='checked']")
      end
      form.all(".form_item") do |form_items| #check that there is a message
        form_items[1].find(".inherited_permissions") do |inherited_permissions|
          expect(inherited_permissions).to have_selector(".no_permissions_message", :text => "There are no operate permissions configured for this stage nor its pipeline group. All Go users can operate on this stage.")
        end
        expect(form_items[1]).to have_selector(".stage_permissions.hidden")
      end
    end
  end

  it "should show the users and roles of a stage under 'define permissions'" do
    StageConfigMother::addApprovalWithUsers(@stage, ["user1", "user2"].to_java(java.lang.String)) #Add stage level security. There is no pipeline group security
    StageConfigMother::addApprovalWithRoles(@stage, ["role1", "role2"].to_java(java.lang.String))

    render

    Capybara.string(response.body).find('form').tap do |form|
      form.all(".form_item.security_mode") do |form_items|
        expect(form_items[0]).to have_selector("span", :text => "For this stage:")
        expect(form_items[0]).to have_selector("label[for='define_permissions']", :text => "Specify locally")
        expect(form_items[0]).to have_selector("input[id='define_permissions'][name='stage[securityMode]'][type='radio'][value='define'][checked='checked']")
      end
      form.all(".form_item") do |form_items|
        expect(form_items[1]).to have_selector(".inherited_permissions.hidden")
        form_items[1].find(".stage_permissions .users") do |users|
          expect(users).to have_selector("input[type='text'][value='user1'][name='stage[operateUsers][][name]']")
          expect(users).to have_selector("input[type='text'][value='user2'][name='stage[operateUsers][][name]']")
        end
        form_items[1].find(".stage_permissions .roles") do |roles|
          expect(roles).to have_selector("input[type='text'][value='role1'][name='stage[operateRoles][][name]']")
          expect(roles).to have_selector("input[type='text'][value='role2'][name='stage[operateRoles][][name]']")
        end
      end
    end
  end

  it "should show the users and roles of a stage under 'define permissions' even if the group has permissions defined" do
    @group.getAuthorization().getOperationConfig().add(AdminUser.new(CaseInsensitiveString.new("user1")))#All these should not be displayed
    @group.getAuthorization().getOperationConfig().add(AdminUser.new(CaseInsensitiveString.new("user2")))
    @group.getAuthorization().getOperationConfig().add(AdminRole.new(CaseInsensitiveString.new("role1")))
    @group.getAuthorization().getOperationConfig().add(AdminRole.new(CaseInsensitiveString.new("role2")))

    StageConfigMother::addApprovalWithUsers(@stage, ["user1"].to_java(java.lang.String))
    StageConfigMother::addApprovalWithRoles(@stage, ["role1"].to_java(java.lang.String))

    render

    Capybara.string(response.body).find('form').tap do |form|
      form.all(".form_item.security_mode") do |form_items|
        expect(form_items[0]).to have_selector("span", :text => "For this stage:")
        expect(form_items[0]).to have_selector("label[for='define_permissions']", :text => "Specify locally")
        expect(form_items[0]).to have_selector("input[id='define_permissions'][name='stage[securityMode]'][type='radio'][value='define'][checked='checked']")
      end
      form.all(".form_item") do |form_items|
        expect(form_items[1]).to have_selector(".inherited_permissions.hidden")
        form_items[1].find(".stage_permissions .users") do |users|
          expect(users).to have_selector("input[type='text'][value='user1'][name='stage[operateUsers][][name]']")
          expect(users).not_to have_selector("input[type='text'][value='user2'][name='stage[operateUsers][][name]']")
        end
        form_items[1].find(".stage_permissions .roles") do |roles|
          expect(roles).to have_selector("input[type='text'][value='role1'][name='stage[operateRoles][][name]']")
          expect(roles).not_to have_selector("input[type='text'][value='role2'][name='stage[operateRoles][][name]']")
        end
      end
    end
  end

  it "should show users and roles from pipeline group with class hidden under 'define permissions' when stage does not have permissions" do
    @group.getAuthorization().getOperationConfig().add(AdminUser.new(CaseInsensitiveString.new("user1")))
    @group.getAuthorization().getOperationConfig().add(AdminUser.new(CaseInsensitiveString.new("user2")))
    @group.getAuthorization().getOperationConfig().add(AdminRole.new(CaseInsensitiveString.new("role1")))
    @group.getAuthorization().getOperationConfig().add(AdminRole.new(CaseInsensitiveString.new("role2")))

    render

    Capybara.string(response.body).find('form').tap do |form|
      form.all(".form_item.security_mode") do |form_items|
        expect(form_items[0]).to have_selector("span", :text => "For this stage:")
        expect(form_items[0]).to have_selector("label[for='define_permissions']", :text => "Specify locally")
        expect(form_items[0]).to have_selector("input[id='inherit_permissions'][name='stage[securityMode]'][type='radio'][value='inherit'][checked='checked']")
      end
      form.all(".form_item") do |form_items|
        expect(form_items[1]).to have_selector(".inherited_permissions")
        form_items[1].find(".stage_permissions.hidden .users") do |users|
          expect(users).to have_selector("input[type='text'][value='user1'][name='stage[operateUsers][][name]']")
          expect(users).to have_selector("input[type='text'][value='user2'][name='stage[operateUsers][][name]']")
        end
        form_items[1].find(".stage_permissions.hidden .roles") do |roles|
          expect(roles).to have_selector("input[type='text'][value='role1'][name='stage[operateRoles][][name]']")
          expect(roles).to have_selector("input[type='text'][value='role2'][name='stage[operateRoles][][name]']")
        end
      end
    end
  end

  it "should show users and roles from pipeline group as uneditable list" do
    @group.getAuthorization().getOperationConfig().add(AdminUser.new(CaseInsensitiveString.new("user1")))
    @group.getAuthorization().getOperationConfig().add(AdminUser.new(CaseInsensitiveString.new("user2")))
    @group.getAuthorization().getOperationConfig().add(AdminRole.new(CaseInsensitiveString.new("role1")))
    @group.getAuthorization().getOperationConfig().add(AdminRole.new(CaseInsensitiveString.new("role2")))

    render

    Capybara.string(response.body).find('form').tap do |form|
      form.all(".form_item.security_mode") do |form_items|
        expect(form_items[0]).to have_selector("span", :text => "For this stage:")
        expect(form_items[0]).to have_selector("label[for='define_permissions']", :text => "Specify locally")
        expect(form_items[0]).to have_selector("input[id='inherit_permissions'][name='stage[securityMode]'][type='radio'][value='inherit'][checked='checked']")
      end
      form.all(".form_item") do |form_items|
        expect(form_items[1]).not_to have_selector(".inherited_permissions .no_permissions_message")
        form_items[1].find(".inherited_permissions .users") do |users|
          expect(users).to have_selector("input[type='text'][value='user1'][name='stage[operateUsers][][name]'][disabled='disabled']")
          expect(users).to have_selector("input[type='text'][value='user2'][name='stage[operateUsers][][name]'][disabled='disabled']")
        end
        form_items[1].find(".inherited_permissions .roles") do |roles|
          expect(roles).to have_selector("input[type='text'][value='role2'][name='stage[operateRoles][][name]'][disabled='disabled']")
          expect(roles).to have_selector("input[type='text'][value='role1'][name='stage[operateRoles][][name]'][disabled='disabled']")
        end
      end
    end
  end
end
