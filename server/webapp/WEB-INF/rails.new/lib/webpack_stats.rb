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

# copy of some code from `webpack-rails` so that asking for assets of an entry-point will return all its deps as well
class WebpackStats

  def asset_paths(source)
    raise StandardError, data["errors"] unless manifest_bundled?

    if data["entrypoints"][source] && paths = data["entrypoints"][source]['assets']
      # # Can be either a string or an array of strings.
      # # Do not include source maps as they are not javascript
      [paths].flatten.reject { |p| p =~ /.*\.map$/ }.map do |p|
        "/go/assets/webpack/#{p}"
      end
    else
      raise EntryPointMissingError, "Can't find entry point '#{source}' in webpack manifest"
    end
  end

  def refresh
    @data = load
  end

  def manifest_bundled?
    !data["errors"].any? { |error| error.include? "Module build failed" }
  end

  def data
    if config.cache_manifest?
      @data ||= load
    else
      refresh
    end
  end

  def load
    if manifest_stats_file.exist?
      JSON.parse(manifest_stats_file.read)
    elsif dev_server.running?
      require 'open-uri'
      JSON.parse(open("#{dev_server.protocol}://#{dev_server.host_with_port}/go/#{Webpacker.config.public_output_path.relative_path_from(Webpacker.config.public_path)}/manifest-stats.json").read)
    else

      raise StandardError, "Could not load manifest from #{manifest_stats_file}. Have you run `bin/webpack` or `bin/webpack-dev-server`"
    end
  end

  def dev_server
    Webpacker.instance.dev_server
  end

  def manifest_stats_file
    Rails.root.join(config.public_output_path, 'manifest-stats.json')
  end

  private

  def config
    Webpacker.instance.config
  end
end
