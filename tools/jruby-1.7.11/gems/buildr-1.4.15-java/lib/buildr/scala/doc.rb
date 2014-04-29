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

module Buildr #:nodoc:
  module Doc #:nodoc:

    module ScaladocDefaults
      include Extension

      # Default scaladoc -doc-title to project's comment or name
      after_define(:scaladoc => :doc) do |project|
        if project.doc.engine? Scaladoc
          options = project.doc.options
          if Scala.version?(2.7)
            options[:windowtitle] = (project.comment || project.name) unless options[:windowtitle]
          else
            doc_title = "doc-title".to_sym
            options[doc_title] = (project.comment || project.name) unless options[doc_title]
            options.delete(:windowtitle) if options[:windowtitle]
          end
        end
      end
    end

    class Scaladoc < Base
      specify :language => :scala, :source_ext => 'scala'

      def generate(sources, target, options = {})
        cmd_args = [ '-d', target]
        cmd_args << '-verbose' if trace?(:scaladoc)
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
          info "Generating Scaladoc for #{project.name}"
          trace (['scaladoc'] + cmd_args).join(' ')
          Java.load
          begin
            if Scala.version?(2.7, 2.8)
              Java.scala.tools.nsc.ScalaDoc.process(cmd_args.to_java(Java.java.lang.String))
            else
              scaladoc = Java.scala.tools.nsc.ScalaDoc.new
              scaladoc.process(cmd_args.to_java(Java.java.lang.String))
            end
          rescue => e
            fail 'Failed to generate Scaladocs, see errors above: ' + e
          end
        end
      end
    end

    class VScaladoc < Base
      VERSION = '1.2-m1'
      Buildr.repositories.remote << 'https://oss.sonatype.org/content/groups/scala-tools'

      class << self
        def dependencies
          case
            when Buildr::Scala.version?("2.7")
              [ "org.scala-tools:vscaladoc:jar:#{VERSION}" ]
            else
              warn "VScalaDoc not supported for Scala 2.8+"
              []
          end
        end
      end

      Java.classpath << lambda { dependencies }

      specify :language => :scala, :source_ext => 'scala'

      def generate(sources, target, options = {})
        cmd_args = [ '-d', target, (trace?(:vscaladoc) ? '-verbose' : ''),
          '-sourcepath', project.compile.sources.join(File::PATH_SEPARATOR) ]
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
          info "Generating VScaladoc for #{project.name}"
          trace (['vscaladoc'] + cmd_args).join(' ')
          Java.load
          Java.org.scala_tools.vscaladoc.Main.main(cmd_args.to_java(Java.java.lang.String)) == 0 or
            fail 'Failed to generate VScaladocs, see errors above'
        end
      end
    end
  end

  module Packaging
    module Scala
      def package_as_scaladoc_spec(spec) #:nodoc:
        spec.merge(:type=>:jar, :classifier=>'scaladoc')
      end

      def package_as_scaladoc(file_name) #:nodoc:
        ZipTask.define_task(file_name).tap do |zip|
          zip.include :from=>doc.target
        end
      end
    end
  end

  class Project #:nodoc:
    include ScaladocDefaults
    include Packaging::Scala
  end
end

Buildr::Doc.engines << Buildr::Doc::Scaladoc
Buildr::Doc.engines << Buildr::Doc::VScaladoc
