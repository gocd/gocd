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

require File.join(File.dirname(__FILE__), "..", "..", "..", "spec_helper")

describe "admin/templates/edit_permissions.html.erb" do
  include GoUtil, ReflectiveUtil

  before(:each) do
    @cruise_config = CruiseConfig.new
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

    response.body.should have_tag("form[action='/admin/templates/some_template/permissions'][method='post']") do
      with_tag("input[type='hidden'][name='config_md5'][value='abcd1234']")

      with_tag("label[for='template_name']", "Template Name")
      with_tag("input[type='text'][name='template[name]'][value='some_template'][id='template_name']")

      match_hidden_row("", Authorization::UserType::USER, Authorization::PrivilegeState::ON)

      match_row("new-admin", Authorization::UserType::USER, Authorization::PrivilegeState::ON)
      match_row("old-admin", Authorization::UserType::USER, Authorization::PrivilegeState::ON)

      with_tag("a[href='/admin/templates/some_template/permissions']")
    end
  end

  def match_hidden_row name, type, admin
    with_tag("textarea#USER_users_and_roles_template") do
      match_inputs name, type, admin
    end
  end

  def match_row name, type, admin
    with_tag("tr##{type}_#{name}") do
      match_inputs name, type, admin
    end
  end

  def match_inputs name, type, admin
    with_tag("input[type='text'][name='template[#{PipelineConfigs::AUTHORIZATION}][][#{Authorization::PresentationElement::NAME}]'][value='#{name}']")
    with_tag("input[type='hidden'][name='template[#{PipelineConfigs::AUTHORIZATION}][][#{Authorization::PresentationElement::TYPE}]'][value='#{type}']")
    with_tag("input[type='hidden'][name='template[#{PipelineConfigs::AUTHORIZATION}][][#{Authorization::PRIVILEGES}][][#{Authorization::PresentationElement::ADMIN_PRIVILEGE}]'][value='#{admin}']")
  end
end
