# Licensed to the Software Freedom Conservancy (SFC) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The SFC licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.

require 'zip'
require 'tempfile'
require 'find'
require 'base64'

module Selenium
  module WebDriver
    #
    # @api private
    #

    module Zipper
      EXTENSIONS = %w[.zip .xpi].freeze

      class << self
        def unzip(path)
          destination = Dir.mktmpdir('webdriver-unzip')
          FileReaper << destination

          Zip::File.open(path) do |zip|
            zip.each do |entry|
              to      = File.join(destination, entry.name)
              dirname = File.dirname(to)

              FileUtils.mkdir_p dirname unless File.exist? dirname
              zip.extract(entry, to)
            end
          end

          destination
        end

        def zip(path)
          with_tmp_zip do |zip|
            ::Find.find(path) do |file|
              unless File.directory?(file)
                add_zip_entry zip, file, file.sub("#{path}/", '')
              end
            end

            zip.commit
            File.open(zip.name, 'rb') { |io| Base64.strict_encode64 io.read }
          end
        end

        def zip_file(path)
          with_tmp_zip do |zip|
            add_zip_entry zip, path, File.basename(path)

            zip.commit
            File.open(zip.name, 'rb') { |io| Base64.strict_encode64 io.read }
          end
        end

        private

        def with_tmp_zip(&blk)
          # can't use Tempfile here since it doesn't support File::BINARY mode on 1.8
          # can't use Dir.mktmpdir(&blk) because of http://jira.codehaus.org/browse/JRUBY-4082
          tmp_dir = Dir.mktmpdir
          zip_path = File.join(tmp_dir, 'webdriver-zip')

          begin
            Zip::File.open(zip_path, Zip::File::CREATE, &blk)
          ensure
            FileUtils.rm_rf tmp_dir
            FileUtils.rm_rf zip_path
          end
        end

        def add_zip_entry(zip, file, entry_name)
          entry = Zip::Entry.new(zip.name, entry_name)
          entry.follow_symlinks = true

          zip.add entry, file
        end
      end
    end # Zipper
  end # WebDriver
end # Selenium
