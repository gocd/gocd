# coding: utf-8

require 'transpec/syntax/have'

module Transpec
  class Syntax
    class Have
      class SourceBuilder
        include Util

        attr_reader :have, :size_source

        def initialize(have, size_source)
          @have = have
          @size_source = size_source
        end

        def replacement_subject_source(base_subject)
          source = case base_subject
                   when String    then base_subject
                   when AST::Node then base_subject_source(base_subject)
                   else fail "Invalid base subject #{base_subject}"
                   end

          if have.subject_is_owner_of_collection?
            if have.collection_accessor_is_private?
              source << ".send(#{have.collection_accessor.inspect}"
              if have.items_method_has_arguments?
                source << ", #{collection_accessor_args_body_source}"
              end
              source << ')'
            else
              source << ".#{have.collection_accessor}#{collection_accessor_args_parentheses_source}"
            end
          end

          source << ".#{have.query_method}"
        end

        def replacement_matcher_source(parenthesize_arg = true)
          case have.expectation.current_syntax_type
          when :should
            replacement_matcher_source_for_should
          when :expect
            replacement_matcher_source_for_expect(parenthesize_arg)
          end
        end

        private

        def base_subject_source(node)
          if node.send_type?
            chainable_source(node)
          else
            node.loc.expression.source
          end
        end

        def replacement_matcher_source_for_should
          case have.method_name
          when :have, :have_exactly then "== #{size_source}"
          when :have_at_least       then ">= #{size_source}"
          when :have_at_most        then "<= #{size_source}"
          end
        end

        def replacement_matcher_source_for_expect(parenthesize_arg)
          case have.method_name
          when :have, :have_exactly
            if parenthesize_arg
              "eq(#{size_source})"
            else
              "eq #{size_source}"
            end
          when :have_at_least
            "be >= #{size_source}"
          when :have_at_most
            "be <= #{size_source}"
          end
        end

        def collection_accessor_args_parentheses_source
          map = have.items_node.loc
          range = map.selector.end.join(map.expression.end)
          range.source
        end

        def collection_accessor_args_body_source
          arg_nodes = have.items_node.children[2..-1]
          range = arg_nodes.first.loc.expression.begin.join(arg_nodes.last.loc.expression.end)
          range.source
        end
      end
    end
  end
end
