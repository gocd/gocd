# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with this
# work for additional information regarding copyright ownership.  The ASF
# licenses this file to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
# WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
# License for the specific language governing permissions and limitations under
# the License.

autoload :RubyForge, 'rubyforge'
Gem.autoload :Package, 'rubygems/package'

module Buildr

  class PackageGemTask < ArchiveTask

    def initialize(*args)
      super
      @spec = Gem::Specification.new
      prepare do
        include(changelog) if changelog
      end
    end

    attr_accessor :changelog

    def spec
      yield @spec if block_given?
      @spec
    end

    def upload
      rubyforge = RubyForge.new
      rubyforge.login
      rubyforge.userconfig.merge!('release_changes'=>changelog.to_s, 'preformatted'=>true) if changelog
      rubyforge.add_release spec.rubyforge_project.downcase, spec.name.downcase, spec.version, package(:gem).to_s
    end

  private

    def create_from(file_map)
      spec.mark_version
      spec.validate

      File.open(name, 'wb') do |io|
        Gem::Package.open(io, 'w', nil) do |pkg|
          pkg.metadata = spec.to_yaml
          file_map.each do |path, content|
            next if content.nil? || File.directory?(content.to_s)
            pkg.add_file_simple(path, File.stat(content.to_s).mode & 0777, File.size(content.to_s)) do |os|
              File.open(content.to_s, "rb") do |file|
                os.write file.read(4096)  until file.eof?
              end
            end
          end
        end
      end
    end

  end


  module PackageAsGem #:nodoc:

    def package_as_gem(file_name) #:nodoc:
      PackageGemTask.define_task(file_name).tap do |gem|
        %w{ lib test doc }.each do |dir|
          gem.include :from=>_(dir), :path=>dir if File.directory?(_(dir))
        end
        gem.spec do |spec|
          spec.name = id
          spec.version = version.gsub('-','.') # RubyGems doesn't like '-' in version numbers
          spec.summary = full_comment
          spec.has_rdoc = true
          spec.rdoc_options << '--title' << comment
          spec.require_path = 'lib'
        end
      end
    end

  end

  class Project
    include PackageAsGem
  end

end
