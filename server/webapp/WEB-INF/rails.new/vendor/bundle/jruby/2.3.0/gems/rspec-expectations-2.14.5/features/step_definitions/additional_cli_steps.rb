# Useful for when the output is slightly different on different versions of ruby
Then /^the output should contain "([^"]*)" or "([^"]*)"$/ do |string1, string2|
  unless [string1, string2].any? { |s| all_output =~ regexp(s) }
    fail %Q{Neither "#{string1}" or "#{string2}" were found in:\n#{all_output}}
  end
end

Then /^the output should contain all of these:$/ do |table|
  table.raw.flatten.each do |string|
    assert_partial_output(string, all_output)
  end
end

Then /^the example(?:s)? should(?: all)? pass$/ do
  step %q{the output should contain "0 failures"}
  step %q{the exit status should be 0}
end

Then /^the example should fail$/ do
  step %q{the output should contain "1 failure"}
  step %q{the exit status should not be 0}
end
