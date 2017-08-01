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

def with_listItem ul, class_name, key, value
  ul.find("li.#{class_name}").tap do |li|
    expect(li).to have_selector("span.key", :text => "#{key}")
    expect(li).to have_selector("span.value", :text => "#{value}")
  end
end

describe "admin/plugins/plugins/index.html.erb" do
  before :each do
    assign(:meta_data_store, @meta_data_store = double('metadata store'))
    view.stub(:can_edit_plugin_settings?).and_return(true)
  end

  it "should have a form to upload plugins" do
    assign(:upload_feature_enabled, true)
    assign(:plugin_descriptors, [valid_descriptor("1")])

    render

    plugin_upload = Capybara.string(response.body).find('div#plugins-listing')
    expect(plugin_upload).to have_selector("form")
  end

  it "should hide thNe upload plugins form when the PLUGIN_UPLOAD_FEATURE is off" do
    assign(:upload_feature_enabled, false)
    assign(:plugin_descriptors, [])

    render

    expect(response).to_not have_selector('div#plugins-listing div#upload-plugin')
    expect(response).to_not have_selector('div#plugins-listing form')
  end

  it "should list a set of plugins" do
    assign(:plugin_descriptors, [valid_descriptor("1"), invalid_descriptor('2', ['message1', 'message2'])])

    render

    Capybara.string(response.body).find('div#plugins-listing').tap do |plugins_listing|
      expect(plugins_listing).not_to have_selector("div.information")
      expect(plugins_listing).to have_selector("ul.plugins") do

        plugins_listing.find("li.plugin.enabled[id='plugin1.id']").tap do |li|

          li.find("div.plugin-details").tap do |plugin_details|
            expect(plugin_details).to have_selector("span.name", :text => "Name is 1")
            expect(plugin_details).to have_selector("span.descriptor-id", :text => "[plugin1.id]")
            expect(plugin_details).to have_selector("span.smaller.version", :text => "1.0.0")
            plugin_details.find("span.plugin-author.smaller").tap do |plugin_author|
              expect(plugin_author).to have_selector("span.key", :text => "Author")
              plugin_author.find("span.value").tap do |span|
                expect(span).to have_selector("a[href='http://url/for/plugin/1']", :text => "ThoughtWorks Go Team - Plugin 1")
              end
            end
          end

          expect(plugins_listing).to have_selector("div.description", :text => "Description for 1")

          li.find("ul.more-info-detail").tap do |ul|
            with_listItem(ul, "plugin-location", "Loaded from:", "/path/to/plugin1.jar")
            with_listItem(ul, "plugin-target-oses", "Target operating systems:", "Linux, Windows")
            with_listItem(ul, "plugin-target-go-version", "Target Go Version:", "13.3.0")
            with_listItem(ul, "plugin-bundled-status", "Bundled:", "Yes")
          end
        end

        plugins_listing.find("li.plugin[id='plugin2.id']").tap do |li|

          li.find("div.plugin-details").tap do |plugin_details|
            expect(plugin_details).to have_selector("span.name", :text => "plugin2.id")
            expect(plugin_details).to have_selector("span.version", :text => "")
            plugin_details.find("span.plugin-author.smaller").tap do |plugin_author|
              expect(plugin_author).to have_selector("span.key", :text => "Author")
              expect(plugin_author).to have_selector("span.value", :text => "Unknown")
            end
          end

          expect(plugins_listing).to have_selector("div.description", :text => "No description available.")

          li.find("ul.more-info-detail").tap do |ul|
            with_listItem(ul, "plugin-location", "Loaded from:", "/path/to/plugin2.jar")
            with_listItem(ul, "plugin-target-oses", "Target operating systems:", "No restrictions")
            with_listItem(ul, "plugin-target-go-version", "Target Go Version:", "Unknown")
            with_listItem(ul, "plugin-bundled-status", "Bundled:", "No")
          end
        end
      end
    end
  end

  it "should add http:// to url if not specified" do
    assign(:plugin_descriptors, [valid_descriptor_without_http("1")])

    render

    Capybara.string(response.body).find('div#plugins-listing').tap do |plugins_listing|
      plugins_listing.find("ul.plugins").tap do |ul|
        ul.find("li.plugin.enabled[id='plugin1.id']").tap do |li|
          li.find("span.plugin-author").tap do |span|
            expect(span).to have_selector("span.key", :text => "Author")
            span.find("span.value").tap do |value|
              expect(value).to have_selector("a[href='http://url/for/plugin/1'][target='_blank']", :text => "ThoughtWorks Go Team - Plugin 1")
            end
          end
        end
      end
    end
  end

  it "should not have a messages section when there are no messages" do
    description = valid_descriptor("1")
    assign(:plugin_descriptors, [description])

    render

    description.status().messages().length.should be 0

    Capybara.string(response.body).find('div#plugins-listing').tap do |plugins_listing|
      plugins_listing.find("li.plugin.enabled[id='plugin1.id']").tap do |li|
        expect(li).not_to have_selector("div.plugin-messages")
      end
    end
  end

  it "should have a messages section when there are messages" do
    description = invalid_descriptor('2', ['message1', 'message2'])
    assign(:plugin_descriptors, [description])

    render

    Capybara.string(response.body).find('div#plugins-listing').tap do |plugins_listing|
      plugins_listing.find("li.plugin.disabled[id='plugin2.id']").tap do |li|
        li.find("div.plugin-messages").tap do |plugin_messages|
          expect(plugin_messages).to have_selector("span", :text => "Messages:")
          plugin_messages.find("ul").tap do |ul|
            expect(ul).to have_selector("li", :text => "message1")
            expect(ul).to have_selector("li", :text => "message2")
          end
        end
      end
    end
  end

  it "should display proper message when no plugins are found" do
    assign(:plugin_descriptors, [])

    render

    Capybara.string(response.body).find('div#plugins-listing').tap do |plugins_listing|
      expect(plugins_listing).to have_selector("div.information", :text => "No plugins found. Please drop your plugin here at this location:")
      expect(plugins_listing).not_to have_selector("li.plugin")
    end
  end

  it "should add settings icon if settings is available" do
    allow(view).to receive(:is_user_an_admin?).and_return(true)
    assign(:plugin_descriptors, [valid_descriptor("1")])

    render

    Capybara.string(response.body).find('div#plugins-listing').tap do |plugins_listing|
      plugins_listing.find("li.plugin.enabled[id='plugin1.id']").tap do |li|
        li.find("div.plugin-details").tap do |plugin_details|
          plugin_details.find("span.settings").tap do |span|
            expect(span).to have_selector("a[href='#']")
          end
        end
      end
    end
  end

  it "should not add settings icon if settings is not available" do
    view.should_receive(:can_edit_plugin_settings?).and_return(false)
    assign(:plugin_descriptors, [valid_descriptor("1")])

    render

    Capybara.string(response.body).find('div#plugins-listing').tap do |plugins_listing|
      plugins_listing.find("li.plugin.enabled[id='plugin1.id']").tap do |li|
        li.find("div.plugin-details").tap do |plugin_details|
          expect(plugin_details).not_to have_selector('span.settings')
        end
      end
    end
  end

  it "should not add settings icon if user cannot edit the plugin settings" do
    view.should_receive(:can_edit_plugin_settings?).and_return(false)
    assign(:plugin_descriptors, [valid_descriptor("1")])

    render

    Capybara.string(response.body).find('div#plugins-listing').tap do |plugins_listing|
      plugins_listing.find("li.plugin.enabled[id='plugin1.id']").tap do |li|
        li.find("div.plugin-details").tap do |plugin_details|
          expect(plugin_details).not_to have_selector('span.settings')
        end
      end
    end
  end

  it "should display status report link if user is an admin" do
    view.should_receive(:plugin_supports_status_report?).and_return(true)
    view.should_receive(:is_user_an_admin?).and_return(true)
    assign(:plugin_descriptors, [valid_descriptor("1")])

    render

    Capybara.string(response.body).find('div#plugins-listing').tap do |plugins_listing|
      plugins_listing.find("li.plugin.enabled[id='plugin1.id']").tap do |li|
        li.find("div.plugin-details").tap do |plugin_details|
          expect(plugin_details.find('a.btn-primary.status-report-btn')).to have_content('Status Report')
        end
      end
    end
  end

  it "should not display status report link if user is not an admin" do
    view.should_receive(:plugin_supports_status_report?).and_return(true)
    view.should_receive(:is_user_an_admin?).and_return(false)
    assign(:plugin_descriptors, [valid_descriptor("1")])

    render

    Capybara.string(response.body).find('div#plugins-listing').tap do |plugins_listing|
      plugins_listing.find("li.plugin.enabled[id='plugin1.id']").tap do |li|
        li.find("div.plugin-details").tap do |plugin_details|
          expect(plugin_details).not_to have_selector('a.btn-primary.status-report-btn')
        end
      end
    end
  end

  def invalid_descriptor name, messages
    descriptor = GoPluginDescriptor.usingId("plugin#{name}.id", "/path/to/plugin#{name}.jar", java.io.File.new('some_random_location_' + name), false)
    descriptor.markAsInvalid(messages, nil)
    GoPluginDescriptorModel::convertToDescriptorWithAllValues descriptor
  end

  def valid_descriptor name
    descriptor = descriptor_which_is_valid name, "http://url/for/plugin/#{name}"
    GoPluginDescriptorModel::convertToDescriptorWithAllValues descriptor
  end

  def valid_descriptor_without_http name
    descriptor = descriptor_which_is_valid name, "url/for/plugin/#{name}"
    GoPluginDescriptorModel::convertToDescriptorWithAllValues descriptor
  end

  private
  def descriptor_which_is_valid name, url
    vendor = GoPluginDescriptor::Vendor.new("ThoughtWorks Go Team - Plugin #{name}", url)
    about = GoPluginDescriptor::About.new("Name is #{name}", "1.0.0", "13.3.0", "Description for #{name}", vendor, ["Linux", "Windows"])
    descriptor = GoPluginDescriptor.new("plugin#{name}.id", "1", about, "/path/to/plugin#{name}.jar", java.io.File.new('some_random_location_' + name), true)
    GoPluginDescriptorModel::convertToDescriptorWithAllValues descriptor
  end
end
