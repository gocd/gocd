/**
 * (The MIT License)
 *
 * Copyright (c) 2008 - 2012:
 *
 * * {Aaron Patterson}[http://tenderlovemaking.com]
 * * {Mike Dalessio}[http://mike.daless.io]
 * * {Charles Nutter}[http://blog.headius.com]
 * * {Sergio Arbeo}[http://www.serabe.com]
 * * {Patrick Mahoney}[http://polycrystal.org]
 * * {Yoko Harada}[http://yokolet.blogspot.com]
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * 'Software'), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 * 
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED 'AS IS', WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
 * CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package nokogiri.internals;

import static nokogiri.internals.NokogiriHelpers.isNamespace;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import nokogiri.XmlNamespace;

import org.w3c.dom.Attr;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

/**
 * Cache of namespages of each node. XmlDocument has one cache of this class.
 * 
 * @author sergio
 * @author Yoko Harada <yokolet@gmail.com>
 */
public class NokogiriNamespaceCache {

    private List<Long> keys;  // order matters.
    private Map<Integer, CacheEntry> cache;  // pair of the index of a given key and entry
    private XmlNamespace defaultNamespace = null;

    public NokogiriNamespaceCache() {
        keys = new ArrayList<Long>();
        cache = new LinkedHashMap<Integer, CacheEntry>();
    }
    
    private Long hashCode(String prefix, String href) {
        long prefix_hash = prefix.hashCode();
        long href_hash = href.hashCode();
        return prefix_hash << 31 | href_hash;
    }

    public XmlNamespace get(String prefix, String href) {
        // prefix should not be null.
        // In case of a default namespace, an empty string should be given to prefix argument.
        if (prefix == null || href == null) return null;
        Long hash = hashCode(prefix, href);
        Integer index = keys.indexOf(hash);
        if (index != -1) return cache.get(index).namespace;
        return null;
    }
    
    public XmlNamespace getDefault() {
        return defaultNamespace;
    }
    
    public XmlNamespace get(String prefix) {
        if (prefix == null) return defaultNamespace;
        long h = prefix.hashCode();
        Long hash = h << 31;
        Long mask = 0xFF00L;
        for (int i=0; i < keys.size(); i++) {
            if ((keys.get(i) & mask) == hash) {
                return cache.get(i).namespace;
            }
        }
        return null;
    }
    
    public List<XmlNamespace> get(Node node) {
        List<XmlNamespace> namespaces = new ArrayList<XmlNamespace>();
        for (int i=0; i < keys.size(); i++) {
            CacheEntry entry = cache.get(i);
            if (entry.isOwner(node)) {
                namespaces.add(entry.namespace);
            }
        }
        return namespaces;
    }

    public void put(XmlNamespace namespace, Node ownerNode) {
        // prefix should not be null.
        // In case of a default namespace, an empty string should be given to prefix argument.
        String prefixString = namespace.getPrefix();
        String hrefString = namespace.getHref();
        Long hash = hashCode(prefixString, hrefString);
        Integer index;
        if ((index = keys.indexOf(hash)) != -1) {
            return;
        } else {
            keys.add(hash);
            index = keys.size() - 1;
            CacheEntry entry = new CacheEntry(namespace, ownerNode);
            cache.put(index, entry);
            if ("".equals(prefixString)) defaultNamespace = namespace;
        }
    }

    public void remove(String prefix, String href) {
        if (prefix == null || href == null) return;
        Long hash = hashCode(prefix, href);
        Integer index = keys.indexOf(hash);
        if (index != -1) {
            cache.remove(index);
        }
        keys.remove(index);
    }
    
    public void clear() {
        // removes namespace declarations from node
        for (int i=0; i < keys.size(); i++) {
            CacheEntry entry = cache.get(i);
            NamedNodeMap attributes = entry.ownerNode.getAttributes();
            for (int j=0; j<attributes.getLength(); j++) {
                String name = ((Attr)attributes.item(j)).getName();
                if (isNamespace(name)) {
                    attributes.removeNamedItem(name);
                }
            }
        }
        keys.clear();
        cache.clear();
        defaultNamespace = null;
    }
    
    public void replaceNode(Node oldNode, Node newNode) {
        for (int i=0; i < keys.size(); i++) {
            CacheEntry entry = cache.get(i);
            if (entry.isOwner(oldNode)) {
                entry.replaceOwner(newNode);
            }
        }
    }

    private class CacheEntry {
        private XmlNamespace namespace;
        private Node ownerNode;
        
        CacheEntry(XmlNamespace namespace, Node ownerNode) {
            this.namespace = namespace;
            this.ownerNode = ownerNode;
        }

        public Boolean isOwner(Node n) {
            return this.ownerNode.isSameNode(n);
        }

        public void replaceOwner(Node newNode) {
            this.ownerNode = newNode;
        }
    }
}
