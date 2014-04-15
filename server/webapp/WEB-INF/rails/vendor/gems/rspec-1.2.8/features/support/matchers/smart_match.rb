Spec::Matchers.define :smart_match do |expected|
  match do |actual|
    case expected
    when /^\/.*\/?$/
      actual =~ eval(expected)
    when /^".*"$/
      actual.index(eval(expected))
    else
      false
    end
  end
end
