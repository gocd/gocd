module RSpec
  module Core
    # @private
    module Extensions
      # @private
      # Used to extend lists of examples and groups to support ordering
      # strategies like randomization.
      module Ordered
        # @private
        module ExampleGroups
          # @private
          def ordered
            RSpec.configuration.group_ordering_block.call(self)
          end
        end

        # @private
        module Examples
          # @private
          def ordered
            RSpec.configuration.example_ordering_block.call(self)
          end
        end
      end
    end
  end
end
