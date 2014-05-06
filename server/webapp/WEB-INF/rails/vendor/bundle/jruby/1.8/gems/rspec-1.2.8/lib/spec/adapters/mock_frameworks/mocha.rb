require 'rubygems' unless ENV['NO_RUBYGEMS']
require 'mocha/standalone'
require 'mocha/object'

module Spec
  module Adapters
    module MockFramework
      include Mocha::Standalone
      def setup_mocks_for_rspec
        mocha_setup
      end
      def verify_mocks_for_rspec
        mocha_verify
      end
      def teardown_mocks_for_rspec
        mocha_teardown
      end
    end
  end
end
