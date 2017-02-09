##########################################################################
# Copyright 2017 ThoughtWorks, Inc.
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

unless Rails.env.production?
  module Webpack
    module Rails
      raise 'Monkey patch tested only on 0.9.10' if Webpack::Rails::VERSION != '0.9.10'
      class <<self
        # monkey patch to prevent caching of the manifest in dev mode
        def manifest
          load_manifest
        end
      end
    end
  end
end

Rails.application.config.webpack.dev_server.enabled = false
Rails.application.config.webpack.output_dir = 'public/assets/webpack'
Rails.application.config.webpack.public_path = 'go/assets/webpack'
