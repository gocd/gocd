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

shared_examples_for :environment_variables_form do
  before do
    set(@cruise_config, "md5", "abc")
  end

  it "should populate plain text env vars for the pipeline" do
    render template: @view_file

    Capybara.string(response.body).find('form').tap do |form|
      expect(form).to have_selector("input[name='#{@object_name}[variables][][name]'][value='env-name']")
      expect(form).to have_selector("input[name='#{@object_name}[variables][][original_name]'][value='env-name']")
      expect(form).to have_selector("input[name='#{@object_name}[variables][][valueForDisplay]'][value='env-val']")

      expect(form).to have_selector("input[name='#{@object_name}[variables][][name]'][value='env-name2']")
      expect(form).to have_selector("input[name='#{@object_name}[variables][][original_name]'][value='env-name2']")
      expect(form).to have_selector("input[name='#{@object_name}[variables][][valueForDisplay]'][value='env-val2']")

      expect(form).to have_selector("input[name='default_as_empty_list[]'][value='#{@object_name}>variables']")
    end
  end

  it "should have correct row templates" do
    render template: @view_file

    Capybara.string(response.body).find('form div#variables_secure tbody.template', visible: false).tap do |template|
      expect(template).to have_selector("input[name='#{@object_name}[variables][][valueForDisplay]']", visible: false)
    end
  end

  it "should show errors" do
    errors = config_errors([EnvironmentVariableConfig::NAME, "bad env var name"], [EnvironmentVariableConfig::VALUE, "bad value"])
    set(@variables.get(0), "configErrors", errors)

    render template: @view_file

    Capybara.string(response.body).find('form').tap do |form|
      expect(form).to have_selector("div.field_with_errors input[name='#{@object_name}[variables][][name]'][value='env-name']")
      expect(form).to have_selector("div.name_value_error", text: "bad env var name")
      expect(form).to have_selector("div.field_with_errors input[name='#{@object_name}[variables][][valueForDisplay]'][value='env-val']")
      expect(form).to have_selector("div.name_value_error", text: "bad value")
    end
  end
end

shared_examples_for :secure_environment_variables_form do

  it "should display the secure variables section" do
    render template: @view_file

    expect(response.body).to have_selector("h3", text: "Secure Variables");
  end

  it "should populate secure env vars for the pipeline" do
    render template: @view_file

    Capybara.string(response.body).find('form').tap do |form|
      expect(form).to have_selector("input[name='#{@object_name}[variables][][name]'][value='password']")
      expect(form).to have_selector("input[name='#{@object_name}[variables][][original_name]'][value='password']")
      expect(form).to have_selector("input[name='#{@object_name}[variables][][valueForDisplay]'][value='#{@encryptedVariable.getEncryptedValue()}'][type='password']")
      expect(form).to have_selector("input[name='#{@object_name}[variables][][secure]'][value='true']")

      expect(form).to have_selector("input[name='default_as_empty_list[]'][value='#{@object_name}>variables']")
    end
  end

  it "should have correct row templates for secure section" do
    render template: @view_file

    Capybara.string(response.body).find('form div#variables_secure tbody.template', visible: false).tap do |template|
      expect(template).to have_selector("input[name='#{@object_name}[variables][][valueForDisplay]'][type='password']", visible: false)
      expect(template).to have_selector("input##{@object_name}_variables__secure", visible: false)
      expect(template).to have_selector("input[type='hidden'][name='#{@object_name}[variables][][#{com.thoughtworks.go.config.EnvironmentVariableConfig::ISCHANGED}]'][value='true']", visible: false)
    end
  end

  it "should show edit link" do
    render template: @view_file

    Capybara.string(response.body).find('form').tap do |form|
      expect(form).to have_selector("input[name='#{@object_name}[variables][][valueForDisplay]'][value='#{@encryptedVariable.getEncryptedValue()}'][type='password'][readonly='readonly']")
      expect(form).to have_selector("input[name='#{@object_name}[variables][][originalValue]'][value='#{@encryptedVariable.getEncryptedValue()}'][type='hidden']")
      expect(form).to have_selector("input[type='hidden'][name='#{@object_name}[variables][][#{com.thoughtworks.go.config.EnvironmentVariableConfig::ISCHANGED}]'][value='false']")
      expect(form).to have_selector("a.edit.skip_dirty_stop", text: "Edit")
      expect(form).to have_selector("a.reset.hidden.skip_dirty_stop", text: "Reset")
    end
  end
end
