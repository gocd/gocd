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
include GoUtil

describe "admin/package_definitions/pipelines_used_in.html.erb" do
  before(:each) do
    @admin = Username.new(CaseInsensitiveString.new("admin"))
    @pipeline_group_one_admin = Username.new(CaseInsensitiveString.new("group-one-admin"))
    @pipeline_group_two_admin = Username.new(CaseInsensitiveString.new("group-two-admin"))

    assign(:cruise_config, @cruise_config = BasicCruiseConfig.new)
    security_config = SecurityConfig.new()
    security_config.modifyPasswordFile(PasswordFileConfig.new("/tmp/pass"));
    admin_config = security_config.adminsConfig()
    admin_config.add(com.thoughtworks.go.config.AdminUser.new(@admin.getUsername()))
    server_config = ServerConfig.new("artifacts-dir", security_config)
    @cruise_config.setServerConfig(server_config)

    @group_one = BasicPipelineConfigs.new()
    @group_one.setGroup("group-one")
    @group_one.getAuthorization().getAdminsConfig().add(com.thoughtworks.go.config.AdminUser.new(@pipeline_group_one_admin.getUsername()))

    group_two = BasicPipelineConfigs.new()
    group_two.setGroup("group-two")
    group_two.getAuthorization().getAdminsConfig().add(com.thoughtworks.go.config.AdminUser.new(@pipeline_group_two_admin.getUsername()))

    @cruise_config.getGroups().add(@group_one)
    @cruise_config.getGroups().add(group_two)


    packagePipelines = ArrayList.new
    @pipeline_config = PipelineConfig.new(CaseInsensitiveString.new("pipeline-name"), MaterialConfigs.new(), [StageConfig.new].to_java(StageConfig))
    packagePipelines.add(Pair.new(@pipeline_config, @group_one))
    assign(:pipelines_with_group, packagePipelines)
  end

  describe "list.html" do
    it "should render package name and package configurations along with listing for super admin" do
      assign(:user, @admin)

      render

      Capybara.string(response.body).find("table[class='list_table']").tap do |table|
        table.all("tr") do |trs|
          trs[0].find("td") do |td|
            expect(td).to have_selector("a[href='#{admin_material_index_path(:pipeline_name => @pipeline_config.name()) }']")
          end
          expect(trs[1]).to have_selector("td", :text => @group_one.getGroup())
        end
      end
    end

    it "should render package name and package configurations along with listing for pipeline admin" do
      assign(:user, @pipeline_group_one_admin)

      render

      Capybara.string(response.body).find("table[class='list_table']").tap do |table|
        table.all("tr") do |trs|
          trs[0].find("td") do |td|
            expect(td).to have_selector("a[href='#{admin_material_index_path(:pipeline_name => @pipeline_config.name()) }']")
          end
          expect(trs[1]).to have_selector("td", :text => @group_one.getGroup())
        end
      end
    end

    it "should render package name and package configurations along with listing for non pipeline admin" do
      assign(:user, @pipeline_group_two_admin)

      render

      Capybara.string(response.body).find("table[class='list_table']").tap do |table|
        table.all("tr") do |trs|
          expect(trs[0]).to have_selector("td", :text => @pipeline_config.name().toString())
          expect(trs[1]).to have_selector("td", :text => @group_one.getGroup())
        end
      end
      expect(response.body).not_to have_selector("a")
    end
  end
end
