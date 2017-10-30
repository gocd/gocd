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

require 'rails_helper'

describe JettyWeakEtagMiddleware do
  before(:each) do
    @app = double('rack-app')
    @middleware = JettyWeakEtagMiddleware.new(@app)
  end

  %w(gzip deflate).each do |compression_type|
    it "should remove --#{compression_type} suffix inserted by GzipFilter from jetty" do
      env = {
        'HTTP_IF_MATCH' => %Q{"foobar--#{compression_type}"},
        'HTTP_IF_NONE_MATCH' => %Q{"foobar--#{compression_type}"}
      }

      expect(@app).to receive(:call).with({
                                        'HTTP_IF_MATCH' => '"foobar"',
                                        'HTTP_IF_NONE_MATCH' => '"foobar"'
                                      })
      @middleware.call(env)
    end
  end
end
