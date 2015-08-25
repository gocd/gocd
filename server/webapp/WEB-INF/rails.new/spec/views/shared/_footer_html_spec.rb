# encoding: UTF-8
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

describe "/shared/_footer.html" do

  it 'should have copyright, license and third-party information in the footer' do
    render :partial => "shared/footer"

    Capybara.string(response.body).find('ul.copyright').tap do |ul|
      expect(ul).to have_selector("li", text: 'Copyright © 2015 ThoughtWorks, Inc. Licensed under Apache License, Version 2.0. Go includes third-party software.')

      ul.first('li').tap do |li|
        expect(li).to have_selector("a[href='http://www.thoughtworks.com/products'][target='_blank']", text: 'ThoughtWorks, Inc.')
        expect(li).to have_selector("a[href='http://www.apache.org/licenses/LICENSE-2.0'][target='_blank']", text: 'Apache License, Version 2.0')
        expect(li).to have_selector("a[href='/NOTICE/cruise_notice_file.pdf'][target='_blank']", text: 'third-party software')
      end
    end
  end

  it 'should have miscellaneous footer links with no support link' do
    render :partial => "shared/footer"

    expect(response.body).to_not have_selector("a[href='http://www.thoughtworks.com/products/support']", text: 'Support')

    Capybara.string(response.body).all('ul.links li').tap do |links_li|
      expect(links_li[0]).to have_selector("a[href='/cctray.xml']", text: "(cc) CCTray Feed")
      expect(links_li[1]).to have_selector("a[href='http://api.go.cd']", text: 'APIs')
      expect(links_li[2]).to have_selector("a[href='http://www.go.cd/community/plugins.html']", text: 'Plugins')
      expect(links_li[3]).to have_selector("a[href='http://www.go.cd/community/resources.html']", text: 'Community')
      expect(links_li[4]).to have_selector("a[href='/about']", text: 'Server Details')
    end

    Capybara.string(response.body).find('ul.links li.last').tap do |links_li|
      expect(links_li).to have_selector("a[href='http://www.go.cd/documentation/user/current']", text: 'Help')
    end
  end
end
