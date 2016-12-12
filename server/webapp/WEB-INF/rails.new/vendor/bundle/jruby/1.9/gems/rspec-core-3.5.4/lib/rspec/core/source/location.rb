module RSpec
  module Core
    class Source
      # @private
      # Represents a source location of node or token.
      Location = Struct.new(:line, :column) do
        def self.location?(array)
          array.is_a?(Array) && array.size == 2 && array.all? { |e| e.is_a?(Integer) }
        end
      end
    end
  end
end
