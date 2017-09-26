require 'rails/generators'
require 'rails/generators/testing/behaviour'
require 'rails/generators/testing/setup_and_teardown'
require 'rails/generators/testing/assertions'
require 'fileutils'

module Rails
  module Generators
    # Disable color in output. Easier to debug.
    no_color!

    # This class provides a TestCase for testing generators. To setup, you need
    # just to configure the destination and set which generator is being tested:
    #
    #   class AppGeneratorTest < Rails::Generators::TestCase
    #     tests AppGenerator
    #     destination File.expand_path("../tmp", File.dirname(__FILE__))
    #   end
    #
    # If you want to ensure your destination root is clean before running each test,
    # you can set a setup callback:
    #
    #   class AppGeneratorTest < Rails::Generators::TestCase
    #     tests AppGenerator
    #     destination File.expand_path("../tmp", File.dirname(__FILE__))
    #     setup :prepare_destination
    #   end
    class TestCase < ActiveSupport::TestCase
      include Rails::Generators::Testing::Behaviour
      include Rails::Generators::Testing::SetupAndTeardown
      include Rails::Generators::Testing::Assertions
      include FileUtils

    end
  end
end
