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

require File.join(File.dirname(__FILE__), "/../../../../spec_helper")

def with_listItem class_name, key, value
  with_tag("li.#{class_name}") do
    with_tag("span.key", "#{key}")
    with_tag("span.value", "#{value}")
  end
end

describe "admin/plugins/plugins/index.html.erb" do

  it "should list a set of plugins" do
    assign(:plugin_descriptors, [valid_descriptor("1"), invalid_descriptor('2', ['message1', 'message2'])])

    render "admin/plugins/plugins/index.html.erb"

    response.should have_tag("div#plugins-listing") do
      without_tag("div.information")
      with_tag("ul.plugins") do

        with_tag("li.plugin.enabled[id='plugin1.id']") do

          with_tag("div.plugin-details") do
            with_tag("span.name", "Name is 1")
            with_tag("span.descriptor-id", "[plugin1.id]")
            with_tag("span.smaller.version", "1.0.0")
            with_tag("span.plugin-author.smaller") do
              with_tag("span.key", "Author")
              with_tag("span.value") do
                with_tag("a[href='http://url/for/plugin/1']", "ThoughtWorks Go Team - Plugin 1")
              end
            end
          end

          with_tag("div.description", "Description for 1")

          with_tag("ul.more-info-detail") do
            with_listItem("plugin-location", "Loaded from:", "/path/to/plugin1.jar")
            with_listItem("plugin-target-oses", "Target operating systems:", "Linux, Windows")
            with_listItem("plugin-target-go-version", "Target Go Version:", "13.3.0")
            with_listItem("plugin-bundled-status", "Bundled:", "Yes")
          end
        end

        with_tag("li.plugin[id='plugin2.id']") do

          with_tag("div.plugin-details") do
            with_tag("span.name", "plugin2.id")
            with_tag("span.version", "")
            with_tag("span.plugin-author.smaller") do
              with_tag("span.key", "Author")
              with_tag("span.value", "Unknown")
            end
          end

          with_tag("div.description", "No description available.")

          with_tag("ul.more-info-detail") do
            with_listItem("plugin-location", "Loaded from:", "/path/to/plugin2.jar")
            with_listItem("plugin-target-oses", "Target operating systems:", "No restrictions")
            with_listItem("plugin-target-go-version", "Target Go Version:", "Unknown")
            with_listItem("plugin-bundled-status", "Bundled:", "No")
          end
        end
      end
    end
  end

  it "should add http:// to url if not specified" do
    assign(:plugin_descriptors, [valid_descriptor_without_http("1")])

    render "admin/plugins/plugins/index.html.erb"

    response.should have_tag("div#plugins-listing") do
      with_tag("ul.plugins") do
        with_tag("li.plugin.enabled[id='plugin1.id']") do
          with_tag("span.plugin-author") do
            with_tag("span.key", "Author")
            with_tag("span.value") do
              with_tag("a[href='http://url/for/plugin/1'][target='_blank']", "ThoughtWorks Go Team - Plugin 1")
            end
          end
        end
      end
    end
  end

  it "should not have a messages section when there are no messages" do
    description = valid_descriptor("1")
    assign(:plugin_descriptors, [description])

    render "admin/plugins/plugins/index.html.erb"

    description.status().messages().length.should be 0
    response.should have_tag("div#plugins-listing") do
      with_tag("li.plugin.enabled[id='plugin1.id']") do
        without_tag("div.plugin-messages")
      end
    end
  end

  it "should have a messages section when there are messages" do
    description = invalid_descriptor('2', ['message1', 'message2'])
    assign(:plugin_descriptors, [description])

    render "admin/plugins/plugins/index.html.erb"

    response.should have_tag("div#plugins-listing") do
      with_tag("li.plugin.disabled[id='plugin2.id']") do
        with_tag("div.plugin-messages") do
          with_tag("span", "Messages:")
          with_tag("ul") do
            with_tag("li", "message1")
            with_tag("li", "message2")
          end
        end
      end
    end
  end

  it "should display proper message when no plugins are found" do
    assign(:plugin_descriptors, [])

    render "admin/plugins/plugins/index.html.erb"

    response.should have_tag("div#plugins-listing") do
      with_tag("div.information", "No plugins found. Please drop your plugin here at this location:")
      without_tag("li.plugin")
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