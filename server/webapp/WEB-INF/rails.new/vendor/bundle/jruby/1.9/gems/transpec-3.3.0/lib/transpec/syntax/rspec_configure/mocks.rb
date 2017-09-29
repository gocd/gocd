# coding: utf-8

require 'transpec/syntax/rspec_configure/framework'

module Transpec
  class Syntax
    class RSpecConfigure
      class Mocks < Framework
        def block_method_name
          :mock_with
        end

        def yield_receiver_to_any_instance_implementation_blocks=(value)
          # Based on the deprecation warning in RSpec 2.99:
          # https://github.com/rspec/rspec-mocks/blob/aab8dc9/lib/rspec/mocks/message_expectation.rb#L478-L491
          comment = <<-END.gsub(/^\s+\|/, '').chomp
            |In RSpec 3, `any_instance` implementation blocks will be yielded the receiving
            |instance as the first block argument to allow the implementation block to use
            |the state of the receiver.
            |In RSpec 2.99, to maintain compatibility with RSpec 3 you need to either set
            |this config option to `false` OR set this to `true` and update your
            |`any_instance` implementation blocks to account for the first block argument
            |being the receiving instance.
          END
          set_config_value!(:yield_receiver_to_any_instance_implementation_blocks, value, comment)
        end
      end
    end
  end
end
