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


module Buildr::Groovy

  # EasyB is a Groovy based BDD framework.
  # To use in your project:
  #
  #   test.using :easyb
  #
  # This framework will search in your project for:
  #   src/spec/groovy/**/*Story.groovy
  #   src/spec/groovy/**/*Specification.groovy
  #
  # Support the following options:
  # * :format -- Report format :txt or :xml, default is :txt
  # * :properties -- Hash of properties passed to the test suite.
  # * :java_args -- Arguments passed to the JVM.
  class EasyB < TestFramework::JavaBDD
    @lang = :groovy
    @bdd_dir = :spec

    VERSION = "0.9"
    TESTS_PATTERN = [ /(Story|Specification).groovy$/ ]
    OPTIONS = [:format, :properties, :java_args]

    class << self
      def version
        Buildr.settings.build['jbehave'] || VERSION
      end

      def dependencies
        @dependencies ||= ["org.easyb:easyb:jar:#{version}",
          'org.codehaus.groovy:groovy:jar:1.5.3','asm:asm:jar:2.2.3',
          'commons-cli:commons-cli:jar:1.0','antlr:antlr:jar:2.7.7']
      end

      def applies_to?(project) #:nodoc:
        %w{
          **/*Specification.groovy **/*Story.groovy
        }.any? { |glob| !Dir[project.path_to(:source, bdd_dir, lang, glob)].empty? }
      end

    private
      def const_missing(const)
        return super unless const == :REQUIRES # TODO: remove in 1.5
        Buildr.application.deprecated "Please use JBehave.dependencies/.version instead of JBehave::REQUIRES/VERSION"
        dependencies
      end
    end

    def tests(dependencies) #:nodoc:
      Dir[task.project.path_to(:source, bdd_dir, lang, "**/*.groovy")].
        select { |name| TESTS_PATTERN.any? { |pat| pat === name } }
    end

    def run(tests, dependencies) #:nodoc:
      options = { :format => :txt }.merge(self.options).only(*OPTIONS)

      if :txt == options[:format]
        easyb_format, ext = 'txtstory', '.txt'
      elsif :xml == options[:format]
        easyb_format, ext = 'xmlbehavior', '.xml'
      else
        raise "Invalid format #{options[:format]} expected one of :txt :xml"
      end

      cmd_args = [ 'org.disco.easyb.BehaviorRunner' ]
      cmd_options = { :properties => options[:properties],
                      :java_args => options[:java_args],
                      :classpath => dependencies }

      tests.inject([]) do |passed, test|
        name = test.sub(/.*?groovy[\/\\]/, '').pathmap('%X')
        report = File.join(task.report_to.to_s, name + ext)
        mkpath report.pathmap('%d')
        begin
          Java::Commands.java cmd_args,
             "-#{easyb_format}", report,
             test, cmd_options.merge(:name => name)
        rescue => e
          passed
        else
          passed << test
        end
      end
    end

  end # EasyB

end

Buildr::TestFramework << Buildr::Groovy::EasyB
