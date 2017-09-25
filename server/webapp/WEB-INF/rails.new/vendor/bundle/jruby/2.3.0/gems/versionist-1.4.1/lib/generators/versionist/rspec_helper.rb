module Versionist
  module RspecHelper
    # Gets the name of the helper file to require in spec files
    # Accounting for rspec-rails 2 vs rspec-rails 3
    def rspec_helper_filename
      if File.exists? "spec/rails_helper.rb"
        "rails_helper"
      else
        "spec_helper"
      end
    end
  end
end
