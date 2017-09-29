# coding: utf-8

require 'transpec/syntax'
require 'transpec/syntax/mixin/examplish'
require 'transpec/syntax/mixin/send'
require 'transpec/util'

module Transpec
  class Syntax
    class Its < Syntax
      include Mixin::Examplish, Mixin::Send, Util

      define_dynamic_analysis do |rewriter|
        key = :project_requires_its?
        code = 'defined?(RSpec::Its)'
        rewriter.register_request(node, key, code, :context)
      end

      def dynamic_analysis_target?
        super && receiver_node.nil? && method_name == :its
      end

      def conversion_target?
        super && !runtime_data[node, :project_requires_its?]
      end

      def convert_to_describe_subject_it!
        insert_before(beginning_of_line_range(block_node), front_code)
        insert_before(expression_range, additional_indentation_for_it)
        replace(range_from_its_to_front_of_block, 'it ')
        insert_after(block_node.loc.expression, rear_code)

        increment_block_base_indentation!

        add_record
      end

      def insert_blank_line_above!
        insert_after(beginning_of_line_range(node), "\n")
      end

      def attribute_expression
        @attribute_expression ||= AttributeExpression.new(attribute_node)
      end

      def attributes
        attribute_expression.attributes
      end

      alias_method :attribute_node, :arg_node

      def block_node
        node.parent
      end

      def description?
        false
      end

      private

      def front_code
        code = ''

        if !previous_line_is_blank? && previous_and_current_line_are_same_indentation_level?
          code << "\n"
        end

        attributes.each_with_index do |attribute, index|
          indentation = block_base_indentation + '  ' * index
          code << indentation + "describe #{attribute.description} do\n"
          code << indentation + "  subject { super()#{attribute.selector} }\n"
        end

        code
      end

      def rear_code
        code = ''

        attributes.size.downto(1) do |level|
          indentation = block_base_indentation + '  ' * (level - 1)
          code << "\n"
          code << "#{indentation}end"
        end

        code
      end

      def additional_indentation_for_it
        '  ' * attributes.size
      end

      def previous_line_is_blank?
        return false unless previous_line_source
        previous_line_source.empty? || previous_line_source.match(/\A\s*\Z/)
      end

      def previous_and_current_line_are_same_indentation_level?
        indentation_of_line(previous_line_source) == block_base_indentation
      end

      def previous_line_source
        expression_range.source_buffer.source_line(expression_range.line - 1)
      rescue IndexError
        nil
      end

      # TODO: This is an ad-hoc solution for nested indentation manipulations.
      def block_base_indentation
        block_node.metadata[:indentation] ||= indentation_of_line(node)
      end

      def increment_block_base_indentation!
        block_node.metadata[:indentation] = block_base_indentation + '  '
      end

      def range_from_its_to_front_of_block
        expression_range.join(block_node.loc.begin.begin)
      end

      def add_record
        super(RecordBuilder.build(self))
      end

      class AttributeExpression
        attr_reader :node

        def initialize(node)
          @node = node
        end

        def brackets?
          node.array_type?
        end

        def literal?
          Util.literal?(node)
        end

        def attributes
          @attributes ||= if brackets?
                            brackets_attributes
                          else
                            non_brackets_attributes
                          end
        end

        private

        def brackets_attributes
          selector = node.loc.expression.source
          description = literal? ? quote(selector) : selector
          [Attribute.new(selector, description)]
        end

        def non_brackets_attributes
          if literal?
            expression = node.children.first.to_s
            chained_names = expression.split('.')
            chained_names.map do |name|
              Attribute.new(".#{name}", quote("##{name}"))
            end
          else
            source = node.loc.expression.source
            selector = ".send(#{source})"
            [Attribute.new(selector, source)]
          end
        end

        def quote(string)
          if string.include?("'")
            '"' + string + '"'
          elsif string.include?('"')
            string.inspect
          else
            "'" + string + "'"
          end
        end
      end

      Attribute = Struct.new(:selector, :description)

      class RecordBuilder < Transpec::RecordBuilder
        param_names :its

        def old_syntax
          if its.attribute_expression.brackets?
            'its([:key]) { }'
          else
            'its(:attr) { }'
          end
        end

        def new_syntax
          if its.attribute_expression.brackets?
            "describe '[:key]' do subject { super()[:key] }; it { } end"
          else
            "describe '#attr' do subject { super().attr }; it { } end"
          end
        end
      end
    end
  end
end
