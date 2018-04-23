# frozen_string_literal: true
module Arel
  module Nodes
    class BindParam < Node
      def ==(other)
        other.is_a?(BindParam)
      end
    end
  end
end
