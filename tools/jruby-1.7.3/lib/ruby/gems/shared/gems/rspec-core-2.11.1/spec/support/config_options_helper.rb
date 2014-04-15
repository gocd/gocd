require 'fakefs/safe'

module ConfigOptionsHelper
  extend RSpec::SharedContext

  before do
    @orig_spec_opts = ENV["SPEC_OPTS"]
    ENV.delete("SPEC_OPTS")
  end

  after do
    ENV["SPEC_OPTS"] = @orig_spec_opts
  end

  def config_options_object(*args)
    coo = RSpec::Core::ConfigurationOptions.new(args)
    coo.parse_options
    coo
  end

  def parse_options(*args)
    config_options_object(*args).options
  end
end
