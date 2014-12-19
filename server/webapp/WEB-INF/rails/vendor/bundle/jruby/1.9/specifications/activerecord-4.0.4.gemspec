# -*- encoding: utf-8 -*-
# stub: activerecord 4.0.4 ruby lib

Gem::Specification.new do |s|
  s.name = "activerecord"
  s.version = "4.0.4"

  s.required_rubygems_version = Gem::Requirement.new(">= 0") if s.respond_to? :required_rubygems_version=
  s.authors = ["David Heinemeier Hansson"]
  s.date = "2014-03-14"
  s.description = "Databases on Rails. Build a persistent domain model by mapping database tables to Ruby classes. Strong conventions for associations, validations, aggregations, migrations, and testing come baked-in."
  s.email = "david@loudthinking.com"
  s.extra_rdoc_files = ["README.rdoc"]
  s.files = ["README.rdoc"]
  s.homepage = "http://www.rubyonrails.org"
  s.licenses = ["MIT"]
  s.rdoc_options = ["--main", "README.rdoc"]
  s.require_paths = ["lib"]
  s.required_ruby_version = Gem::Requirement.new(">= 1.9.3")
  s.rubygems_version = "2.1.9"
  s.summary = "Object-relational mapper framework (part of Rails)."

  if s.respond_to? :specification_version then
    s.specification_version = 4

    if Gem::Version.new(Gem::VERSION) >= Gem::Version.new('1.2.0') then
      s.add_runtime_dependency(%q<activesupport>, ["= 4.0.4"])
      s.add_runtime_dependency(%q<activemodel>, ["= 4.0.4"])
      s.add_runtime_dependency(%q<arel>, ["~> 4.0.0"])
      s.add_runtime_dependency(%q<activerecord-deprecated_finders>, ["~> 1.0.2"])
    else
      s.add_dependency(%q<activesupport>, ["= 4.0.4"])
      s.add_dependency(%q<activemodel>, ["= 4.0.4"])
      s.add_dependency(%q<arel>, ["~> 4.0.0"])
      s.add_dependency(%q<activerecord-deprecated_finders>, ["~> 1.0.2"])
    end
  else
    s.add_dependency(%q<activesupport>, ["= 4.0.4"])
    s.add_dependency(%q<activemodel>, ["= 4.0.4"])
    s.add_dependency(%q<arel>, ["~> 4.0.0"])
    s.add_dependency(%q<activerecord-deprecated_finders>, ["~> 1.0.2"])
  end
end
