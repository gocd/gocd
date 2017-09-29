# coding: utf-8

require 'transpec/syntax'
require 'transpec/syntax/mixin/should_base'
require 'transpec/syntax/example'
require 'transpec/syntax/its'
require 'transpec/util'
require 'active_support/inflector/methods'
require 'active_support/inflector/inflections'
require 'active_support/inflections'

module Transpec
  class Syntax
    class OnelinerShould < Syntax
      include Mixin::ShouldBase, RSpecDSL, Util

      def dynamic_analysis_target?
        super && receiver_node.nil? && [:should, :should_not].include?(method_name)
      end

      def conversion_target?
        return false unless dynamic_analysis_target?
        return true unless runtime_data.run?(send_analysis_target_node)
        return false unless defined_in_rspec_source?
        # #should inside of #its is dynamically defined in MemoizedHelper,
        # so it cannot be differentiated from user-defined methods by the dynamic analysis in Send.
        # https://github.com/rspec/rspec-core/blob/v2.14.8/lib/rspec/core/memoized_helpers.rb#L439
        !example_method_defined_by_user? || in_its?
      end

      def expectize!(negative_form = 'not_to')
        replacement = 'is_expected.'
        replacement << (positive? ? 'to' : negative_form)
        replace(should_range, replacement)

        @current_syntax_type = :expect

        add_record(ExpectRecordBuilder.build(self, negative_form))
      end

      def convert_have_items_to_standard_should!
        return unless have_matcher.conversion_target?

        insert_example_description!

        subject_source = have_matcher.replacement_subject_source('subject')
        insert_before(expression_range, "#{subject_source}.")

        add_record(HaveRecordBuilder.build(self, have_matcher))
      end

      def convert_have_items_to_standard_expect!(negative_form = 'not_to')
        return unless have_matcher.conversion_target?

        insert_example_description!

        subject_source = have_matcher.replacement_subject_source('subject')
        expect_to_source = "expect(#{subject_source})."
        expect_to_source << (positive? ? 'to' : negative_form)
        replace(should_range, expect_to_source)

        @current_syntax_type = :expect

        add_record(HaveRecordBuilder.build(self, have_matcher, negative_form))
      end

      def example
        return @example if instance_variable_defined?(:@example)

        @example = nil

        node.each_ancestor(:block) do |block_node|
          send_node = block_node.children[0]

          found = Syntax.all_syntaxes.find do |syntax_class|
            next unless syntax_class.ancestors.include?(Mixin::Examplish)
            syntax = syntax_class.new(send_node, runtime_data, project, source_rewriter)
            next unless syntax.conversion_target?
            @example = syntax
          end

          break if found
        end

        @example
      end

      def build_description(size)
        description = positive? ? 'has ' : 'does not have '

        case have_matcher.method_name
        when :have_at_least then description << 'at least '
        when :have_at_most  then description << 'at most '
        end

        items = have_matcher.items_name

        if positive? && size == '0'
          size = 'no'
        elsif size == '1'
          items = ActiveSupport::Inflector.singularize(have_matcher.items_name)
        end

        description << "#{size} #{items}"
      end

      private

      def insert_example_description!
        unless have_matcher.conversion_target?
          fail 'This one-liner #should does not have #have matcher!'
        end

        return unless example

        unless example.description?
          example.insert_description!(build_description(have_matcher.size_source))
        end

        example.convert_singleline_block_to_multiline!

        example.insert_blank_line_above! if in_its?
      end

      def in_its?
        example.is_a?(Its)
      end

      class ExpectRecordBuilder < RecordBuilder
        param_names :should, :negative_form_of_to

        def old_syntax
          syntax = 'it { should'
          syntax << '_not' unless should.positive?
          syntax << ' ... }'
        end

        def new_syntax
          syntax = 'it { is_expected.'
          syntax << (should.positive? ? 'to' : negative_form_of_to)
          syntax << ' ... }'
        end
      end

      class HaveRecordBuilder < Have::RecordBuilder
        param_names :should, :have, :negative_form_of_to

        def old_syntax
          syntax = had_description? ? "it '...' do" : 'it {'
          syntax << " #{should.method_name} #{have.method_name}(n).#{old_items} "
          syntax << (had_description? ? 'end' : '}')
        end

        def new_syntax
          syntax = new_description
          syntax << ' '
          syntax << new_expectation
          syntax << ' '
          syntax << source_builder.replacement_matcher_source
          syntax << ' '
          syntax << (has_description? ? 'end' : '}')
        end

        def new_description
          if has_description?
            if had_description?
              "it '...' do"
            else
              "it '#{should.build_description('n')}' do"
            end
          else
            'it {'
          end
        end

        def new_expectation
          case should.current_syntax_type
          when :should
            "#{new_subject}.#{should.method_name}"
          when :expect
            "expect(#{new_subject})." + (should.positive? ? 'to' : negative_form_of_to)
          end
        end

        def new_subject
          build_new_subject('subject')
        end

        def had_description?
          return false unless should.example
          should.example.description?
        end

        def has_description? # rubocop:disable PredicateName
          !should.example.nil?
        end
      end
    end
  end
end
