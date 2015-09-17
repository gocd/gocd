guard :rspec, :all_on_start => false, :all_after_pass => false do
  watch(%r{^spec/.+_spec\.rb$})
  watch(%r{^lib/(.+)\.rb$})                { |m| "spec/#{m[1]}_spec.rb" }
  watch('spec/support/adapter_helper.rb')  { "spec/listen/adapters" }
  watch('spec/support/listener_helper.rb') { "spec/listen/listener_spec.rb" }
  watch('spec/support/fixtures_helper.rb') { "spec" }
  watch('spec/spec_helper.rb')             { "spec" }
end
