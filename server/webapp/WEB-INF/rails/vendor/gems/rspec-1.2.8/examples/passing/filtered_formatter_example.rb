# This demonstrates how you can write custom formatters to handle arbitrary
# options passed to the +describe+ and +it+ methods. To see it in action, stand
# in the project root and say:
# 
#   bin/spec -r examples/passing/filtered_formatter.rb examples/passing/filtered_formatter_example.rb -f FilteredFormatter
#   
# You should only see the examples and groups below that are not explicitly
# marked :show => false
#
#   group 1
#     example 1 a
#   group 3
#     example 3


describe "group 1", :show => true do
  it "example 1 a", :show => true do
  end
  it "example 1 b", :show => false do
  end
end

describe "group 2", :show => false do
  it "example 2" do
  end
end

describe "group 3" do
  it "example 3" do
  end
end