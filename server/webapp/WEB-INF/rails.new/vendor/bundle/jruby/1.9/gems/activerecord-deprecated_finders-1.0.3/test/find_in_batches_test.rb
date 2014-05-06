require 'helper'

describe 'find_in_batches' do
  after do
    Post.destroy_all
  end

  it 'accepts finder options' do
    foo = Post.create title: 'foo'
    Post.create title: 'bar'

    assert_deprecated do
      Post.find_in_batches(conditions: "title = 'foo'") do |records|
        records.must_equal [foo]
      end
    end
  end
end
