# -*- encoding: utf-8 -*-
require File.expand_path('../lib/active_record/deprecated_finders/version', __FILE__)

Gem::Specification.new do |gem|
  gem.authors       = ["Jon Leighton"]
  gem.email         = ["j@jonathanleighton.com"]
  gem.description   = %q{Deprecated finder APIs extracted from Active Record.}
  gem.summary       = %q{This gem contains deprecated finder APIs extracted from Active Record.}
  gem.homepage      = "https://github.com/rails/activerecord-deprecated_finders"

  gem.files         = [".gitignore",".travis.yml","Gemfile","LICENSE","README.md","Rakefile","activerecord-deprecated_finders.gemspec","activerecord-deprecated_finders.gemspec.erb","lib/active_record/deprecated_finders.rb","lib/active_record/deprecated_finders/association_builder.rb","lib/active_record/deprecated_finders/base.rb","lib/active_record/deprecated_finders/collection_proxy.rb","lib/active_record/deprecated_finders/dynamic_matchers.rb","lib/active_record/deprecated_finders/relation.rb","lib/active_record/deprecated_finders/version.rb","test/associations_test.rb","test/calculate_test.rb","test/default_scope_test.rb","test/dynamic_methods_test.rb","test/find_in_batches_test.rb","test/finder_options_test.rb","test/finder_test.rb","test/helper.rb","test/scope_test.rb","test/scoped_test.rb","test/update_all_test.rb","test/with_scope_test.rb"]
  gem.test_files    = ["test/associations_test.rb","test/calculate_test.rb","test/default_scope_test.rb","test/dynamic_methods_test.rb","test/find_in_batches_test.rb","test/finder_options_test.rb","test/finder_test.rb","test/helper.rb","test/scope_test.rb","test/scoped_test.rb","test/update_all_test.rb","test/with_scope_test.rb"]
  gem.executables   = []
  gem.name          = "activerecord-deprecated_finders"
  gem.require_paths = ["lib"]
  gem.version       = ActiveRecord::DeprecatedFinders::VERSION

  gem.add_development_dependency 'minitest',     '>= 3'
  gem.add_development_dependency 'activerecord', '>= 4.0.0.beta', '< 5'
  gem.add_development_dependency 'sqlite3',      '~> 1.3'
end
