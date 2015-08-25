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

describe "admin/garage/index.html.erb" do
  before :each do
    @garage_data = double('garage data')
    view.should_receive(:garage_gc_path).and_return('garage_gc_path')
  end

  it 'should show config.git details' do
    size = "42MB"
    @garage_data.should_receive(:getConfigRepositorySize).and_return(size)
    assign(:garage_data, @garage_data)

    render

    Capybara.string(response.body).find('div#config_version_content').tap do |div|
      expect(div).to have_selector("h3", :text => "Config Version Control GC")
      expect(div).to have_selector("p", :text => "Current size of the config.git directory is: #{size}")
      div.find("form[action='garage_gc_path'][method='post']").tap do |form|
        expect(form).to have_selector("input[type='submit'][value='Perform GC']")
      end
    end
  end

  it 'should show config.git flash messages' do
    size = "42MB"
    @garage_data.should_receive(:getConfigRepositorySize).and_return(size)
    assign(:garage_data, @garage_data)
    flash = {:notice => {:gc => "notice"}, :error => {:gc => "error"}}
    view.should_receive(:flash).at_least(:once).and_return(flash)

    render

    Capybara.string(response.body).find('div#config_version_content').tap do |div|
      div.find("div.flash").tap do |flash|
        expect(flash).to have_selector("p.notice code", :text => "notice")
        expect(flash).to have_selector("p.error code", :text => "error")
      end
    end
  end
end
