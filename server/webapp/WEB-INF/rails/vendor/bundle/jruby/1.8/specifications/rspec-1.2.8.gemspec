# -*- encoding: utf-8 -*-

Gem::Specification.new do |s|
  s.name = "rspec"
  s.version = "1.2.8"

  s.required_rubygems_version = Gem::Requirement.new(">= 0") if s.respond_to? :required_rubygems_version=
  s.authors = ["RSpec Development Team"]
  s.date = "2009-07-16"
  s.description = "Behaviour Driven Development for Ruby."
  s.email = ["rspec-devel@rubyforge.org"]
  s.executables = ["autospec", "spec"]
  s.extra_rdoc_files = ["License.txt", "Manifest.txt", "TODO.txt", "examples/failing/README.txt"]
  s.files = ["bin/autospec", "bin/spec", "License.txt", "Manifest.txt", "TODO.txt", "examples/failing/README.txt"]
  s.homepage = "http://rspec.info"
  s.post_install_message = "**************************************************\n\n  Thank you for installing rspec-1.2.8\n\n  Please be sure to read History.rdoc and Upgrade.rdoc\n  for useful information about this release.\n\n**************************************************\n"
  s.rdoc_options = ["--main", "README.rdoc"]
  s.require_paths = ["lib"]
  s.rubyforge_project = "rspec"
  s.rubygems_version = "1.8.24"
  s.summary = "rspec 1.2.8"

  if s.respond_to? :specification_version then
    s.specification_version = 3

    if Gem::Version.new(Gem::VERSION) >= Gem::Version.new('1.2.0') then
      s.add_development_dependency(%q<cucumber>, [">= 0.2.2"])
      s.add_development_dependency(%q<hoe>, [">= 2.3.1"])
    else
      s.add_dependency(%q<cucumber>, [">= 0.2.2"])
      s.add_dependency(%q<hoe>, [">= 2.3.1"])
    end
  else
    s.add_dependency(%q<cucumber>, [">= 0.2.2"])
    s.add_dependency(%q<hoe>, [">= 2.3.1"])
  end
end
