module Versionist
  class NewPresenterGenerator < Rails::Generators::NamedBase
    include InflectorFixes
    include RspecHelper

    desc "creates a new presenter for an existing API version"
    source_root File.expand_path('../templates', __FILE__)

    argument :module_name, :type => :string

    def new_presenter
      in_root do
        raise "API module namespace #{module_name} doesn't exist. Please run \'rails generate versionist:new_api_version\' generator first" if !File.exists?("app/presenters/#{module_name_for_path(module_name)}")
        template 'new_presenter.rb', File.join("app", "presenters", "#{module_name_for_path(module_name)}", "#{file_name}_presenter.rb")
      end
    end

    def new_presenter_test
      in_root do
        case Versionist.configuration.configured_test_framework
        when :test_unit
          if Versionist.older_than_rails_5?
            template 'new_presenter_test.rb', File.join("test", "presenters", "#{module_name_for_path(module_name)}", "#{file_name}_presenter_test.rb")
          else
            template 'new_presenter_test_rails_5.rb', File.join("test", "presenters", "#{module_name_for_path(module_name)}", "#{file_name}_presenter_test_rails_5.rb")
          end
        when :rspec
          @rspec_require_file = rspec_helper_filename
          template 'new_presenter_spec.rb', File.join("spec", "presenters", "#{module_name_for_path(module_name)}", "#{file_name}_presenter_spec.rb"), :assigns => { :rspec_require_file => @rspec_require_file }
        else
          say "Unsupported test_framework: #{Versionist.configuration.configured_test_framework}"
        end
      end
    end
  end
end
