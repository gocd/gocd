shared_context "isolate load path mutation" do
  original_load_path = nil
  before { original_load_path = $LOAD_PATH.dup }
  after  { $LOAD_PATH.replace(original_load_path) }
end

