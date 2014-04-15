#!/usr/bin/env ruby

class Array
	# "Peels" off a tuple of the elements at each successive index across multiple arrays.
	# Assumes self is an array of these multiple arrays. Stops when any of the arrays is 
	# exhausted. I stole this from a ruby mailing list somewhere. I also considered calling this each_tuple
    def peel(&p)
        collect { |a|
            a.length
        }.min.times { |i|
            yield collect { |a| a[i] }
        }
    end

	# syntactic sugar for Cipher
	def collect_peel(&p)
		collected = []
		peel { |a,b| collected << p.call(a,b) }
		collected
	end
end

class Fixnum
	def offset_mod(base)
		((self-1)%base)+1
	end
end
