# coding: utf-8

require 'active_support/concern'
require 'transpec/syntax/mixin/send'
require 'transpec/syntax/mixin/matcher_owner'
require 'transpec/syntax/receive'
require 'transpec/util'

module Transpec
  class Syntax
    module Mixin
      module ExpectBase
        extend ActiveSupport::Concern
        include Send, MatcherOwner

        included do
          add_matcher Receive
        end

        def dynamic_analysis_target?
          super && receiver_node.nil? && matcher_node
        end

        def current_syntax_type
          :expect
        end

        def method_name_for_instance
          fail NotImplementedError
        end

        def positive?
          to_method_name = to_node.children[1]
          # `expect { do_something }.should raise_error` is possible in RSpec 2.
          [:to, :should].include?(to_method_name)
        end

        def subject_node
          arg_node || parent_node
        end

        def to_node
          return nil unless parent_node

          if parent_node.block_type? && parent_node.children.first.equal?(node)
            parent_node.parent
          elsif parent_node.send_type? && parent_node.children.first.equal?(node)
            parent_node
          else
            nil
          end
        end

        def matcher_node
          return nil unless to_node
          to_arg_node = to_node.children[2]
          return nil unless to_arg_node
          Util.each_forward_chained_node(to_arg_node, :include_origin)
            .select(&:send_type?).to_a.last
        end

        def block_node
          Util.block_node_taken_by_method(to_node)
        end

        def subject_range
          subject_node.loc.expression
        end
      end
    end
  end
end
