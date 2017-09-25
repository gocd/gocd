#--
# Extensions to fixnum to define some constants missing from Ruby itself

class Fixnum

  unless constants.include? :MAX

    # future versions of Ruby may end up defining this constant
    # in a more portable way, as documented by Matz himself in:
    #
    #   https://bugs.ruby-lang.org/issues/7517
    #
    # ... but until such time, we define the constant ourselves
    MAX = (2**(0.size * 8 - 2) - 1) # :nodoc:

  end

end
