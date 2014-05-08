class Spork::AppFramework
  APP_FRAMEWORKS = []
  def self.inherited(child)
    APP_FRAMEWORKS << child
  end
  # Iterates through all SUPPORTED_FRAMEWORKS and returns the symbolic name of the project application framework detected.  Otherwise, returns :Unknown
  def self.detect_framework_name
    detect_framework.short_name
  end
  
  # Same as detect_framework_name, but returns an instance of the specific AppFramework class.
  def self.detect_framework_class
    APP_FRAMEWORKS.select(&:present?).first || Spork::AppFramework::Unknown
  end
  
  # Same as detect_framework_name, but returns an instance of the specific AppFramework class.
  def self.detect_framework
    detect_framework_class.new
  end

  # Initializes, stores, and returns a singleton instance of the named AppFramework.
  #
  # == Parameters
  #
  # # +name+ - A symbolic name of a AppFramework subclass
  #
  # == Example
  #
  #   Spork::AppFramework[:Rails]
  def self.[](name)
    instances[name] ||= const_get(name).new
  end
  
  def self.short_name
    name.gsub('Spork::AppFramework::', '')
  end
  
  # If there is some stuff out of the box that the Spork can do to speed up tests without the test helper file being bootstrapped, this should return false.
  def bootstrap_required?
    entry_point.nil?
  end
  
  # Abstract: The path to the file that loads the project environment, ie config/environment.rb.  Returns nil if there is none.
  def entry_point
    raise NotImplementedError
  end
  
  def preload(&block)
    yield
  end
  
  def short_name
    self.class.short_name
  end

  def self.present?
    raise "#{self} should have defined #{self}.present?, but didn't"
  end
  
  protected
    def self.instances
      @instances ||= {}
    end
end

Spork.detect_and_require('spork/app_framework/*.rb')
