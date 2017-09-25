require File.expand_path(File.dirname(__FILE__) + '/../test_helper')

Expectations do
  expect "message" do
    errors = Validatable::Errors.new
    errors.add(:attribute, "message")
    errors.on(:attribute)
  end
  
  expect ["message"] do
    errors = Validatable::Errors.new
    errors.add(:attribute, "message")
    errors.raw(:attribute)
  end
  
  expect "something new" do
    errors = Validatable::Errors.new
    errors.add(:attribute, "something old")
    errors.replace(:attribute, ["something new"])
    errors.on(:attribute)
  end
  
  expect "Capitalized word" do
    errors = Validatable::Errors.new
    errors.humanize("capitalized_word")
  end
  
  expect "Capitalized word without" do
    errors = Validatable::Errors.new
    errors.humanize("capitalized_word_without_id")
  end
  
  expect ["A humanized message", "a base message"] do
    errors = Validatable::Errors.new
    errors.add(:base, "a base message")
    errors.add(:a_humanized, "message")
    errors.full_messages.sort
  end
  
  expect true do
    Validatable::Errors.included_modules.include?(Enumerable)
  end
  
  expect ["message1", "message2"] do
    errors = Validatable::Errors.new
    errors.add(:attribute, "message1")
    errors.add(:attribute, "message2")
    errors.on(:attribute)
  end
  
  expect 2 do
    errors = Validatable::Errors.new
    errors.add(:attribute, "message1")
    errors.add(:attribute, "message2")
    errors.count
  end 
  
  expect 2 do
    errors = Validatable::Errors.new
    errors.add(:attribute1, "message1")
    errors.add(:attribute2, "message2")
    errors.count
  end   
end