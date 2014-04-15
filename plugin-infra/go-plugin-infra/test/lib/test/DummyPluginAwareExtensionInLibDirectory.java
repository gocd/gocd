package lib.test;

import com.thoughtworks.go.plugin.api.annotation.Extension;
import com.thoughtworks.go.plugin.api.info.PluginDescriptor;
import com.thoughtworks.go.plugin.api.info.PluginDescriptorAware;

@Extension
public class DummyPluginAwareExtensionInLibDirectory implements PluginDescriptorAware {
    @Override
    public void setPluginDescriptor(PluginDescriptor descriptor) {
        throw new UnsupportedOperationException();
    }
}
