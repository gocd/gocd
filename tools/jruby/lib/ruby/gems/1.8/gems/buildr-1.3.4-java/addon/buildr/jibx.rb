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


require 'java/java'


module Buildr

  # Provides JiBX bytecode enhancement. Require explicitly using <code>require 'buildr/jibx'</code>.
  module JiBX 

    JIBX_VERSION = '1.1.5'
    BCEL_VERSION = '5.2'
    STAX_VERSION = '1.0-2'
    XPP3_VERSION = '1.1.4c'

    REQUIRES = [ "org.jibx:jibx-bind:jar:#{JIBX_VERSION}",
      "org.jibx:jibx-run:jar:#{JIBX_VERSION}",
      "org.apache.bcel:bcel:jar:#{BCEL_VERSION}",
      "javax.xml.stream:stax-api:jar:#{STAX_VERSION}",
      "xpp3:xpp3:jar:#{XPP3_VERSION}" ]

    Java.classpath << REQUIRES

    class << self

      def bind(options)
        rake_check_options options, :classpath, :output, :binding, :target, :verbose, :load
        artifacts = Buildr.artifacts(options[:classpath]).each { |a| a.invoke }.map(&:to_s) + [options[:output].to_s]
        binding = file(options[:binding]).tap { |task| task.invoke }.to_s

        Buildr.ant 'jibx' do |ant|
          ant.taskdef :name=>'bind', :classname=>'org.jibx.binding.ant.CompileTask',
            :classpath => requires.join(File::PATH_SEPARATOR)
          ant.bind :verbose => options[:verbose].to_s, :load => options[:load].to_s, :binding=>options[:binding].to_s do
            ant.classpath :path => artifacts.join(File::PATH_SEPARATOR)
          end
        end
      end

    private

      def requires()
        @requires ||= Buildr.artifacts(REQUIRES).each { |artifact| artifact.invoke }.map(&:to_s)
      end

    end

    def jibx_bind(options = nil)
      
      # FIXME - add support for :bindingfileset and :classpathset
      # Note: either :binding or :bindingfileset should be set, and either
      # :classpath or :classpathset should be set, and options passed to
      # ant.bind should be adjusted accordingly. At present, only :binding
      # and :classpath are supported (which should be fine for most!)
      jibx_options = { :output => compile.target,
        :classpath => compile.classpath,
        :binding => path_to(:source, :main, :resources, 'META-INF/binding.xml'),
        :target => compile.target,
        :load => false,
        :verbose => false
      }

      JiBX.bind jibx_options.merge(options || {})
    end

  end

  class Project
    include JiBX 
  end
end

