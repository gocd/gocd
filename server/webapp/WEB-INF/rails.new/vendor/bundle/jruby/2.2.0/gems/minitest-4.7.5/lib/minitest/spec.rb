#!/usr/bin/ruby -w

require 'minitest/unit'

class Module # :nodoc:
  def infect_an_assertion meth, new_name, dont_flip = false # :nodoc:
    # warn "%-22p -> %p %p" % [meth, new_name, dont_flip]
    self.class_eval <<-EOM
      def #{new_name} *args
        case
        when Proc === self then
          MiniTest::Spec.current.#{meth}(*args, &self)
        when #{!!dont_flip} then
          MiniTest::Spec.current.#{meth}(self, *args)
        else
          MiniTest::Spec.current.#{meth}(args.first, self, *args[1..-1])
        end
      end
    EOM
  end

  ##
  # infect_with_assertions has been removed due to excessive clever.
  # Use infect_an_assertion directly instead.

  def infect_with_assertions(pos_prefix, neg_prefix,
                             skip_re,
                             dont_flip_re = /\c0/,
                             map = {})
    abort "infect_with_assertions is dead. Use infect_an_assertion directly"
  end
end

module Kernel # :nodoc:
  ##
  # Describe a series of expectations for a given target +desc+.
  #
  # TODO: find good tutorial url.
  #
  # Defines a test class subclassing from either MiniTest::Spec or
  # from the surrounding describe's class. The surrounding class may
  # subclass MiniTest::Spec manually in order to easily share code:
  #
  #     class MySpec < MiniTest::Spec
  #       # ... shared code ...
  #     end
  #
  #     class TestStuff < MySpec
  #       it "does stuff" do
  #         # shared code available here
  #       end
  #       describe "inner stuff" do
  #         it "still does stuff" do
  #           # ...and here
  #         end
  #       end
  #     end

  def describe desc, additional_desc = nil, &block # :doc:
    stack = MiniTest::Spec.describe_stack
    name  = [stack.last, desc, additional_desc].compact.join("::")
    sclas = stack.last || if Class === self && is_a?(MiniTest::Spec::DSL) then
                            self
                          else
                            MiniTest::Spec.spec_type desc
                          end

    cls = sclas.create name, desc

    stack.push cls
    cls.class_eval(&block)
    stack.pop
    cls
  end
  private :describe
end

##
# MiniTest::Spec -- The faster, better, less-magical spec framework!
#
# For a list of expectations, see MiniTest::Expectations.

class MiniTest::Spec < MiniTest::Unit::TestCase

  ##
  # Oh look! A MiniTest::Spec::DSL module! Eat your heart out DHH.

  module DSL
    ##
    # Contains pairs of matchers and Spec classes to be used to
    # calculate the superclass of a top-level describe. This allows for
    # automatically customizable spec types.
    #
    # See: register_spec_type and spec_type

    TYPES = [[//, MiniTest::Spec]]

    ##
    # Register a new type of spec that matches the spec's description.
    # This method can take either a Regexp and a spec class or a spec
    # class and a block that takes the description and returns true if
    # it matches.
    #
    # Eg:
    #
    #     register_spec_type(/Controller$/, MiniTest::Spec::Rails)
    #
    # or:
    #
    #     register_spec_type(MiniTest::Spec::RailsModel) do |desc|
    #       desc.superclass == ActiveRecord::Base
    #     end

    def register_spec_type(*args, &block)
      if block then
        matcher, klass = block, args.first
      else
        matcher, klass = *args
      end
      TYPES.unshift [matcher, klass]
    end

    ##
    # Figure out the spec class to use based on a spec's description. Eg:
    #
    #     spec_type("BlahController") # => MiniTest::Spec::Rails

    def spec_type desc
      TYPES.find { |matcher, klass|
        if matcher.respond_to? :call then
          matcher.call desc
        else
          matcher === desc.to_s
        end
      }.last
    end

    def describe_stack # :nodoc:
      Thread.current[:describe_stack] ||= []
    end

    ##
    # Returns the children of this spec.

    def children
      @children ||= []
    end

    def nuke_test_methods! # :nodoc:
      self.public_instance_methods.grep(/^test_/).each do |name|
        self.send :undef_method, name
      end
    end

    ##
    # Define a 'before' action. Inherits the way normal methods should.
    #
    # NOTE: +type+ is ignored and is only there to make porting easier.
    #
    # Equivalent to MiniTest::Unit::TestCase#setup.

    def before type = nil, &block
      define_method :setup do
        super()
        self.instance_eval(&block)
      end
    end

    ##
    # Define an 'after' action. Inherits the way normal methods should.
    #
    # NOTE: +type+ is ignored and is only there to make porting easier.
    #
    # Equivalent to MiniTest::Unit::TestCase#teardown.

    def after type = nil, &block
      define_method :teardown do
        self.instance_eval(&block)
        super()
      end
    end

    ##
    # Define an expectation with name +desc+. Name gets morphed to a
    # proper test method name. For some freakish reason, people who
    # write specs don't like class inheritance, so this goes way out of
    # its way to make sure that expectations aren't inherited.
    #
    # This is also aliased to #specify and doesn't require a +desc+ arg.
    #
    # Hint: If you _do_ want inheritence, use minitest/unit. You can mix
    # and match between assertions and expectations as much as you want.

    def it desc = "anonymous", &block
      block ||= proc { skip "(no tests defined)" }

      @specs ||= 0
      @specs += 1

      name = "test_%04d_%s" % [ @specs, desc ]

      define_method name, &block

      self.children.each do |mod|
        mod.send :undef_method, name if mod.public_method_defined? name
      end

      name
    end

    ##
    # Essentially, define an accessor for +name+ with +block+.
    #
    # Why use let instead of def? I honestly don't know.

    def let name, &block
      define_method name do
        @_memoized ||= {}
        @_memoized.fetch(name) { |k| @_memoized[k] = instance_eval(&block) }
      end
    end

    ##
    # Another lazy man's accessor generator. Made even more lazy by
    # setting the name for you to +subject+.

    def subject &block
      let :subject, &block
    end

    def create name, desc # :nodoc:
      cls = Class.new(self) do
        @name = name
        @desc = desc

        nuke_test_methods!
      end

      children << cls

      cls
    end

    def name # :nodoc:
      defined?(@name) ? @name : super
    end

    def to_s # :nodoc:
      name # Can't alias due to 1.8.7, not sure why
    end

    # :stopdoc:
    attr_reader :desc
    alias :specify :it
    # :startdoc:
  end

  extend DSL

  TYPES = DSL::TYPES # :nodoc:
end

##
# It's where you hide your "assertions".

module MiniTest::Expectations
  ##
  # See MiniTest::Assertions#assert_empty.
  #
  #    collection.must_be_empty
  #
  # :method: must_be_empty

  infect_an_assertion :assert_empty, :must_be_empty, :unary

  ##
  # See MiniTest::Assertions#assert_equal
  #
  #    a.must_equal b
  #
  # :method: must_equal

  infect_an_assertion :assert_equal, :must_equal

  ##
  # See MiniTest::Assertions#assert_in_delta
  #
  #    n.must_be_close_to m [, delta]
  #
  # :method: must_be_close_to

  infect_an_assertion :assert_in_delta, :must_be_close_to

  alias :must_be_within_delta :must_be_close_to # :nodoc:

  ##
  # See MiniTest::Assertions#assert_in_epsilon
  #
  #    n.must_be_within_epsilon m [, epsilon]
  #
  # :method: must_be_within_epsilon

  infect_an_assertion :assert_in_epsilon, :must_be_within_epsilon

  ##
  # See MiniTest::Assertions#assert_includes
  #
  #    collection.must_include obj
  #
  # :method: must_include

  infect_an_assertion :assert_includes, :must_include, :reverse

  ##
  # See MiniTest::Assertions#assert_instance_of
  #
  #    obj.must_be_instance_of klass
  #
  # :method: must_be_instance_of

  infect_an_assertion :assert_instance_of, :must_be_instance_of

  ##
  # See MiniTest::Assertions#assert_kind_of
  #
  #    obj.must_be_kind_of mod
  #
  # :method: must_be_kind_of

  infect_an_assertion :assert_kind_of, :must_be_kind_of

  ##
  # See MiniTest::Assertions#assert_match
  #
  #    a.must_match b
  #
  # :method: must_match

  infect_an_assertion :assert_match, :must_match

  ##
  # See MiniTest::Assertions#assert_nil
  #
  #    obj.must_be_nil
  #
  # :method: must_be_nil

  infect_an_assertion :assert_nil, :must_be_nil, :unary

  ##
  # See MiniTest::Assertions#assert_operator
  #
  #    n.must_be :<=, 42
  #
  # This can also do predicates:
  #
  #    str.must_be :empty?
  #
  # :method: must_be

  infect_an_assertion :assert_operator, :must_be, :reverse

  ##
  # See MiniTest::Assertions#assert_output
  #
  #    proc { ... }.must_output out_or_nil [, err]
  #
  # :method: must_output

  infect_an_assertion :assert_output, :must_output

  ##
  # See MiniTest::Assertions#assert_raises
  #
  #    proc { ... }.must_raise exception
  #
  # :method: must_raise

  infect_an_assertion :assert_raises, :must_raise

  ##
  # See MiniTest::Assertions#assert_respond_to
  #
  #    obj.must_respond_to msg
  #
  # :method: must_respond_to

  infect_an_assertion :assert_respond_to, :must_respond_to, :reverse

  ##
  # See MiniTest::Assertions#assert_same
  #
  #    a.must_be_same_as b
  #
  # :method: must_be_same_as

  infect_an_assertion :assert_same, :must_be_same_as

  ##
  # See MiniTest::Assertions#assert_send
  # TODO: remove me
  #
  #    a.must_send
  #
  # :method: must_send

  infect_an_assertion :assert_send, :must_send

  ##
  # See MiniTest::Assertions#assert_silent
  #
  #    proc { ... }.must_be_silent
  #
  # :method: must_be_silent

  infect_an_assertion :assert_silent, :must_be_silent

  ##
  # See MiniTest::Assertions#assert_throws
  #
  #    proc { ... }.must_throw sym
  #
  # :method: must_throw

  infect_an_assertion :assert_throws, :must_throw

  ##
  # See MiniTest::Assertions#refute_empty
  #
  #    collection.wont_be_empty
  #
  # :method: wont_be_empty

  infect_an_assertion :refute_empty, :wont_be_empty, :unary

  ##
  # See MiniTest::Assertions#refute_equal
  #
  #    a.wont_equal b
  #
  # :method: wont_equal

  infect_an_assertion :refute_equal, :wont_equal

  ##
  # See MiniTest::Assertions#refute_in_delta
  #
  #    n.wont_be_close_to m [, delta]
  #
  # :method: wont_be_close_to

  infect_an_assertion :refute_in_delta, :wont_be_close_to

  alias :wont_be_within_delta :wont_be_close_to # :nodoc:

  ##
  # See MiniTest::Assertions#refute_in_epsilon
  #
  #    n.wont_be_within_epsilon m [, epsilon]
  #
  # :method: wont_be_within_epsilon

  infect_an_assertion :refute_in_epsilon, :wont_be_within_epsilon

  ##
  # See MiniTest::Assertions#refute_includes
  #
  #    collection.wont_include obj
  #
  # :method: wont_include

  infect_an_assertion :refute_includes, :wont_include, :reverse

  ##
  # See MiniTest::Assertions#refute_instance_of
  #
  #    obj.wont_be_instance_of klass
  #
  # :method: wont_be_instance_of

  infect_an_assertion :refute_instance_of, :wont_be_instance_of

  ##
  # See MiniTest::Assertions#refute_kind_of
  #
  #    obj.wont_be_kind_of mod
  #
  # :method: wont_be_kind_of

  infect_an_assertion :refute_kind_of, :wont_be_kind_of

  ##
  # See MiniTest::Assertions#refute_match
  #
  #    a.wont_match b
  #
  # :method: wont_match

  infect_an_assertion :refute_match, :wont_match

  ##
  # See MiniTest::Assertions#refute_nil
  #
  #    obj.wont_be_nil
  #
  # :method: wont_be_nil

  infect_an_assertion :refute_nil, :wont_be_nil, :unary

  ##
  # See MiniTest::Assertions#refute_operator
  #
  #    n.wont_be :<=, 42
  #
  # This can also do predicates:
  #
  #    str.wont_be :empty?
  #
  # :method: wont_be

  infect_an_assertion :refute_operator, :wont_be, :reverse

  ##
  # See MiniTest::Assertions#refute_respond_to
  #
  #    obj.wont_respond_to msg
  #
  # :method: wont_respond_to

  infect_an_assertion :refute_respond_to, :wont_respond_to, :reverse

  ##
  # See MiniTest::Assertions#refute_same
  #
  #    a.wont_be_same_as b
  #
  # :method: wont_be_same_as

  infect_an_assertion :refute_same, :wont_be_same_as
end

class Object # :nodoc:
  include MiniTest::Expectations unless ENV["MT_NO_EXPECTATIONS"]
end
