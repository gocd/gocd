# coding: utf-8

require 'active_support/concern'
require 'transpec/syntax/mixin/block_owner'

module Transpec
  class Syntax
    module Mixin
      module Examplish
        extend ActiveSupport::Concern
        include Mixin::BlockOwner

        def block_node
          parent_node.block_type? ? parent_node : nil
        end

        def insert_description!(description)
          fail 'This example already has description' if description?
          insert_before(block_node.loc.begin, "'#{description}' ")
        end
      end
    end
  end
end
