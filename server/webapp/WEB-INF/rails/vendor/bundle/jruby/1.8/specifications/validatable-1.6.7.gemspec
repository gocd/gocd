# -*- encoding: utf-8 -*-

Gem::Specification.new do |s|
  s.name = "validatable"
  s.version = "1.6.7"

  s.required_rubygems_version = Gem::Requirement.new(">= 0") if s.respond_to? :required_rubygems_version=
  s.authors = ["Jay Fields"]
  s.date = "2008-03-20"
  s.description = "Validatable is a library for adding validations."
  s.email = "validatable-developer@rubyforge.org"
  s.extra_rdoc_files = ["README"]
  s.files = ["README"]
  s.homepage = "http://validatable.rubyforge.org"
  s.rdoc_options = ["--title", "Validatable", "--main", "README", "--line-numbers"]
  s.require_paths = ["lib"]
  s.rubyforge_project = "validatable"
  s.rubygems_version = "1.8.24"
  s.summary = "Validatable is a library for adding validations."

  if s.respond_to? :specification_version then
    s.specification_version = 2

    if Gem::Version.new(Gem::VERSION) >= Gem::Version.new('1.2.0') then
    else
    end
  else
  end
end
