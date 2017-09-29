# coding: utf-8

require 'transpec/syntax'
require 'transpec/syntax/mixin/context_sensitive'
require 'transpec/syntax/mixin/metadata'
require 'transpec/syntax/mixin/monkey_patch'
require 'transpec/syntax/mixin/rspec_rails'
require 'transpec/rspec_dsl'

module Transpec
  class Syntax
    class ExampleGroup < Syntax
      include Mixin::ContextSensitive, Mixin::Metadata, Mixin::MonkeyPatch, Mixin::RSpecRails,
              RSpecDSL, Util

      DIRECTORY_TO_TYPE_MAP = {
        'controllers' => :controller,
        'helpers'     => :helper,
        'mailers'     => :mailer,
        'models'      => :model,
        'requests'    => :request,
        'integration' => :request,
        'api'         => :request,
        'routing'     => :routing,
        'views'       => :view,
        'features'    => :feature
      }

      def dynamic_analysis_target?
        return false unless super
        return false if receiver_node && const_name(receiver_node) != 'RSpec'
        EXAMPLE_GROUP_METHODS.include?(method_name)
      end

      def should_be_in_example_group_context?
        false
      end

      def convert_to_non_monkey_patch!
        return if receiver_node
        insert_before(expression_range, 'RSpec.')
        add_record(NonMonkeyPatchRecordBuilder.build(self))
      end

      def add_explicit_type_metadata!
        return unless rspec_rails?
        return unless method_name == :describe
        return if explicit_type_metadata?

        type = implicit_type_metadata
        return unless type

        code = ', '
        code << if metadata_hash_style == :arrow
                  ":type => #{type.inspect}"
                else
                  "type: #{type.inspect}"
                end

        insert_after(metadata_insertion_point, code)

        add_record(ExplicitTypeMetadataRecordBuilder.build(self))
      end

      def implicit_type_metadata
        dirs = file_path.split('/')
        return nil unless dirs.first == 'spec'
        DIRECTORY_TO_TYPE_MAP[dirs[1]]
      end

      private

      def explicit_type_metadata?
        metadata_key_nodes.any? do |node|
          next false unless node.sym_type?
          key = node.children.first
          key == :type
        end
      end

      def file_path
        expression_range.source_buffer.name
      end

      def metadata_hash_style
        symbol_key_nodes = metadata_key_nodes.select(&:sym_type?)

        has_colon_separator_pair = symbol_key_nodes.any? do |sym_node|
          !sym_node.loc.expression.source.start_with?(':')
        end

        if has_colon_separator_pair
          :colon
        else
          :arrow
        end
      end

      def metadata_insertion_point
        hash_metadata_node_index = arg_nodes.find_index(&:hash_type?)

        last_non_hash_arg_node = if hash_metadata_node_index
                                   arg_nodes[hash_metadata_node_index - 1]
                                 else
                                   arg_nodes.last
                                 end

        last_non_hash_arg_node.loc.expression.end
      end

      class NonMonkeyPatchRecordBuilder < RecordBuilder
        param_names :example_group

        def old_syntax
          base_syntax
        end

        def new_syntax
          "RSpec.#{base_syntax}"
        end

        def base_syntax
          "#{example_group.method_name} 'something' { }"
        end
      end

      class ExplicitTypeMetadataRecordBuilder < RecordBuilder
        param_names :example_group

        def old_syntax
          "describe 'some #{spec_type}' { }"
        end

        def new_syntax
          "describe 'some #{spec_type}', :type => #{spec_type.inspect} { }"
        end

        def spec_type
          example_group.implicit_type_metadata
        end
      end
    end
  end
end
