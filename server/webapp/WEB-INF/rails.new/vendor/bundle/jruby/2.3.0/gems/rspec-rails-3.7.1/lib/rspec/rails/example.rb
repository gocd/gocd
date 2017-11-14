require 'rspec/rails/example/rails_example_group'
require 'rspec/rails/example/controller_example_group'
require 'rspec/rails/example/request_example_group'
require 'rspec/rails/example/helper_example_group'
require 'rspec/rails/example/view_example_group'
require 'rspec/rails/example/mailer_example_group'
require 'rspec/rails/example/routing_example_group'
require 'rspec/rails/example/model_example_group'
require 'rspec/rails/example/job_example_group'
require 'rspec/rails/example/feature_example_group'
if ActionPack::VERSION::STRING >= "5.1"
  begin
    require 'puma'
    require 'capybara'
    require 'rspec/rails/example/system_example_group'
  # rubocop:disable Lint/HandleExceptions
  rescue LoadError
    # rubocop:enable Lint/HandleExceptions
  end
end
