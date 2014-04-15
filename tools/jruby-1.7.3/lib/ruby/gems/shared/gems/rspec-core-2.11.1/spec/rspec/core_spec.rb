require 'spec_helper'

describe RSpec do
  describe "::configuration" do
    it "returns the same object every time" do
      RSpec.configuration.should equal(RSpec.configuration)
    end
  end

  describe "::configure" do
    it "yields the current configuration" do
      RSpec.configure do |config|
        config.should equal(RSpec::configuration)
      end
    end
  end

  describe "::world" do
    it "returns the same object every time" do
      RSpec.world.should equal(RSpec.world)
    end
  end

  describe "::reset" do
    it "resets the configuration and world objects" do
      config_before_reset = RSpec.configuration
      world_before_reset  = RSpec.world

      RSpec.reset

      RSpec.configuration.should_not equal(config_before_reset)
      RSpec.world.should_not equal(world_before_reset)
    end
  end
end
