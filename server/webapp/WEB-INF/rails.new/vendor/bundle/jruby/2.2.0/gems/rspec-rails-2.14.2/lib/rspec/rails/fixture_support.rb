module RSpec
  module Rails
    if defined?(ActiveRecord::TestFixtures)
      module FixtureSupport
        extend ActiveSupport::Concern
        include RSpec::Rails::SetupAndTeardownAdapter
        include RSpec::Rails::MinitestLifecycleAdapter if ::ActiveRecord::VERSION::STRING > '4'
        include RSpec::Rails::MinitestAssertionAdapter
        include ActiveRecord::TestFixtures

        included do
          # TODO (DC 2011-06-25) this is necessary because fixture_file_upload
          # accesses fixture_path directly on ActiveSupport::TestCase. This is
          # fixed in rails by https://github.com/rails/rails/pull/1861, which
          # should be part of the 3.1 release, at which point we can include
          # these lines for rails < 3.1.
          ActiveSupport::TestCase.class_eval do
            include ActiveRecord::TestFixtures
            self.fixture_path = RSpec.configuration.fixture_path
          end
          # /TODO

          self.fixture_path = RSpec.configuration.fixture_path
          self.use_transactional_fixtures = RSpec.configuration.use_transactional_fixtures
          self.use_instantiated_fixtures  = RSpec.configuration.use_instantiated_fixtures
          fixtures RSpec.configuration.global_fixtures if RSpec.configuration.global_fixtures
        end
      end

      RSpec.configure do |c|
        c.include RSpec::Rails::FixtureSupport
        c.add_setting :use_transactional_fixtures, :alias_with => :use_transactional_examples
        c.add_setting :use_instantiated_fixtures
        c.add_setting :global_fixtures
        c.add_setting :fixture_path
      end
    end
  end
end
