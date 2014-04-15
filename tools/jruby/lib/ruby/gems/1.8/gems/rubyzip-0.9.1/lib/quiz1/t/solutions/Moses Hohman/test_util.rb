#!/usr/bin/env ruby
require 'test/unit'
require 'util'

class TestArray < Test::Unit::TestCase
	def test_peel_all_arrays_same_length
		tuples = []
		a1 = [1,3,5]
		a2 = [2,4,6]
		a3 = [1,4,9]
		[a1, a2, a3].peel { |x,y,z| tuples << [x,y,z] }
		assert_equal([[1,2,1],[3,4,4],[5,6,9]], tuples)
	end

	def test_peel_one_array_shorter
		tuples = []
		a1 = [1,3,5]
		a2 = [2,4]
		a3 = [1,4,9]
		[a1, a2, a3].peel { |x,y,z| tuples << [x,y,z] }
		assert_equal([[1,2,1],[3,4,4]], tuples)
	end

	def test_collect_peel
		a1 = [1,3,5]
		a2 = [2,4,6]
		assert_equal([3,7,11], [a1, a2].collect_peel { |a,b| a+b })
	end
end

class TestFixnum < Test::Unit::TestCase
	def test_offset_mod
		assert_equal(1, 27.offset_mod(26), "27 wrong")
		assert_equal(26, 26.offset_mod(26), "26 wrong")
		assert_equal(26, 0.offset_mod(26), "0 wrong")
		assert_equal(25, -1.offset_mod(26), "-1 wrong")
	end
end
