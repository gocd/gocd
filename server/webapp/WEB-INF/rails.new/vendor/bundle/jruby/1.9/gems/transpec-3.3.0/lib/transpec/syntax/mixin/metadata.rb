# coding: utf-8

require 'active_support/concern'
require 'transpec/syntax/mixin/send'

module Transpec
  class Syntax
    module Mixin
      module Metadata
        extend ActiveSupport::Concern
        include Send

        def metadata_nodes
          return arg_nodes if arg_nodes.empty?

          # The first argument must be always description.
          non_description_arg_nodes = arg_nodes.drop(1)

          non_description_arg_nodes.drop_while do |node|
            # Possibly there still may be descriptions after the first arg.
            #   describe 'something', '#some_method', :foo, bar: true { }
            ![:hash, :sym].include?(node.type)
          end
        end

        def metadata_key_nodes
          metadata_nodes.each_with_object([]) do |node, key_nodes|
            if node.hash_type?
              key_nodes.concat(node.children.map { |pair_node| pair_node.children.first })
            else
              key_nodes << node
            end
          end
        end
      end
    end
  end
end
