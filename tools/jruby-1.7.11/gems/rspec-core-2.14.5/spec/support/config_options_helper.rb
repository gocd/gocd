module ConfigOptionsHelper
  extend RSpec::SharedContext

  around(:each) { |e| without_env_vars('SPEC_OPTS', &e) }

  def config_options_object(*args)
    coo = RSpec::Core::ConfigurationOptions.new(args)
    coo.parse_options
    coo
  end

  def parse_options(*args)
    config_options_object(*args).options
  end
end
