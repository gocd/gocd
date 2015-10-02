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

describe "admin/templates/index.html.erb" do

  include ReflectiveUtil
  include GoUtil

  before(:each) do
    assign(:template_to_pipelines, {
                                      "template1" => to_list(["pipeline1", "pipeline2"]),
                                      "template2" => to_list(["pipeline3"]),
                                      "template3" => to_list([])
                                    })
    assign(:user, Username.new(CaseInsensitiveString.new("loser")))
    view.stub(:tab_with_display_name).and_return("tab_link")
    view.stub(:mycruise_available?).and_return(false)
    view.stub(:can_view_admin_page?).and_return(true)
    view.stub(:is_user_a_template_admin?).and_return(false)
    view.stub(:is_user_an_admin?).and_return(true)
    assign(:cruise_config, cruise_config = BasicCruiseConfig.new)
    set(cruise_config, "md5", "abcd1234")
  end

  it "should display the list of all the templates and the pipelines in it" do
    render

    expect(view.instance_variable_get("@tab_name")).to eq("templates")

    Capybara.string(response.body).find('.templates').tap do |templates|
      all_the_templates = templates.all(".template")
      expect(all_the_templates.size).to eq(3)

      all_the_templates[0].tap do |template|
        expect(template).to have_selector("h2", :text => "template1")
        template.find("table") do |table|
          table.find("thead tr.pipeline") do |tr|
            expect(tr).to have_selector("th", :text => "Pipeline")
            expect(tr).to have_selector("th", :text => "Actions")
          end
          table.find("tbody") do |tbody|
            tbody.find("tr.pipeline") do |tr|
              expect(tr).to have_selector("td a[href='#{pipeline_edit_path(:pipeline_name => "pipeline1", :current_tab => "general")}']", :text => "pipeline1")
              tr.find("td a[href='#{pipeline_edit_path(:pipeline_name => "pipeline1", :current_tab => "general")}'][class='action_icon edit_icon']") do |td|
                expect(td).to have_selector("span", :text => "Edit")
              end
            end
            tbody.find("tr.pipeline") do |tr|
              expect(tr).to have_selector("td a[href='#{pipeline_edit_path(:pipeline_name => "pipeline2", :current_tab => "general")}']", :text => "pipeline2")
              tr.find("td a[href='#{pipeline_edit_path(:pipeline_name => "pipeline2", :current_tab => "general")}'][class='action_icon edit_icon']") do |td|
                expect(td).to have_selector("span", :text => "Edit")
              end
            end
          end
        end
      end

      all_the_templates[1].tap do |template|
        expect(template).to have_selector("h2", "template2")
        template.find("table") do |table|
          table.find("tbody") do |tbody|
            tbody.find("tr.pipeline") do |tr|
              expect(tr).to have_selector("td a[href='#{pipeline_edit_path(:pipeline_name => "pipeline3", :current_tab => "general")}']", :text => "pipeline3")
              tr.find("td a[href='#{pipeline_edit_path(:pipeline_name => "pipeline3", :current_tab => "general")}'][class='action_icon edit_icon']") do |td|
                expect(td).to have_selector("span", :text => "Edit")
              end
            end
          end
        end
      end

      all_the_templates[2].tap do |template|
        expect(template).to have_selector("h2", :text => "template3")
        expect(template).to have_selector(".information", :text => "No pipelines associated with this template")
      end
    end
  end

  it "should display the list of all the templates and the pipelines in it if user is both template admin and super admin" do
    view.stub(:is_user_a_template_admin?).and_return(true)
    view.stub(:is_user_an_admin?).and_return(true)

    render

    expect(view.instance_variable_get("@tab_name")).to eq("templates")

    Capybara.string(response.body).find('.templates').tap do |templates|
      all_the_templates = templates.all(".template")
      expect(all_the_templates.size).to eq(3)

      all_the_templates[0].tap do |template|
        expect(template).to have_selector("h2", :text => "template1")
        template.find("table") do |table|
          table.find("thead tr.pipeline") do |tr|
            expect(tr).to have_selector("th", :text => "Pipeline")
            expect(tr).to have_selector("th", :text => "Actions")
          end
          table.find("tbody") do |tbody|
            tbody.find("tr.pipeline") do |tr|
              expect(tr).to have_selector("td a[href='#{pipeline_edit_path(:pipeline_name => "pipeline1", :current_tab => "general")}']", :text => "pipeline1")
              tr.find("td a[href='#{pipeline_edit_path(:pipeline_name => "pipeline1", :current_tab => "general")}'][class='action_icon edit_icon']") do |td|
                expect(td).to have_selector("span", :text => "Edit")
              end
            end
            tbody.find("tr.pipeline") do |tr|
              expect(tr).to have_selector("td a[href='#{pipeline_edit_path(:pipeline_name => "pipeline2", :current_tab => "general")}']", :text => "pipeline2")
              tr.find("td a[href='#{pipeline_edit_path(:pipeline_name => "pipeline2", :current_tab => "general")}'][class='action_icon edit_icon']") do |td|
                expect(td).to have_selector("span", :text => "Edit")
              end
            end
          end
        end
      end

      all_the_templates[1].tap do |template|
        expect(template).to have_selector("h2", "template2")
        template.find("table") do |table|
          table.find("tbody") do |tbody|
            tbody.find("tr.pipeline") do |tr|
              expect(tr).to have_selector("td a[href='#{pipeline_edit_path(:pipeline_name => "pipeline3", :current_tab => "general")}']", :text => "pipeline3")
              tr.find("td a[href='#{pipeline_edit_path(:pipeline_name => "pipeline3", :current_tab => "general")}'][class='action_icon edit_icon']") do |td|
                expect(td).to have_selector("span", :text => "Edit")
              end
            end
          end
        end
      end

      all_the_templates[2].tap do |template|
        expect(template).to have_selector("h2", :text => "template3")
        expect(template).to have_selector(".information", :text => "No pipelines associated with this template")
      end
    end
  end

  it "should display that there are pipelines using this template but not show pipelines if user is template admin" do
    view.stub(:is_user_a_template_admin?).and_return(true)
    view.stub(:is_user_an_admin?).and_return(false)

    render

    expect(view.instance_variable_get("@tab_name")).to eq("templates")

    Capybara.string(response.body).find('.templates').tap do |templates|
      templates.find("#template_container_template1") do |template_container|
        expect(template_container).to have_selector("h2", :text => "template1")
        template_container.find("table") do |table|
          table.find("thead tr.pipeline") do |tr|
            expect(tr).to have_selector("th", :text => "Pipeline")
            expect(tr).to have_selector("th", :text => "Actions")
          end
          table.find("tbody") do |tbody|
            tbody.find("tr") do |tr|
              expect(tr).to have_selector("td span", :text => "This template is used in 2 pipelines.")
            end
          end
        end
      end
      templates.find("#template_container_template2") do |template_container|
        expect(template_container).to have_selector("h2", :text => "template2")
        template_container.find("table") do |table|
          table.find("thead tr.pipeline") do |tr|
            expect(tr).to have_selector("th", :text => "Pipeline")
            expect(tr).to have_selector("th", :text => "Actions")
          end
          table.find("tbody") do |tbody|
            tbody.find("tr") do |tr|
              expect(tr).to have_selector("td span", :text => "This template is used in 1 pipeline.")
            end
          end
        end
      end
    end
  end

  it "should display a message when there are no templates configured" do
    assign(:template_to_pipelines, {})

    render

    Capybara.string(response.body).find('.templates').tap do |templates|
      expect(templates).to have_selector(".information", :text => "There are no templates configured")
      expect(templates).not_to have_selector(".template")
    end
  end


  it "should display a link to create a new template when running enterprise mode" do
    assign(:template_to_pipelines, {})

    render

    Capybara.string(response.body).find('.templates').tap do |templates|
      expect(templates).to have_selector("a[href='#'][class='add_link']", :text => "Add New Template")
    end
  end

  it "should not display a link to create a new template when running enterprise mode for a template admin" do
    view.stub(:is_user_a_template_admin?).and_return(true)
    view.stub(:is_user_an_admin?).and_return(false)
    assign(:template_to_pipelines, {})

    render

    Capybara.string(response.body).find('.templates').tap do |templates|
      expect(templates).to have_selector("span[title='You are unauthorized to perform this operation. Please contact a Go System Administrator to create a template.']")
    end
  end

  it "should display a link to create a new template when running enterprise mode if user is both template admin and super admin" do
    view.stub(:is_user_a_template_admin?).and_return(true)
    view.stub(:is_user_an_admin?).and_return(true)
    assign(:template_to_pipelines, {})

    render

    Capybara.string(response.body).find('.templates').tap do |templates|
      expect(templates).to have_selector("a[href='#'][class='add_link']", :text => "Add New Template")
    end
  end

  it "should display a link to create a new template when running enterprise mode with templates already configured" do
    assign(:template_to_pipelines, {"template1" => to_list(["pipeline1", "pipeline2"])})

    render

    Capybara.string(response.body).find('.templates').tap do |templates|
      expect(templates).to have_selector("a[href='#'][class='add_link']", :text => "Add New Template")
    end
  end

  it "should display a edit permissions link next to the template name" do

    assign(:template_to_pipelines, {"unused_template" => to_list([]), "used_template" => to_list(["pipeline"])})

    render

    Capybara.string(response.body).find('.templates').tap do |templates|
      templates.all(".template") do |template_list|
        expect(template_list[0]).to have_selector("h2", :text => "unused_template")
        template_list[0].find("a[href='#{edit_template_permissions_path(:template_name => "unused_template")}'][class='action_icon lock_icon']") do |a|
          expect(a).to have_selector("span", :text => "Permissions")
        end
      end
    end
  end

  it "should display a delete button next to the template name" do
    assign(:template_to_pipelines, {"unused_template" => to_list([]), "used_template" => to_list(["pipeline"])})

    render

    Capybara.string(response.body).find('.templates').tap do |templates|
      templates.all(".template") do |template_list|
        expect(template_list[0]).to have_selector("h2", :text => "unused_template")
        expect(template_list[0]).to have_selector(".information", :text => "No pipelines associated with this template")
        template_list[0].find("form#delete_template_unused_template[action='#{delete_template_path(:pipeline_name => "unused_template")}'][method='post']") do |form|
          expect(form).to have_selector("input[type='hidden'][name='_method'][value='delete']")
          expect(form).to have_selector("span#trigger_delete_unused_template.delete_parent[title='Delete this template']")
          expect(form).to have_selector("script[type='text/javascript']", :text => /Util.escapeDotsFromId\('trigger_delete_unused_template #warning_prompt'\)/)
          expect(form).to have_selector("div#warning_prompt[style='display:none;']", :text => /Are you sure you want to delete the template 'unused_template' \?/)
        end
      end
      templates.all(".template") do |template_list|
        expect(template_list[1]).to have_selector("h2", :text => "used_template")
        expect(template_list[1]).to have_selector("span.delete_icon_disabled[title='Cannot delete this template as it is used by at least one pipeline']")
      end
    end
  end

  it "should disable the delete button next to the template name for template admin" do
    view.stub(:is_user_a_template_admin?).and_return(true)
    view.stub(:is_user_an_admin?).and_return(false)
    assign(:template_to_pipelines, {"used_template" => to_list(["pipeline"])})

    render

    Capybara.string(response.body).find('.templates').tap do |templates|
      templates.find(".template") do |template|
        expect(template).to have_selector("h2", :text => "used_template")
        expect(template).to have_selector("span.delete_icon_disabled[title='You are unauthorized to perform this operation. Please contact a Go System Administrator to delete this template.']")
      end
    end
  end

  it "should display a delete button next to the template name if user is both template admin and super admin" do
    view.stub(:is_user_a_template_admin?).and_return(true)
    view.stub(:is_user_an_admin?).and_return(true)

    assign(:template_to_pipelines, {"unused_template" => to_list([]), "used_template" => to_list(["pipeline"])})

    render

    Capybara.string(response.body).find('.templates').tap do |templates|
      templates.all(".template") do |template_list|
        expect(template_list[0]).to have_selector("h2", :text => "unused_template")
        expect(template_list[0]).to have_selector(".information", :text => "No pipelines associated with this template")
        template_list[0].find("form#delete_template_unused_template[action='#{delete_template_path(:pipeline_name => "unused_template")}'][method='post']") do |form|
          expect(form).to have_selector("input[type='hidden'][name='_method'][value='delete']")
          expect(form).to have_selector("span#trigger_delete_unused_template.delete_parent[title='Delete this template']")
          expect(form).to have_selector("script[type='text/javascript']", :text => /Util.escapeDotsFromId\('trigger_delete_unused_template #warning_prompt'\)/)
          expect(form).to have_selector("div#warning_prompt[style='display:none;']", :text => /Are you sure you want to delete the template 'unused_template' \?/)
        end
      end
      templates.all(".template") do |template_list|
        expect(template_list[1]).to have_selector("h2", :text => "used_template")
        expect(template_list[1]).to have_selector("span.delete_icon_disabled[title='Cannot delete this template as it is used by at least one pipeline']")
      end
    end
  end

  it "should disable the edit permissions link next to the template name for template admin and not super admin" do
    view.stub(:is_user_a_template_admin?).and_return(true)
    view.stub(:is_user_an_admin?).and_return(false)

    assign(:template_to_pipelines, {"used_template" => to_list(["pipeline"])})

    render

    Capybara.string(response.body).find('.templates').tap do |templates|
      templates.find(".template") do |template|
        expect(template).to have_selector("h2", :text => "used_template")
        expect(template).to have_selector("span.lock_icon_disabled[title='You are unauthorized to perform this operation. Please contact a Go System Administrator to add/remove a template admin.']", :text => "Permissions")
      end
    end
  end

  it "should enable the edit permissions link next to the template name if user is both template admin and super admin" do
    view.stub(:is_user_a_template_admin?).and_return(true)
    view.stub(:is_user_an_admin?).and_return(true)

    assign(:template_to_pipelines, {"used_template" => to_list(["pipeline"])})

    render

    Capybara.string(response.body).find('.templates').tap do |templates|
      templates.find(".template") do |template|
        expect(template).to have_selector("h2", :text => "used_template")
        template.find("a[href='#{edit_template_permissions_path(:template_name => "used_template")}'][class='action_icon lock_icon']") do |a|
          expect(a).to have_selector("span", "Permissions")
        end
      end
    end
  end

  it "should display an edit button next to the template name" do
    assign(:template_to_pipelines, {"unused_template" => to_list([]), "used_template" => to_list(["pipeline"])})

    render

    Capybara.string(response.body).find('.templates').tap do |templates|
      templates.all(".template") do |template_list|
        expect(template_list[0]).to have_selector("h2", :text => "unused_template")
        expect(template_list[0]).to have_selector("a[href='#{template_edit_path(:pipeline_name => "unused_template", :current_tab => "general", :stage_parent => "templates")}']")
      end
    end
  end
end
