# coding: utf-8

require 'transpec/util'
require 'ast'

module Transpec
  class Syntax
    class RSpecConfigure
      module ConfigModification
        include Util, ::AST::Sexp

        def block_node
          fail NotImplementedError
        end

        private

        def set_config_value!(config_name, value, comment = nil)
          config_node = find_config_node("#{config_name}=")

          if config_node
            current_value = config_node.children[2].loc.expression.source
            return if value.to_s == current_value
            modify_config_value!(config_node, value)
          else
            add_config!(config_name, value, comment)
          end
        end

        def find_config_node(config_method_name)
          return nil unless block_node

          config_method_name = config_method_name.to_sym

          block_node.each_descendant(:send).find do |send_node|
            receiver_node, method_name, = *send_node
            next unless receiver_node == s(:lvar, block_arg_name)
            method_name == config_method_name
          end
        end

        def modify_config_value!(config_node, value)
          arg_range = config_node.children[2].loc.expression
          replace(arg_range, value.to_s)

          config_name = config_node.loc.selector.source
          old_syntax = config_record_syntax(config_name, arg_range.source)
          new_syntax = config_record_syntax(config_name, value)
          add_record(old_syntax, new_syntax, type: :modification)
        end

        def replace_config!(old_config_name, new_config_name)
          config_node = find_config_node(old_config_name)
          return unless config_node
          new_selector = new_config_name.to_s.sub(/=$/, '')
          replace(config_node.loc.selector, new_selector)

          old_syntax = config_record_syntax(old_config_name)
          new_syntax = config_record_syntax(new_config_name)
          add_record(old_syntax, new_syntax)
        end

        def block_arg_name
          return nil unless block_node
          first_block_arg_name(block_node)
        end

        def config_record_syntax(config_name, value = nil)
          selector = config_name.to_s.sub(/=$/, '')
          syntax = "RSpec.configure { |c| c.#{selector}"

          value = 'something' if config_name.to_s.end_with?('=')
          syntax << " = #{value}" unless value.nil?

          syntax << ' }'
        end

        # TODO: Refactor this to remove messy overrides in Framework.
        module ConfigAddition
          def add_config!(config_name, value = nil, comment = nil)
            lines = generate_config_lines(config_name, value, comment)
            lines.unshift('') unless empty_block_body?
            lines.map! { |line| line + "\n" }

            insertion_position = beginning_of_line_range(block_node_to_insert_code.loc.end)
            insert_after(insertion_position, lines.join(''))

            block_node_to_insert_code.metadata[:added_config] = true

            add_record(nil, config_record_syntax(config_name, value))
          end

          def generate_config_lines(config_name, value = nil, comment = nil)
            lines = []

            if comment
              comment_lines = comment.each_line.map do |line|
                "#{body_indentation}# #{line.chomp}".rstrip
              end
              lines.concat(comment_lines)
            end

            config_line = body_indentation + "#{config_variable_name}.#{config_name}"
            config_line << " = #{value}" unless value.nil?
            lines << config_line

            lines
          end

          def config_variable_name
            block_arg_name
          end

          def body_indentation
            indentation_of_line(block_node) + (' ' * 2)
          end

          def block_node_to_insert_code
            block_node
          end

          def empty_block_body?
            block_node = block_node_to_insert_code
            (block_node.loc.end.line - block_node.loc.begin.line <= 1) &&
              !block_node.metadata[:added_config]
          end
        end

        include ConfigAddition
      end
    end
  end
end
