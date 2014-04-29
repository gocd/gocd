# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements. See the NOTICE file distributed with this
# work for additional information regarding copyright ownership. The ASF
# licenses this file to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
# WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
# License for the specific language governing permissions and limitations under
# the License.

module Buildr
  module Wsgen
    class << self

      # :call-seq:
      #    java2wsdl(project, classnames, options) => String
      #
      # Uses wsgen to generate wsdl files form annotated, compiled classes. The parameters are
      # * :project -- The project in which the classes are compiled.
      # * :classnames -- Either an array of classnames to convert to wsdl or a map keyed on classnames and service
      #                  specific customizations provided.
      #
      # Service options include:
      # * :service_name -- The name of the service.
      # * :namespace_url -- The namespace of the service.
      #
      # Method options include:
      # * :output_dir -- The target directory.
      # * :namespace_url -- The default namespace for the services.
      #
      # For example:
      #  Buildr::Wsgen.java2wsdl(project, %w(com.example.MyService))
      #  Buildr::Wsgen.java2wsdl(project, %w(com.example.MyService com.example.MyOtherService))
      #  Buildr::Wsgen.java2wsdl(project, %w(com.example.MyService com.example.MyOtherService), :namespace_url => "http://example.com/services")
      #  Buildr::Wsgen.java2wsdl(project, {"com.example.MyService" => {:service_name => 'MiaService', :namespace_url => "http://example.com/it/services"}))
      def java2wsdl(project, classnames, options = {})
        desc "Generate wsdl from java"
        project.task("java2wsdl").enhance([project.compile.target])

        base_wsdl_dir = File.expand_path(options[:output_dir] || project._(:target, :generated, :wsgen, :main, :wsdl))
        project.iml.main_source_directories << base_wsdl_dir if project.iml?
        project.file(base_wsdl_dir)
        project.task("java2wsdl").enhance([base_wsdl_dir])

        services = classnames.is_a?(Array) ? classnames.inject({}) {|result, element| result[element] = {}; result} : classnames

        services.each_pair do |classname, config|

          name_parts = classname.split('.')
          service_name = config[:service_name] || name_parts.last
          namespace_url = config[:namespace_url] || options[:namespace_url] || "http://#{name_parts[0...-1].reverse.join('.')}"
          wsdl_file = File.expand_path("#{base_wsdl_dir}/META-INF/wsdl/#{service_name}.wsdl")

          project.file(wsdl_file) do
            mkdir_p File.dirname(wsdl_file)
            cp = Buildr.artifacts(project.compile.dependencies + [project.compile.target]).map(&:to_s).join(File::PATH_SEPARATOR)

            java_dir = project._(:target, :ignored, :wsgen, :main, :java)
            intermediate_dir = project._(:target, :ignored, :wsgen, :main, :java)

            rm_rf java_dir
            rm_rf intermediate_dir
            mkdir_p java_dir
            mkdir_p intermediate_dir

            args = []
            args << "-keep"
            args << "-inlineSchemas" if (options[:inlineSchemas] && ENV_JAVA['java.version'] >= '1.7')
            args << "-wsdl"
            args << "-servicename"
            args << "{#{namespace_url}}#{service_name}"
            args << "-portname"
            args << "{#{namespace_url}}#{service_name}Port"
            args << "-d "
            args << intermediate_dir
            args << "-r"
            args << "#{base_wsdl_dir}/META-INF/wsdl"
            args << "-keep"
            args << "-s"
            args << java_dir
            args << "-cp"
            args << cp
            args << classname

            command = "wsgen #{args.join(' ')}"
            trace command
            sh command
            if $? != 0
              raise "Problem building wsdl"
            end

            content = IO.read(wsdl_file).gsub('REPLACE_WITH_ACTUAL_URL', "http://example.com/#{service_name}")
            File.open(wsdl_file, 'wb') { |f| f.write content }
          end

          project.file(base_wsdl_dir).enhance([wsdl_file])
          project.task("java2wsdl").enhance([wsdl_file])
        end

        base_wsdl_dir
      end

      # :call-seq:
      #    wsdl2java(project, wsdls, options) => String
      #
      # Uses wsgen to generate java files form wsdls. The parameters are
      # * :project -- The project in which the classes are compiled.
      # * :wsdls -- A hash of wsdl filenames to service configuration.
      #
      # Service options include:
      # * :service_name -- The name of the service.
      # * :package -- The package in which to generate the code.
      #
      # Method options include:
      # * :output_dir -- The target directory.
      # * :target -- The target version for generated source..
      # * :package -- The default package in which to generate the code.
      #
      # For example:
      #  Buildr::Wsgen.wsdl2java(project, {_('src/main/wsdl/MyService.wsdl') => {}})
      #  Buildr::Wsgen.wsdl2java(project, {_('src/main/wsdl/MyService.wsdl') => {:package => 'com.example'}})
      #  Buildr::Wsgen.wsdl2java(project, {_('src/main/wsdl/MyService.wsdl') => {:output_dir => _(:target, :wsdl, :java)}})
      #  Buildr::Wsgen.wsdl2java(project, {_('src/main/wsdl/MyService.wsdl') => {}}, :package => 'com.example' )
      #  Buildr::Wsgen.wsdl2java(project, {_('src/main/wsdl/MyService.wsdl') => {}}, :wsdl_location => 'file:META-INF/wsdl/SpecificTaskService.wsdl' )
      def wsdl2java(project, wsdls, options = {})
        desc "Generate java from wsdl"
        project.task("wsdl2java")

        ws_dir = File.expand_path(options[:output_dir] || project._(:target, :generated, "ws/main/java"))
        project.file(ws_dir)
        project.task('wsdl2java').enhance([ws_dir])

        target = options[:target] || '2.1'

        wsdls.each_pair do |wsdl_file, config|
          pkg = config[:package] || options[:package]
          service = config[:service] || File.basename(wsdl_file, '.wsdl')
          wsdl_location = config[:wsdl_location]
          java_file = "#{ws_dir}/#{pkg.gsub('.', '/')}/#{service}.java"
          project.file(java_file => [project.file(wsdl_file)]) do
            mkdir_p ws_dir
            command = []
            command << "wsimport"
            command << "-keep"
            command << "-Xnocompile"
            command << "-target"
            command << target
            command << "-s"
            command << ws_dir
            command << "-p"
            command << pkg
            if wsdl_location
              command << "-wsdllocation"
              command << wsdl_location
            end
            command << wsdl_file

            `#{command.join(' ')}`
            if $? != 0 || !File.exist?(java_file)
              rm_rf java_file
              raise "Problem building webservices"
            end
          end
          project.file(ws_dir).enhance([java_file])
        end

        project.compile.from ws_dir
        project.iml.main_source_directories << ws_dir if project.iml?
        project.compile.enhance(['wsdl2java'])

        ws_dir
      end
    end
  end
end
