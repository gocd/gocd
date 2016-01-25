# encoding: UTF-8
##########################################################################
# Copyright 2016 ThoughtWorks, Inc.
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
##########################################################################
require 'spec_helper'

describe "/navigation_elements/_footer" do

  partial_page = "navigation_elements/footer"
  it 'should have copyright, license and third-party information in the footer' do
    render :partial => partial_page

    Capybara.string(response.body).find('p.copyright').tap do |paragraph|
      expect(paragraph).to have_text('Copyright © 2016 ThoughtWorks, Inc. Licensed under Apache License, Version 2.0. Go includes third-party software.')
      expect(paragraph).to have_selector("a[href='http://www.thoughtworks.com/products'][target='_blank']", text: 'ThoughtWorks, Inc.')
      expect(paragraph).to have_selector("a[href='http://www.apache.org/licenses/LICENSE-2.0'][target='_blank']", text: 'Apache License, Version 2.0')
      expect(paragraph).to have_selector("a[href='/NOTICE/cruise_notice_file.pdf'][target='_blank']", text: 'third-party software')
    end
  end

  it 'should have miscellaneous footer links' do
    render :partial => partial_page

    assert_links= {
      'twitter'        => 'http://twitter.com/goforcd',
      'github'         => 'https://github.com/gocd/gocd',
      'forums'         => 'https://groups.google.com/d/forum/go-cd',
      'documentation'  => 'https://go.cd/current/documentation',
      'plugins'        => 'http://www.go.cd/community/plugins.html',
      'api'            => 'https://api.go.cd',
      'server-details' => url_for_path('about'),
      'cctray'         => url_for_path('cctray.xml')
    }

    expect(response.body).to_not have_selector("a[href='http://www.thoughtworks.com/products/support']", text: 'Support')

    Capybara.string(response.body).find('span.social').tap do |links|
      assert_links.each do |key, value|
        expect(links).to have_selector("a[href='#{value}'][class='#{key}']")
      end
    end
  end
end
