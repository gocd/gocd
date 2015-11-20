# -*- ruby -*-

$:.unshift 'lib'
require './lib/hoe.rb'

Hoe.plugin :seattlerb
Hoe.plugin :isolate

Hoe.spec "hoe" do
  developer "Ryan Davis", "ryand-ruby@zenspider.com"

  self.group_name = "seattlerb"

  blog_categories << "Seattle.rb" << "Ruby"

  license "MIT"

  pluggable!
  require_rubygems_version '>= 1.4'

  dependency "rake", [">= 0.8", "< 11.0"] # FIX: to force it to exist pre-isolate
end

task :plugins do
  puts `find lib/hoe -name \*.rb | xargs grep -h module.Hoe::`.
    gsub(/module/, '*')
end

task :known_plugins do
  dep         = Gem::Dependency.new(/^hoe-/, Gem::Requirement.default)
  fetcher     = Gem::SpecFetcher.fetcher
  spec_tuples = fetcher.find_matching dep

  max = spec_tuples.map { |(tuple, source)| tuple.first.size }.max

  spec_tuples.each do |(tuple, source)|
    spec = Gem::SpecFetcher.fetcher.fetch_spec(tuple, URI.parse(source))
    puts "* %-#{max}s - %s (%s)" % [spec.name, spec.summary, spec.authors.first]
  end
end

[:redocs, :docs].each do |t|
  task t do
    cp "Hoe.pdf", "doc"
    sh "chmod ug+w doc/Hoe.pdf"
  end
end

# vim: syntax=ruby
