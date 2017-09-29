# coding: utf-8

require 'transpec/syntax'
require 'transpec/syntax/mixin/expect_base'
require 'transpec/syntax/have'
require 'transpec/syntax/raise_error'

module Transpec
  class Syntax
    class Expect < Syntax
      include Mixin::ExpectBase

      add_matcher Have
      add_matcher RaiseError

      def dynamic_analysis_target?
        super && [:expect, :expect_any_instance_of].include?(method_name)
      end

      def method_name_for_instance
        :expect
      end

      def any_instance?
        method_name == :expect_any_instance_of
      end
    end
  end
end
