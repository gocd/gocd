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

module Buildr
  module Doc

    module GroovydocDefaults
      include Extension

      # Default groovydoc -doc-title to project's comment or name
      after_define(:groovydoc => :doc) do |project|
        if project.doc.engine? Groovydoc
          options = project.doc.options
          options[:windowtitle] = (project.comment || project.name) unless options[:windowtitle]
        end
      end
    end

    class Groovydoc < Base
      specify :language => :groovy, :source_ext => ['java', 'groovy']

      def generate(sources, target, options = {})
        mkdir_p target
        cmd_args = [ '-d', target, trace?(:groovydoc) ? '-verbose' : nil ].compact
        options.reject { |key, value| [:sourcepath, :classpath].include?(key) }.
          each { |key, value| value.invoke if value.respond_to?(:invoke) }.
          each do |key, value|
            case value
            when true, nil
              cmd_args << "-#{key}"
            when false
              cmd_args << "-no#{key}"
            when Hash
              value.each { |k,v| cmd_args << "-#{key}" << k.to_s << v.to_s }
            else
              cmd_args += Array(value).map { |item| ["-#{key}", item.to_s] }.flatten
            end
          end
        [:sourcepath, :classpath].each do |option|
          Array(options[option]).flatten.tap do |paths|
            cmd_args << "-#{option}" << paths.flatten.map(&:to_s).join(File::PATH_SEPARATOR) unless paths.empty?
          end
        end
        cmd_args += sources.flatten.uniq
        unless Buildr.application.options.dryrun
          info "Generating Groovydoc for #{project.name}"
          trace (['groovydoc'] + cmd_args).join(' ')
          result = Java::Commands.java('org.codehaus.groovy.tools.groovydoc.Main', cmd_args,
                                        :classpath => Buildr::Groovy.dependencies)
        end
      end
    end
  end

  class Project
    include GroovydocDefaults
  end
end

Buildr::Doc.engines << Buildr::Doc::Groovydoc

