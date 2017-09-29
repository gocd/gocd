# coding: utf-8

require 'active_support/concern'
require 'transpec/syntax/mixin/send'
require 'transpec/syntax/mixin/matcher_owner'
require 'transpec/syntax/have'
require 'transpec/syntax/operator'
require 'transpec/syntax/raise_error'
require 'transpec/util'

module Transpec
  class Syntax
    module Mixin
      module ShouldBase
        extend ActiveSupport::Concern
        include Send, MatcherOwner

        included do
          add_matcher Have
          add_matcher Operator
          add_matcher RaiseError
        end

        def current_syntax_type
          @current_syntax_type ||= :should
        end

        def positive?
          method_name == :should
        end

        def matcher_node
          if arg_node
            Util.each_forward_chained_node(arg_node, :include_origin)
              .select(&:send_type?).to_a.last
          else
            parent_node
          end
        end

        def should_range
          if arg_node
            selector_range
          else
            selector_range.join(expression_range.end)
          end
        end
      end
    end
  end
end
