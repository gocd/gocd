require "rake/clean"
require "rubygems/package_task"

GEM_SPEC = Gem::Specification.new do |s|
  # basic information
  s.name        = "mini_portile"
  s.version     = "0.5.3"
  s.platform    = Gem::Platform::RUBY

  # description and details
  s.summary     = "Simplistic port-like solution for developers"
  s.description = "Simplistic port-like solution for developers. It provides a standard and simplified way to compile against dependency libraries without messing up your system."

  # requirements
  s.required_ruby_version = ">= 1.8.7"
  s.required_rubygems_version = ">= 1.3.5"

  # dependencies (add_dependency)
  # development dependencies (add_development_dependency)

  # components, files and paths
  s.files = FileList["examples/Rakefile", "lib/**/*.rb", "Rakefile", "*.{rdoc,txt}"]

  s.require_path = 'lib'

  # documentation
  s.has_rdoc = true
  s.rdoc_options << '--main'  << 'README.rdoc' << '--title' << 'MiniPortile -- Documentation'

  s.extra_rdoc_files = %w(README.rdoc History.txt LICENSE.txt)

  # project information
  s.homepage          = 'http://github.com/luislavena/mini_portile'
  s.licenses          = ['MIT']

  # author and contributors
  s.author      = 'Luis Lavena'
  s.email       = 'luislavena@gmail.com'
end

Gem::PackageTask.new(GEM_SPEC) do |pkg|
  pkg.need_tar = false
  pkg.need_zip = false
end
