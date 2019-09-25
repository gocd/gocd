#
# Copyright 2019 ThoughtWorks, Inc.
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
#

require 'rails_helper'

describe "admin/templates/index.html.erb" do

  include ReflectiveUtil
  include GoUtil

  before(:each) do
    assign(:template_to_pipelines, {
      "template1" => to_list([CaseInsensitiveString.new("pipeline1"), CaseInsensitiveString.new("pipeline2")]),
      "template2" => to_list([CaseInsensitiveString.new("pipeline3")]),
      "template3" => to_list([])
    })
    assign(:user, Username.new(CaseInsensitiveString.new("loser")))
    allow(view).to receive(:tab_with_display_name).and_return("tab_link")
    allow(view).to receive(:mycruise_available?).and_return(false)
    allow(view).to receive(:can_view_admin_page?).and_return(true)
    allow(view).to receive(:is_user_a_template_admin_for_template?).and_return(false)
    allow(view).to receive(:is_user_an_admin?).and_return(true)

    assign(:cruise_config, cruise_config = BasicCruiseConfig.new)
    @go_config_service = double('go_config_service')
    view.stub(:go_config_service).and_return(@go_config_service)
    allow(@go_config_service).to receive(:isPipelineEditable).and_return(true)
    set(cruise_config, "md5", "abcd1234")
  end

  after :each do
    assign(:template_to_pipelines, {})
  end

  it "should display the list of all the templates and the pipelines in it" do
    render

    expect(view.instance_variable_get("@tab_name")).to eq("templates")

    Capybara.string(response.body).find('.templates').tap do |templates|
      all_the_templates = templates.all(".template")
      expect(all_the_templates.size).to eq(3)

      all_the_templates[0].tap do |template|
        expect(template).to have_selector("h2", :text => "template1")
        template.find("table").tap do |table|
          table.find("thead tr.pipeline").tap do |tr|
            expect(tr).to have_selector("th", :text => "Pipeline")
            expect(tr).to have_selector("th", :text => "Actions")
          end
          table.find("tbody").tap do |tbody|
            tbody.all("tr.pipeline")[0].tap do |tr|
              expect(tr).to have_selector("td a[href='#{pipeline_edit_path(:pipeline_name => "pipeline1", :current_tab => "general")}']", :text => "pipeline1")
              tr.find("td a[href='#{pipeline_edit_path(:pipeline_name => "pipeline1", :current_tab => "general")}'][class='action_icon edit_icon']").tap do |td|
                expect(td).to have_selector("span", :text => "Edit")
              end
            end
            tbody.all("tr.pipeline")[1].tap do |tr|
              expect(tr).to have_selector("td a[href='#{pipeline_edit_path(:pipeline_name => "pipeline2", :current_tab => "general")}']", :text => "pipeline2")
              tr.find("td a[href='#{pipeline_edit_path(:pipeline_name => "pipeline2", :current_tab => "general")}'][class='action_icon edit_icon']").tap do |td|
                expect(td).to have_selector("span", :text => "Edit")
              end
            end
          end
        end
      end

      all_the_templates[1].tap do |template|
        expect(template).to have_selector("h2", "template2")
        template.find("table").tap do |table|
          table.find("tbody").tap do |tbody|
            tbody.find("tr.pipeline").tap do |tr|
              expect(tr).to have_selector("td a[href='#{pipeline_edit_path(:pipeline_name => "pipeline3", :current_tab => "general")}']", :text => "pipeline3")
              tr.find("td a[href='#{pipeline_edit_path(:pipeline_name => "pipeline3", :current_tab => "general")}'][class='action_icon edit_icon']").tap do |td|
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
    allow(view).to receive(:is_user_a_template_admin_for_template?).and_return(true)
    allow(view).to receive(:is_user_an_admin?).and_return(true)

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
            tbody.all("tr.pipeline").tap do |tr|
              expect(tr[0]).to have_selector("td a[href='#{pipeline_edit_path(:pipeline_name => "pipeline1", :current_tab => "general")}']", :text => "pipeline1")
              tr[0].find("td a[href='#{pipeline_edit_path(:pipeline_name => "pipeline1", :current_tab => "general")}'][class='action_icon edit_icon']") do |td|
                expect(td).to have_selector("span", :text => "Edit")
              end
              expect(tr[1]).to have_selector("td a[href='#{pipeline_edit_path(:pipeline_name => "pipeline2", :current_tab => "general")}']", :text => "pipeline2")
              tr[1].find("td a[href='#{pipeline_edit_path(:pipeline_name => "pipeline2", :current_tab => "general")}'][class='action_icon edit_icon']") do |td|
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

  it 'should display names of config repo pipelines using the template for an admin' do
    view.stub(:is_user_a_template_admin_for_template?).and_return(true)
    view.stub(:is_user_an_admin?).and_return(true)
    allow(@go_config_service).to receive(:isPipelineEditable).with('pipeline2').and_return(false)

    render

    expect(view.instance_variable_get("@tab_name")).to eq("templates")

    Capybara.string(response.body).find('.templates').tap do |templates|
      templates.find("#template_container_template1").tap do |template_container|
        expect(template_container).to have_selector("h2", :text => "template1")
        template_container.find("table").tap do |table|
          table.find("thead tr.pipeline").tap do |tr|
            expect(tr).to have_selector("th", :text => "Pipeline")
            expect(tr).to have_selector("th", :text => "Actions")
          end
          table.find("tbody").tap do |tbody|
            expect(tbody).to have_selector("tr.pipeline td a[href='#{pipeline_edit_path(:pipeline_name => "pipeline1", :current_tab => "general")}']")

            tbody.find("tr.pipeline td a[href='#{pipeline_edit_path(:pipeline_name => "pipeline1", :current_tab => "general")}'][class='action_icon edit_icon']").tap do |td|
              expect(td).to have_selector("span", :text => "Edit")
            end
            expect(tbody).to have_selector("tr.pipeline td span", text: 'pipeline2')
            expect(tbody).to have_selector("tr.pipeline td span.edit_icon_disabled[title='Cannot edit pipeline pipeline2. Either you are unauthorized to edit the pipeline or the pipeline is defined in configuration repository.']")
          end
        end
      end
    end
  end

  it "should display names of pipelines using this template if user is a template admin" do
    allow(view).to receive(:has_admin_permissions_for_pipeline?).and_return(false)
    allow(view).to receive(:is_user_a_template_admin_for_template?).and_return(true)
    allow(view).to receive(:is_user_an_admin?).and_return(false)

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
            tbody.all("tr.pipeline").tap do |pipelines|
              expect(pipelines[0]).to have_selector("span", text: 'pipeline1')
              expect(pipelines[0]).to have_selector("span.edit_icon_disabled[title='Cannot edit pipeline pipeline1. Either you are unauthorized to edit the pipeline or the pipeline is defined in configuration repository.']")
              expect(pipelines[1]).to have_selector("span", text: 'pipeline2')
              expect(pipelines[1]).to have_selector("span.edit_icon_disabled[title='Cannot edit pipeline pipeline2. Either you are unauthorized to edit the pipeline or the pipeline is defined in configuration repository.']")
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
            tbody.all("tr.pipeline").tap do |pipelines|
              expect(pipelines[0]).to have_selector("span", text: 'pipeline3')
              expect(pipelines[0]).to have_selector("span.edit_icon_disabled[title='Cannot edit pipeline pipeline3. Either you are unauthorized to edit the pipeline or the pipeline is defined in configuration repository.']")
            end
          end
        end
      end
    end
  end

  it "should display names and edit pipeline link of pipelines using this template if user is a template admin" do
    allow(view).to receive(:has_admin_permissions_for_pipeline?).and_return(true)
    allow(view).to receive(:is_user_a_template_admin_for_template?).and_return(true)
    allow(view).to receive(:is_user_an_admin?).and_return(false)

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
            tbody.all("tr.pipeline").tap do |pipelines|
              pipelines[0].find("td a[href='#{pipeline_edit_path(:pipeline_name => "pipeline1", :current_tab => "general")}'][class='action_icon edit_icon']") do |pipeline|
                expect(pipeline).to have_selector("span", :text => "Edit")
              end
              pipelines[1].find("td a[href='#{pipeline_edit_path(:pipeline_name => "pipeline2", :current_tab => "general")}'][class='action_icon edit_icon']") do |pipeline|
                expect(pipeline).to have_selector("span", :text => "Edit")
              end
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
            tbody.all("tr.pipeline").tap do |pipelines|
              pipelines[0].find("td a[href='#{pipeline_edit_path(:pipeline_name => "pipeline3", :current_tab => "general")}'][class='action_icon edit_icon']") do |pipeline|
                expect(pipeline).to have_selector("span", :text => "Edit")
              end
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
      expect(templates).to have_selector("a[href='#'][class='link_as_button primary']", :text => "Add New Template")
    end
  end

  it "should not display a link to create a new template when running enterprise mode for a template admin" do
    allow(view).to receive(:is_user_a_template_admin_for_template?).and_return(true)
    allow(view).to receive(:is_user_an_admin?).and_return(false)
    assign(:template_to_pipelines, {})

    render

    Capybara.string(response.body).find('.templates').tap do |templates|
      expect(templates).to have_selector("span[title='You are unauthorized to perform this operation. Please contact a Go System Administrator to create a template.']")
    end
  end

  it "should display a link to create a new template when running enterprise mode if user is both template admin and super admin" do
    allow(view).to receive(:is_user_a_template_admin_for_template?).and_return(true)
    allow(view).to receive(:is_user_an_admin?).and_return(true)
    assign(:template_to_pipelines, {})

    render

    Capybara.string(response.body).find('.templates').tap do |templates|
      expect(templates).to have_selector("a[href='#'][class='link_as_button primary']", :text => "Add New Template")
    end
  end

  it "should display a link to create a new template when running enterprise mode with templates already configured" do
    assign(:template_to_pipelines, {"template1" => to_list([CaseInsensitiveString.new("pipeline1"), CaseInsensitiveString.new("pipeline2")])})

    render

    Capybara.string(response.body).find('.templates').tap do |templates|
      expect(templates).to have_selector("a[href='#'][class='link_as_button primary']", :text => "Add New Template")
    end
  end

  it "should display a edit permissions link next to the template name" do

    assign(:template_to_pipelines, {"unused_template" => to_list([]), "used_template" => to_list([CaseInsensitiveString.new("pipeline")])})

    render

    Capybara.string(response.body).find('.templates').tap do |templates|
      templates.all(".template").tap do |template_list|
        expect(template_list[0]).to have_selector("h2", :text => "unused_template")
        template_list[0].find("a[href='#{edit_template_permissions_path(:template_name => "unused_template")}'][class='action_icon lock_icon']") do |a|
          expect(a).to have_selector("span", :text => "Permissions")
        end
      end
    end
  end

  it "should display a delete button next to the template name" do
    allow(view).to receive(:is_user_a_template_admin_for_template?).and_return(true)
    assign(:template_to_pipelines, {"unused_template" => to_list([]), "used_template" => to_list([CaseInsensitiveString.new("pipeline")])})

    render

    Capybara.string(response.body).find('.templates').tap do |templates|
      templates.all(".template").tap do |template_list|
        expect(template_list[0]).to have_selector("h2", :text => "unused_template")
        expect(template_list[0]).to have_selector(".information", :text => "No pipelines associated with this template")
        template_list[0].find("form#delete_template_unused_template[action='#{delete_template_path(:pipeline_name => "unused_template")}'][method='post']") do |form|
          expect(form).to have_selector("input[type='hidden'][name='_method'][value='delete']", visible: :hidden)
          expect(form).to have_selector("span#trigger_delete_unused_template.delete_parent[title='Delete this template']")
          expect(form).to have_selector("div#warning_prompt[style='display:none;']", {text: /Are you sure you want to delete the template 'unused_template' \?/, visible: :hidden})
        end
      end
      templates.all(".template").tap do |template_list|
        expect(template_list[1]).to have_selector("h2", :text => "used_template")
        expect(template_list[1]).to have_selector("span.delete_icon_disabled[title=\"Cannot delete the template 'used_template' as it is used by pipeline(s): '[pipeline]'\"]")
      end
    end
  end

  it "should display the delete button next to the template name for template admin" do
    allow(view).to receive(:is_user_a_template_admin_for_template?).and_return(true)
    allow(view).to receive(:is_user_an_admin?).and_return(false)
    assign(:template_to_pipelines, {"unused_template" => to_list([])})

    render

    Capybara.string(response.body).find('.templates').tap do |templates|
      templates.find(".template") do |template|
        expect(template).to have_selector("h2", :text => "unused_template")
        expect(template).to have_selector(".information", :text => "No pipelines associated with this template")
        template.find("form#delete_template_unused_template[action='#{delete_template_path(:pipeline_name => "unused_template")}'][method='post']") do |form|
          expect(form).to have_selector("input[type='hidden'][name='_method'][value='delete']", visible: :hidden)
          expect(form).to have_selector("span#trigger_delete_unused_template.delete_parent[title='Delete this template']")
          expect(form).to have_selector("div#warning_prompt[style='display:none;']", {text: /Are you sure you want to delete the template 'unused_template' \?/, visible: :hidden})
        end

      end
    end
  end

  it 'should disable the delete button if user not not a template admin or super admin' do
    allow(view).to receive(:is_user_an_admin?).and_return(false)
    assign(:template_to_pipelines, {"unused_template" => to_list([])})

    render

    Capybara.string(response.body).find('.templates').tap do |templates|
      templates.find(".template") do |template|
        expect(template).to have_selector("h2", :text => "unused_template")
        expect(template).to have_selector("span.delete_icon_disabled[title='You are unauthorized to perform this operation. Please contact a Go System Administrator to delete this template.']")
      end
    end
  end

  it "should display a delete button next to the template name if user is both template admin and super admin" do
    allow(view).to receive(:is_user_a_template_admin_for_template?).and_return(true)
    allow(view).to receive(:is_user_an_admin?).and_return(true)

    assign(:template_to_pipelines, {"unused_template" => to_list([]), "used_template" => to_list([CaseInsensitiveString.new("pipeline")])})

    render

    Capybara.string(response.body).find('.templates').tap do |templates|
      templates.all(".template").tap do |template_list|
        expect(template_list[0]).to have_selector("h2", :text => "unused_template")
        expect(template_list[0]).to have_selector(".information", :text => "No pipelines associated with this template")
        template_list[0].find("form#delete_template_unused_template[action='#{delete_template_path(:pipeline_name => "unused_template")}'][method='post']") do |form|
          expect(form).to have_selector("input[type='hidden'][name='_method'][value='delete']", visible: :hidden)
          expect(form).to have_selector("span#trigger_delete_unused_template.delete_parent[title='Delete this template']")
          expect(form).to have_selector("div#warning_prompt[style='display:none;']", {text: /Are you sure you want to delete the template 'unused_template' \?/, visible: :hidden})
        end
      end
      templates.all(".template").tap do |template_list|
        expect(template_list[1]).to have_selector("h2", :text => "used_template")
        expect(template_list[1]).to have_selector("span.delete_icon_disabled[title*=\"Cannot delete the template 'used_template' as it is used by pipeline(s): '[pipeline]'\"]")
      end
    end
  end

  it "should disable the edit permissions link next to the template name for template admin and not super admin" do
    allow(view).to receive(:has_admin_permissions_for_pipeline?).and_return(false)
    allow(view).to receive(:is_user_a_template_admin_for_template?).and_return(true)
    allow(view).to receive(:is_user_an_admin?).and_return(false)

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
    allow(view).to receive(:is_user_a_template_admin_for_template?).and_return(true)
    allow(view).to receive(:is_user_an_admin?).and_return(true)

    assign(:template_to_pipelines, {"used_template" => to_list([CaseInsensitiveString.new("pipeline")])})

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
    allow(view).to receive(:is_user_a_template_admin_for_template?).and_return(true)
    assign(:template_to_pipelines, {"unused_template" => to_list([]), "used_template" => to_list([CaseInsensitiveString.new("pipeline")])})

    render

    Capybara.string(response.body).find('.templates').tap do |templates|
      templates.all("div.template").tap do |template_list|
        expect(template_list[0]).to have_selector("h2", :text => "unused_template")
        expect(template_list[0]).to have_selector("a[href='#{template_edit_path(:pipeline_name => "unused_template", :current_tab => "general", :stage_parent => "templates")}']")
      end
    end
  end

  it 'should display the view button next to the template name' do
    assign(:template_to_pipelines, {"unused_template" => to_list([])})

    render

    Capybara.string(response.body).find('.templates').tap do |templates|
      templates.all(".template").tap do |template_list|
        expect(template_list[0]).to have_selector("h2", :text => "unused_template")
        expect(template_list[0]).to have_selector("a[href='#{config_view_templates_show_path(:name => "unused_template")}']", text: 'View')
      end
    end
  end
end
