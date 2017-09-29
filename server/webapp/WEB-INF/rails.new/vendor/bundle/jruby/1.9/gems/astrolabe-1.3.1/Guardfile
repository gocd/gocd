# coding: utf-8

# This group allows Guard to skip running RuboCop if RSpec failed.
group :red_green_refactor, halt_on_fail: true do
  guard :rspec, all_on_start: true, cmd: 'bundle exec rspec --format Fuubar' do
    watch(%r{^spec/.+_spec\.rb$})
    watch(%r{^lib/(.+)\.rb$})        { |m| "spec/#{m[1]}_spec.rb" }
    watch('spec/spec_helper.rb')     { 'spec' }
    watch(%r{^spec/support/.+\.rb$}) { 'spec' }
    watch(%r{^benchmark/.+_spec\.rb$})
  end

  guard :rubocop, cli: '--format fuubar' do
    watch(%r{.+\.rb$})
    watch(%r{(?:.+/)?\.rubocop\.yml$}) { |m| File.dirname(m[0]) }
  end
end
