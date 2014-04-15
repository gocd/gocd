# -*- encoding: utf-8 -*-

Gem::Specification.new do |s|
  s.name = %q{rspec-extra-formatters}
  s.version = "0.3"

  s.required_rubygems_version = Gem::Requirement.new(">= 0") if s.respond_to? :required_rubygems_version=
  s.authors = ["Diego Souza", "Flor\303\251al TOUMIKIAN"]
  s.date = %q{2011-05-26}
  s.description = %q{    rspec-extra-formatters Provides TAP and JUnit formatters for rspec
}
  s.email = %q{dsouza+rspec-extra-formatters@bitforest.org}
  s.extra_rdoc_files = ["LICENSE", "README.rst"]
  s.files = ["lib/tap_formatter.rb", "lib/rspec-extra-formatters/junit_formatter.rb", "lib/rspec-extra-formatters/tap_formatter.rb", "lib/j_unit_formatter.rb", "lib/rspec-extra-formatters.rb", "spec/rspec-extra-formatters/tap_formatter_spec.rb", "spec/rspec-extra-formatters/junit_formatter_spec.rb", "spec/spec_helper.rb", "LICENSE", "README.rst"]
  s.homepage = %q{http://dsouza.bitforest.org/2011/01/22/rspec-tap-and-junit-formatters/}
  s.require_paths = ["lib"]
  s.rubygems_version = %q{1.3.5}
  s.summary = %q{TAP and JUnit formatters for rspec}
  s.test_files = ["spec/rspec-extra-formatters/tap_formatter_spec.rb", "spec/rspec-extra-formatters/junit_formatter_spec.rb", "spec/spec_helper.rb"]

  if s.respond_to? :specification_version then
    current_version = Gem::Specification::CURRENT_SPECIFICATION_VERSION
    s.specification_version = 3

    if Gem::Version.new(Gem::RubyGemsVersion) >= Gem::Version.new('1.2.0') then
      s.add_development_dependency(%q<rspec>, [">= 0"])
    else
      s.add_dependency(%q<rspec>, [">= 0"])
    end
  else
    s.add_dependency(%q<rspec>, [">= 0"])
  end
end
