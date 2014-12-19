require 'helper'

describe 'scoped' do
  it 'accepts a deprecated conditions option' do
    assert_deprecated { Post.scoped(conditions: :foo) }.where_values.must_equal [:foo]
  end

  it 'accepts a deprecated include option' do
    assert_deprecated { Post.scoped(include: :foo) }.includes_values.must_equal [:foo]
  end

  it 'is deprecated' do
    assert_deprecated { Post.scoped }
  end
end
