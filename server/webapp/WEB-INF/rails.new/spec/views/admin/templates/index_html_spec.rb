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

require File.join(File.dirname(__FILE__), "/../../../spec_helper")

describe "admin/templates/index.html.erb" do

  include ReflectiveUtil
  include GoUtil

  before(:each) do
    assigns[:template_to_pipelines] = {
                                      "template1" => to_list(["pipeline1", "pipeline2"]),
                                      "template2" => to_list(["pipeline3"]),
                                      "template3" => to_list([])
                                    }
    assigns[:user] = Username.new(CaseInsensitiveString.new("loser"))
    template.stub(:tab_with_display_name).and_return("tab_link")
    template.stub(:mycruise_available?).and_return(false)
    template.stub(:can_view_admin_page?).and_return(true)
    template.stub(:is_user_a_template_admin?).and_return(false)
    template.stub(:is_user_an_admin?).and_return(true)
    assigns[:cruise_config] = cruise_config = CruiseConfig.new
    set(cruise_config, "md5", "abcd1234")
  end

  it "should display the list of all the templates and the pipelines in it" do
    render "admin/templates/index.html"

    assigns[:tab_name].should == "templates"

    response.body.should have_tag(".templates") do
      with_tag(".template") do
        with_tag("h2", "template1")
        with_tag("table") do
          with_tag("thead tr.pipeline") do
            with_tag("th", "Pipeline")
            with_tag("th", "Actions")
          end
          with_tag("tbody") do
            with_tag("tr.pipeline") do
              with_tag("td a[href='#{pipeline_edit_path(:pipeline_name => "pipeline1", :current_tab => "general")}']", "pipeline1")
              with_tag("td a[href='#{pipeline_edit_path(:pipeline_name => "pipeline1", :current_tab => "general")}'][class='action_icon edit_icon']") do
                with_tag("span", "Edit")
              end
            end
            with_tag("tr.pipeline") do
              with_tag("td a[href='#{pipeline_edit_path(:pipeline_name => "pipeline2", :current_tab => "general")}']", "pipeline2")
              with_tag("td a[href='#{pipeline_edit_path(:pipeline_name => "pipeline2", :current_tab => "general")}'][class='action_icon edit_icon']") do
                with_tag("span", "Edit")
              end
            end
          end
        end
      end
      with_tag(".template") do
        with_tag("h2", "template2")
        with_tag("table") do
          with_tag("tbody") do
            with_tag("tr.pipeline") do
              with_tag("td a[href='#{pipeline_edit_path(:pipeline_name => "pipeline3", :current_tab => "general")}']", "pipeline3")
              with_tag("td a[href='#{pipeline_edit_path(:pipeline_name => "pipeline3", :current_tab => "general")}'][class='action_icon edit_icon']") do
                with_tag("span", "Edit")
              end
            end
          end
        end
      end
      with_tag(".template") do
        with_tag("h2", "template3")
        with_tag(".information", "No pipelines associated with this template")
      end
    end
  end

  it "should display the list of all the templates and the pipelines in it if user is both template admin and super admin" do
    template.stub(:is_user_a_template_admin?).and_return(true)
    template.stub(:is_user_an_admin?).and_return(true)

    render "admin/templates/index.html"

    assigns[:tab_name].should == "templates"

    response.body.should have_tag(".templates") do
      with_tag(".template") do
        with_tag("h2", "template1")
        with_tag("table") do
          with_tag("thead tr.pipeline") do
            with_tag("th", "Pipeline")
            with_tag("th", "Actions")
          end
          with_tag("tbody") do
            with_tag("tr.pipeline") do
              with_tag("td a[href='#{pipeline_edit_path(:pipeline_name => "pipeline1", :current_tab => "general")}']", "pipeline1")
              with_tag("td a[href='#{pipeline_edit_path(:pipeline_name => "pipeline1", :current_tab => "general")}'][class='action_icon edit_icon']") do
                with_tag("span", "Edit")
              end
            end
            with_tag("tr.pipeline") do
              with_tag("td a[href='#{pipeline_edit_path(:pipeline_name => "pipeline2", :current_tab => "general")}']", "pipeline2")
              with_tag("td a[href='#{pipeline_edit_path(:pipeline_name => "pipeline2", :current_tab => "general")}'][class='action_icon edit_icon']") do
                with_tag("span", "Edit")
              end
            end
          end
        end
      end
      with_tag(".template") do
        with_tag("h2", "template2")
        with_tag("table") do
          with_tag("tbody") do
            with_tag("tr.pipeline") do
              with_tag("td a[href='#{pipeline_edit_path(:pipeline_name => "pipeline3", :current_tab => "general")}']", "pipeline3")
              with_tag("td a[href='#{pipeline_edit_path(:pipeline_name => "pipeline3", :current_tab => "general")}'][class='action_icon edit_icon']") do
                with_tag("span", "Edit")
              end
            end
          end
        end
      end
      with_tag(".template") do
        with_tag("h2", "template3")
        with_tag(".information", "No pipelines associated with this template")
      end
    end
  end

  it "should display that there are pipelines using this template but not show pipelines if user is template admin" do
    template.stub(:is_user_a_template_admin?).and_return(true)
    template.stub(:is_user_an_admin?).and_return(false)
    render "admin/templates/index.html"
    assigns[:tab_name].should == "templates"

    response.body.should have_tag(".templates") do
      with_tag("#template_container_template1") do
        with_tag("h2", "template1")
        with_tag("table") do
          with_tag("thead tr.pipeline") do
            with_tag("th", "Pipeline")
            with_tag("th", "Actions")
          end
          with_tag("tbody") do
            with_tag("tr") do
              with_tag("td span", "This template is used in 2 pipelines.")
            end
          end
        end
      end
      with_tag("#template_container_template2") do
        with_tag("h2", "template2")
        with_tag("table") do
          with_tag("thead tr.pipeline") do
            with_tag("th", "Pipeline")
            with_tag("th", "Actions")
          end
          with_tag("tbody") do
            with_tag("tr") do
              with_tag("td span", "This template is used in 1 pipeline.")
            end
          end
        end
      end
    end
  end

  it "should display a message when there are no templates configured" do
    assigns[:template_to_pipelines] = {}

    render "admin/templates/index.html.erb"

    response.body.should have_tag(".templates") do
      with_tag(".information", "There are no templates configured")
      without_tag(".template")
    end
  end


  it "should display a link to create a new template when running enterprise mode" do
    assigns[:template_to_pipelines] = {}

    render "admin/templates/index.html.erb"

    response.body.should have_tag(".templates") do
      with_tag("a[href='#'][class='add_link']", "Add New Template")
    end
  end

  it "should not display a link to create a new template when running enterprise mode for a template admin" do
    template.stub(:is_user_a_template_admin?).and_return(true)
    template.stub(:is_user_an_admin?).and_return(false)
    assigns[:template_to_pipelines] = {}

    render "admin/templates/index.html.erb"

    response.body.should have_tag(".templates") do
      with_tag("span[title=?]", "You are unauthorized to perform this operation. Please contact a Go System Administrator to create a template.")
    end
  end

  it "should display a link to create a new template when running enterprise mode if user is both template admin and super admin" do
    template.stub(:is_user_a_template_admin?).and_return(true)
    template.stub(:is_user_an_admin?).and_return(true)
    assigns[:template_to_pipelines] = {}

    render "admin/templates/index.html.erb"

    response.body.should have_tag(".templates") do
      with_tag("a[href='#'][class='add_link']", "Add New Template")
    end
  end

  it "should display a link to create a new template when running enterprise mode with templates already configured" do
    assigns[:template_to_pipelines] = {"template1" => to_list(["pipeline1", "pipeline2"])}

    render "admin/templates/index.html.erb"

    response.body.should have_tag(".templates") do
      with_tag("a[href='#'][class='add_link']", "Add New Template")
    end
  end

  it "should display a edit permissions link next to the template name" do

    assigns[:template_to_pipelines] = {"unused_template" => to_list([]), "used_template" => to_list(["pipeline"])}

    render "admin/templates/index.html.erb"

    response.body.should have_tag(".templates") do
      with_tag(".template") do
        with_tag("h2", "unused_template")
        with_tag("a[href='#{edit_template_permissions_path(:template_name => "unused_template")}'][class='action_icon lock_icon']") do
          with_tag("span", "Permissions")
        end
      end
    end
  end

  it "should display a delete button next to the template name" do

    assigns[:template_to_pipelines] = {"unused_template" => to_list([]), "used_template" => to_list(["pipeline"])}

    render "admin/templates/index.html.erb"

    response.body.should have_tag(".templates") do
      with_tag(".template") do
        with_tag("h2", "unused_template")
        with_tag(".information", "No pipelines associated with this template")
        with_tag("form#delete_template_unused_template[action='#{delete_template_path(:pipeline_name => "unused_template")}'][method='post']") do
          with_tag("input[type='hidden'][name='_method'][value='delete']")
          with_tag("span#trigger_delete_unused_template.delete_parent[title=?]", "Delete this template")
          with_tag("script[type='text/javascript']", /Util.escapeDotsFromId\('trigger_delete_unused_template #warning_prompt'\)/)
          with_tag("div#warning_prompt[style='display:none;']", /Are you sure you want to delete the template 'unused_template' \?/)
        end
      end
      with_tag(".template") do
        with_tag("h2", "used_template")
        with_tag("span.delete_icon_disabled[title=?]", "Cannot delete this template as it is used by at least one pipeline")
      end
    end
  end

  it "should disable the delete button next to the template name for template admin" do
    template.stub(:is_user_a_template_admin?).and_return(true)
    template.stub(:is_user_an_admin?).and_return(false)
    assigns[:template_to_pipelines] = {"used_template" => to_list(["pipeline"])}

    render "admin/templates/index.html.erb"

    response.body.should have_tag(".templates") do
      with_tag(".template") do
        with_tag("h2", "used_template")
        with_tag("span.delete_icon_disabled[title=?]", "You are unauthorized to perform this operation. Please contact a Go System Administrator to delete this template.")
      end
    end
  end

  it "should display a delete button next to the template name if user is both template admin and super admin" do
    template.stub(:is_user_a_template_admin?).and_return(true)
    template.stub(:is_user_an_admin?).and_return(true)

    assigns[:template_to_pipelines] = {"unused_template" => to_list([]), "used_template" => to_list(["pipeline"])}

    render "admin/templates/index.html.erb"

    response.body.should have_tag(".templates") do
      with_tag(".template") do
        with_tag("h2", "unused_template")
        with_tag(".information", "No pipelines associated with this template")
        with_tag("form#delete_template_unused_template[action='#{delete_template_path(:pipeline_name => "unused_template")}'][method='post']") do
          with_tag("input[type='hidden'][name='_method'][value='delete']")
          with_tag("span#trigger_delete_unused_template.delete_parent[title=?]", "Delete this template")
          with_tag("script[type='text/javascript']", /Util.escapeDotsFromId\('trigger_delete_unused_template #warning_prompt'\)/)
          with_tag("div#warning_prompt[style='display:none;']", /Are you sure you want to delete the template 'unused_template' \?/)
        end
      end
      with_tag(".template") do
        with_tag("h2", "used_template")
        with_tag("span.delete_icon_disabled[title=?]", "Cannot delete this template as it is used by at least one pipeline")
      end
    end
  end

  it "should disable the edit permissions link next to the template name for template admin and not super admin" do
    template.stub(:is_user_a_template_admin?).and_return(true)
    template.stub(:is_user_an_admin?).and_return(false)

    assigns[:template_to_pipelines] = {"used_template" => to_list(["pipeline"])}

    render "admin/templates/index.html.erb"

    response.body.should have_tag(".templates") do
      with_tag(".template") do
        with_tag("h2", "used_template")
        with_tag("span.lock_icon_disabled[title=?]", "You are unauthorized to perform this operation. Please contact a Go System Administrator to add/remove a template admin.", "Permissions")
      end
    end
  end

  it "should enable the edit permissions link next to the template name if user is both template admin and super admin" do
    template.stub(:is_user_a_template_admin?).and_return(true)
    template.stub(:is_user_an_admin?).and_return(true)

    assigns[:template_to_pipelines] = {"used_template" => to_list(["pipeline"])}

    render "admin/templates/index.html.erb"
    response.body.should have_tag(".templates") do
      with_tag(".template") do
        with_tag("h2", "used_template")
        with_tag("a[href='#{edit_template_permissions_path(:template_name => "used_template")}'][class='action_icon lock_icon']") do
          with_tag("span", "Permissions")
        end
      end
    end
  end

  it "should display an edit button next to the template name" do

    assigns[:template_to_pipelines] = {"unused_template" => to_list([]), "used_template" => to_list(["pipeline"])}

    render "admin/templates/index.html.erb"

    response.body.should have_tag(".templates") do
      with_tag(".template") do
        with_tag("h2", "unused_template")
        with_tag("a[href=?]", template_edit_path(:pipeline_name => "unused_template", :current_tab => "general", :stage_parent => "templates"))
      end
    end
  end
end