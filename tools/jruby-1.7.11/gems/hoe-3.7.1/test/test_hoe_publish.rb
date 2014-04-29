require 'rubygems'
require 'minitest/autorun'
require 'hoe'

class TestHoePublish < Minitest::Test
  def setup
    @hoe = Hoe.spec 'blah' do
      self.version = '1.0'

      developer 'author', ''
      license 'MIT'
    end
  end

  def test_make_rdoc_cmd
    expected = %W[
                #{Gem.bin_wrapper "rdoc"}
                --title blah-1.0\ Documentation
                -o doc
                --main README.txt
                lib
                History.txt Manifest.txt README.txt
               ]
    assert_equal expected, @hoe.make_rdoc_cmd
  end
end
