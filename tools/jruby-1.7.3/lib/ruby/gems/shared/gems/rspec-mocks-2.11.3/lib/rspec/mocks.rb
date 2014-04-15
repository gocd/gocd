require 'rspec/mocks/framework'
require 'rspec/mocks/version'
require 'rspec/mocks/example_methods'

module RSpec
  module Mocks
    class << self
      attr_accessor :space

      def setup(host)
        add_extensions unless extensions_added?
        (class << host; self; end).class_eval do
          include RSpec::Mocks::ExampleMethods
        end
        self.space ||= RSpec::Mocks::Space.new
      end

      def verify
        space.verify_all
      end

      def teardown
        space.reset_all
      end

    private

      def add_extensions
        Object.class_eval { include RSpec::Mocks::Methods }
        Class.class_eval  { include RSpec::Mocks::AnyInstance }
        $_rspec_mocks_extensions_added = true
      end

      def extensions_added?
        defined?($_rspec_mocks_extensions_added)
      end
    end
  end
end
