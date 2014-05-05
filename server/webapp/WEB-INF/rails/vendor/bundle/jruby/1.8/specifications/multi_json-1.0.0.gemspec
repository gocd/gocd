# -*- encoding: utf-8 -*-

Gem::Specification.new do |s|
  s.name = "multi_json"
  s.version = "1.0.0"

  s.required_rubygems_version = Gem::Requirement.new(">= 1.3.6") if s.respond_to? :required_rubygems_version=
  s.authors = ["Michael Bleigh"]
  s.date = "2011-04-26"
  s.description = "A gem to provide swappable JSON backends utilizing Yajl::Ruby, the JSON gem, ActiveSupport, or JSON pure."
  s.email = ["michael@intridea.com"]
  s.extra_rdoc_files = ["LICENSE", "README.rdoc"]
  s.files = ["LICENSE", "README.rdoc"]
  s.homepage = "http://github.com/intridea/multi_json"
  s.rdoc_options = ["--charset=UTF-8"]
  s.require_paths = ["lib"]
  s.rubygems_version = "1.8.24"
  s.summary = "A gem to provide swappable JSON backends."

  if s.respond_to? :specification_version then
    s.specification_version = 3

    if Gem::Version.new(Gem::VERSION) >= Gem::Version.new('1.2.0') then
    else
    end
  else
  end
end
