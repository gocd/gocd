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

    module JavadocDefaults
      include Extension

      # Default javadoc -windowtitle to project's comment or name
      after_define(:javadoc => :doc) do |project|
        if project.doc.engine? Javadoc
          options = project.doc.options
          options[:windowtitle] = (project.comment || project.name) unless options[:windowtitle]
        end
      end
    end

    # A convenient task for creating Javadocs from the project's compile task. Minimizes all
    # the hard work to calling #from and #using.
    #
    # For example:
    #   doc.from(projects('myapp:foo', 'myapp:bar')).using(:windowtitle=>'My App')
    # Or, short and sweet:
    #   desc 'My App'
    #   define 'myapp' do
    #     . . .
    #     doc projects('myapp:foo', 'myapp:bar')
    #   end
    class Javadoc < Base

      specify :language => :java, :source_ext => 'java'

      def generate(sources, target, options = {})
        cmd_args = [ '-d', target, trace?(:javadoc) ? '-verbose' : '-quiet' ]
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
          info "Generating Javadoc for #{project.name}"
          trace (['javadoc'] + cmd_args).join(' ')
          Java.load
          Java.com.sun.tools.javadoc.Main.execute(cmd_args.to_java(Java.java.lang.String)) == 0 or
            fail 'Failed to generate Javadocs, see errors above'
        end
      end
    end
  end

  class Project
    include JavadocDefaults
  end
end

Buildr::Doc.engines << Buildr::Doc::Javadoc
