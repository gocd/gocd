# coding: utf-8

require 'transpec/base_rewriter'
require 'transpec/dynamic_analyzer/constants'
require 'transpec/dynamic_analyzer/node_util'
require 'transpec/util'
require 'transpec/syntax'

Transpec::Syntax.require_all

module Transpec
  class DynamicAnalyzer
    class Rewriter < BaseRewriter
      include NodeUtil, Util

      EVAL_TARGET_TYPES = [:object, :context]

      def process(ast, source_rewriter)
        # TODO: Currently multitheading is not considered...
        clear_requests!
        collect_requests(ast)
        process_requests(source_rewriter)
      end

      def requests
        # AST::Node#eql? returns true if two nodes have the same structure,
        # even if they are not identical objects.
        @requests ||= {}.compare_by_identity
      end

      def clear_requests!
        @requests = nil
      end

      def register_request(node, key, instance_eval_string, eval_target_type = :object)
        unless EVAL_TARGET_TYPES.include?(eval_target_type)
          fail "Target type must be any of #{EVAL_TARGET_TYPES}"
        end

        requests[node] ||= {}
        requests[node][key] = [eval_target_type, instance_eval_string]
      end

      private

      def collect_requests(ast)
        return unless ast

        ast.each_node do |node|
          Syntax.standalone_syntaxes.each do |syntax_class|
            syntax = syntax_class.new(node)
            collect_requests_of_syntax(syntax)
          end
        end
      end

      def collect_requests_of_syntax(syntax)
        return unless syntax.dynamic_analysis_target?
        syntax.register_dynamic_analysis_request(self)
        syntax.dependent_syntaxes.each do |dependent_syntax|
          collect_requests_of_syntax(dependent_syntax)
        end
      end

      def process_requests(source_rewriter)
        requests.each do |node, analysis_codes|
          inject_analysis_code(node, analysis_codes, source_rewriter)
        end
      end

      def inject_analysis_code(node, analysis_codes, source_rewriter)
        front, rear = build_wrapper_codes(node, analysis_codes)

        source_range = if (block_node = block_node_taken_by_method(node))
                         block_node.loc.expression
                       else
                         node.loc.expression
                       end

        source_rewriter.insert_before_multi(source_range, front)
        source_rewriter.insert_after_multi(source_range, rear)
      end

      def build_wrapper_codes(node, analysis_codes)
        front = "#{ANALYSIS_MODULE}.#{ANALYSIS_METHOD}(("
        rear = format('), self, %s, %s)', node_id(node).inspect, hash_literal(analysis_codes))
        [front, rear]
      end

      # Hash#inspect generates invalid literal with following example:
      #
      # > eval({ :predicate? => 1 }.inspect)
      # SyntaxError: (eval):1: syntax error, unexpected =>
      # {:predicate?=>1}
      #               ^
      def hash_literal(hash)
        literal = '{ '

        hash.each_with_index do |(key, value), index|
          literal << ', ' unless index.zero?
          literal << "#{key.inspect} => #{value.inspect}"
        end

        literal << ' }'
      end
    end
  end
end
