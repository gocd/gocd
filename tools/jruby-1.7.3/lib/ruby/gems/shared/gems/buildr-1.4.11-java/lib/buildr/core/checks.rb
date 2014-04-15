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
  # Methods added to Project to allow checking the build.
  module Checks

    module Matchers #:nodoc:

      class << self

        # Define matchers that operate by calling a method on the tested object.
        # For example:
        #   foo.should contain(bar)
        # calls:
        #   foo.contain(bar)
        def match_using(*names)
          names.each do |name|
            matcher = Class.new do
              # Initialize with expected arguments (i.e. contain(bar) initializes with bar).
              define_method(:initialize) { |*args| @expects = args }
              # Matches against actual value (i.e. foo.should exist called with foo).
              define_method(:matches?) do |actual|
                @actual = actual
                return actual.send("#{name}?", *@expects) if actual.respond_to?("#{name}?")
                return actual.send(name, *@expects) if actual.respond_to?(name)
                raise "You can't check #{actual}, it doesn't respond to #{name}."
              end
              # Some matchers have arguments, others don't, treat appropriately.
              define_method :failure_message do
                args = " " + @expects.map{ |arg| "'#{arg}'" }.join(", ") unless @expects.empty?
                "Expected #{@actual} to #{name}#{args}"
              end
              define_method :negative_failure_message do
                args = " " + @expects.map{ |arg| "'#{arg}'" }.join(", ") unless @expects.empty?
                "Expected #{@actual} to not #{name}#{args}"
              end
            end
            # Define method to create matcher.
            define_method(name) { |*args| matcher.new(*args) }
          end
        end

      end

      # Define delegate matchers for exist and contain methods.
      match_using :exist, :contain

    end


    # An expectation has subject, description and block. The expectation is validated by running the block,
    # and can access the subject from the method #it. The description is used for reporting.
    #
    # The expectation is run by calling #run_against. You can share expectations by running them against
    # different projects (or any other context for that matter).
    #
    # If the subject is missing, it is set to the argument of #run_against, typically the project itself.
    # If the description is missing, it is set from the project. If the block is missing, the default behavior
    # prints "Pending" followed by the description. You can use this to write place holders and fill them later.
    class Expectation

      attr_reader :description, :subject, :block

      # :call-seq:
      #   initialize(subject, description?) { .... }
      #   initialize(description?) { .... }
      #
      # First argument is subject (returned from it method), second argument is description. If you omit the
      # description, it will be set from the subject. If you omit the subject, it will be set from the object
      # passed to run_against.
      def initialize(*args, &block)
        @description = args.pop if String === args.last
        @subject = args.shift
        raise ArgumentError, "Expecting subject followed by description, and either one is optional. Not quite sure what to do with this list of arguments." unless args.empty?
        @block = block || lambda { |klass| info "Pending: #{description}" }
      end

      # :call-seq:
      #   run_against(context)
      #
      # Runs this expectation against the context object. The context object is different from the subject,
      # but used as the subject if no subject specified (i.e. returned from the it method).
      #
      # This method creates a new context object modeled after the context argument, but a separate object
      # used strictly for running this expectation, and used only once. The context object will pass methods
      # to the context argument, so you can call any method, e.g. package(:jar).
      #
      # It also adds all matchers defined in Buildr and RSpec, and two additional methods:
      # * it() -- Returns the subject.
      # * description() -- Returns the description.
      def run_against(context)
        subject = @subject || context
        description = @description ? "#{subject} #{@description}" : subject.to_s
        # Define anonymous class and load it with:
        # - All instance methods defined in context, so we can pass method calls to the context.
        # - it() method to return subject, description() method to return description.
        # - All matchers defined by Buildr and RSpec.
        klass = Class.new
        klass.instance_eval do
          context.class.instance_methods.each do |method|
            define_method(method) { |*args| context.send(method, *args) } unless instance_methods.include?(method)
          end
          define_method(:it) { subject }
          define_method(:description) { description }
          include ::RSpec::Matchers
          include Matchers
        end

        # Run the expectation. We only print the expectation name when tracing (to know they all ran),
        # or when we get a failure.
        begin
          trace description
          klass.new.instance_eval &@block
        rescue Exception=>error
          raise error.exception("#{description}\n#{error}").tap { |wrapped| wrapped.set_backtrace(error.backtrace) }
        end
      end

    end


    include Extension

    before_define(:check => :package) do |project|
      # The check task can do any sort of interesting things, but the most important is running expectations.
      project.task("check") do |task|
        project.expectations.inject(true) do |passed, expect|
          begin
            expect.run_against project
            passed
          rescue Exception=>ex
            if verbose
              error ex
              error ex.backtrace.select { |line| line =~ /#{Buildr.application.buildfile}/ }.join("\n")
            end
            false
          end
        end or fail "Checks failed for project #{project.name} (see errors above)."
      end
      project.task("package").enhance do |task|
        # Run all actions before checks.
        task.enhance { project.task("check").invoke }
      end
    end


    # :call-seq:
    #    check(description) { ... }
    #    check(subject, description) { ... }
    #
    # Adds an expectation. The expectation is run against the project by the check task, executed after packaging.
    # You can access any package created by the project.
    #
    # An expectation is written using a subject, description and block to validate the expectation. For example:
    #
    # For example:
    #   check package(:jar), "should exist" do
    #     it.should exist
    #   end
    #   check package(:jar), "should contain a manifest" do
    #     it.should contain("META-INF/MANIFEST.MF")
    #   end
    #   check package(:jar).path("com/acme"), "should contain classes" do
    #     it.should_not be_empty
    #   end
    #   check package(:jar).entry("META-INF/MANIFEST"), "should be a recent license" do
    #     it.should contain(/Copyright (C) 2007/)
    #   end
    #
    # If you omit the subject, the project is used as the subject. If you omit the description, the subject is
    # used as description.
    #
    # During development you can write placeholder expectations by omitting the block. This will simply report
    # the expectation as pending.
    def check(*args, &block)
      expectations << Checks::Expectation.new(*args, &block)
    end

    # :call-seq:
    #   expectations() => Expectation*
    #
    # Returns a list of expectations (see #check).
    def expectations()
      @expectations ||= []
    end

  end

end


module Rake #:nodoc:
  class FileTask

    # :call-seq:
    #   exist?() => boolean
    #
    # Returns true if this file exists.
    def exist?()
      File.exist?(name)
    end

    # :call-seq:
    #   empty?() => boolean
    #
    # Returns true if file/directory is empty.
    def empty?()
      File.directory?(name) ? Dir.glob("#{name}/*").empty? : File.read(name).empty?
    end

    # :call-seq:
    #   contain?(pattern*) => boolean
    #   contain?(file*) => boolean
    #
    # For a file, returns true if the file content matches against all the arguments. An argument may be
    # a string or regular expression.
    #
    # For a directory, return true if the directory contains the specified files. You can use relative
    # file names and glob patterns (using *, **, etc).
    def contain?(*patterns)
      if File.directory?(name)
        patterns.map { |pattern| "#{name}/#{pattern}" }.all? { |pattern| !Dir[pattern].empty? }
      else
        contents = File.read(name)
        patterns.map { |pattern| Regexp === pattern ? pattern : Regexp.new(Regexp.escape(pattern.to_s)) }.
          all? { |pattern| contents =~ pattern }
      end
    end

  end
end


class Buildr::Project
  include Buildr::Checks
end
