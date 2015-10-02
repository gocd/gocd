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

describe "admin/templates/edit_permissions.html.erb" do
  include GoUtil, ReflectiveUtil

  before(:each) do
    @cruise_config = BasicCruiseConfig.new
    set(@cruise_config, "md5", "abcd1234")

    @template = PipelineTemplateConfigMother.createTemplate('some_template')
    @cruise_config.addTemplate(@template)

    @template.getAuthorization().getAdminsConfig().add(AdminUser.new(CaseInsensitiveString.new("new-admin")))
    @template.getAuthorization().getAdminsConfig().add(AdminUser.new(CaseInsensitiveString.new("old-admin")))

    assign(:cruise_config, @cruise_config)
    assign(:pipeline, @template)
  end

  it "should render template edit permissions form with grid for users" do
    render

    Capybara.string(response.body).find("form[action='/admin/templates/some_template/permissions'][method='post']").tap do |form|
      expect(form).to have_selector("input[type='hidden'][name='config_md5'][value='abcd1234']")

      expect(form).to have_selector("label[for='template_name']", :text => "Template Name")
      expect(form).to have_selector("input[type='text'][name='template[name]'][value='some_template'][id='template_name']")

      match_hidden_row(form, "", Authorization::UserType::USER, Authorization::PrivilegeState::ON)

      match_row(form, "new-admin", Authorization::UserType::USER, Authorization::PrivilegeState::ON)
      match_row(form, "old-admin", Authorization::UserType::USER, Authorization::PrivilegeState::ON)

      expect(form).to have_selector("a[href='/admin/templates/some_template/permissions']")
    end
  end

  def match_hidden_row form, name, type, admin
    form.find("textarea#USER_users_and_roles_template") do |textarea|
      match_inputs textarea, name, type, admin
    end
  end

  def match_row form, name, type, admin
    form.find("tr##{type}_#{name}") do |tr|
      match_inputs tr, name, type, admin
    end
  end

  def match_inputs element, name, type, admin
    expect(element).to have_selector("input[type='text'][name='template[#{PipelineConfigs::AUTHORIZATION}][][#{Authorization::PresentationElement::NAME}]'][value='#{name}']")
    expect(element).to have_selector("input[type='hidden'][name='template[#{PipelineConfigs::AUTHORIZATION}][][#{Authorization::PresentationElement::TYPE}]'][value='#{type}']")
    expect(element).to have_selector("input[type='hidden'][name='template[#{PipelineConfigs::AUTHORIZATION}][][#{Authorization::PRIVILEGES}][][#{Authorization::PresentationElement::ADMIN_PRIVILEGE}]'][value='#{admin}']")
  end
end
