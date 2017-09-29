# coding: utf-8

require 'transpec/syntax/rspec_configure/config_modification'

module Transpec
  class Syntax
    class RSpecConfigure
      # This cannot be a Syntax class since this can be instanciated and used even when there's no
      # corresponding node in the existing code.
      class Framework
        include Rewritable, ConfigModification

        attr_reader :rspec_configure

        def initialize(rspec_configure)
          @rspec_configure = rspec_configure
        end

        def source_rewriter
          rspec_configure.source_rewriter
        end

        def add_record(*args)
          rspec_configure.add_record(*args)
        end

        def block_node
          return @block_node if instance_variable_defined?(:@block_node)

          @block_node = rspec_configure.block_node.each_descendant(:block).find do |block_node|
            send_node = block_node.children.first
            receiver_node, method_name, = *send_node
            next unless receiver_node == s(:lvar, rspec_configure.block_arg_name)
            method_name == block_method_name
            # TODO: Check expectation framework.
          end
        end

        private

        def block_method_name
          fail NotImplementedError
        end

        def generate_config_lines(config_name, value = nil, comment = nil)
          lines = super

          unless block_node
            lines.unshift(framework_begin_code)
            lines << framework_end_code
          end

          lines
        end

        def config_variable_name
          super || new_config_variable_name
        end

        def new_config_variable_name
          if rspec_configure.block_arg_name.to_s == framework_type_name
            'config'
          else
            framework_type_name
          end
        end

        def framework_type_name
          @framework_type_name ||= self.class.name.split('::').last.downcase
        end

        def body_indentation
          if block_node
            super
          else
            rspec_configure_body_indentation + (' ' * 2)
          end
        end

        def block_node_to_insert_code
          super || rspec_configure.block_node
        end

        def framework_begin_code
          code = format(
            '%s.%s :rspec do |%s|',
            rspec_configure.block_arg_name, block_method_name, config_variable_name
          )
          rspec_configure_body_indentation + code
        end

        def framework_end_code
          rspec_configure_body_indentation + 'end'
        end

        def rspec_configure_body_indentation
          indentation_of_line(rspec_configure.node) + (' ' * 2)
        end

        def config_record_syntax(config_name, value = nil)
          inner_block_arg = framework_type_name[0]
          syntax = "RSpec.configure { |c| c.#{block_method_name} :rspec "
          syntax << "{ |#{inner_block_arg}| #{inner_block_arg}.#{config_name}"
          syntax << " = #{value}" unless value.nil?
          syntax << ' } }'
        end

        module SyntaxConfig
          def syntaxes
            return [] unless syntaxes_node

            case syntaxes_node.type
            when :sym
              [syntaxes_node.children.first]
            when :array
              syntaxes_node.children.map do |child_node|
                child_node.children.first
              end
            else
              fail UnknownSyntaxError, "Unknown syntax specification: #{syntaxes_node}"
            end
          end

          def syntaxes=(syntaxes)
            unless [Array, Symbol].include?(syntaxes.class)
              fail ArgumentError, 'Syntaxes must be either an array or a symbol.'
            end

            set_config_value!(:syntax, syntaxes.inspect)
          end

          private

          def syntaxes_node
            return @syntaxes_node if instance_variable_defined?(:@syntaxes_node)

            syntax_setter_node = find_config_node(:syntax=)

            @syntaxes_node = if syntax_setter_node
                               syntax_setter_node.children[2]
                             else
                               nil
                             end
          end

          class UnknownSyntaxError < StandardError; end
        end

        include SyntaxConfig
      end
    end
  end
end
