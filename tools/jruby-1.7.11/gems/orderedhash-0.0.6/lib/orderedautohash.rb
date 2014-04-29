#
# auto vivifying ordered hash that dumps as yaml nicely
#
require 'orderedhash' unless defined? OrderedHash

class AutoOrderedHash < OrderedHash
  def initialize(*args)
    super(*args){|a,k| a[k] = __class__.new(*args)}
  end
  def class # for nice yaml
    Hash
  end
  def __class__
    AutoOrderedHash
  end
end # class AutoOrderedHash

OrderedAutoHash = AutoOrderedHash

def OrderedAutoHash(*a, &b)
  OrderedAutoHash.new(*a, &b)
end
def AutoOrderedHash(*a, &b)
  AutoOrderedHash.new(*a, &b)
end
