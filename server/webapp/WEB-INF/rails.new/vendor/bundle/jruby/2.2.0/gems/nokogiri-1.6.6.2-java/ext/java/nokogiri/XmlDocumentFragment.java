/**
 * (The MIT License)
 *
 * Copyright (c) 2008 - 2011:
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

package nokogiri;

import static nokogiri.internals.NokogiriHelpers.getLocalNameForNamespace;
import static nokogiri.internals.NokogiriHelpers.getNokogiriClass;
import static nokogiri.internals.NokogiriHelpers.getPrefix;
import static nokogiri.internals.NokogiriHelpers.isNamespace;
import static nokogiri.internals.NokogiriHelpers.rubyStringToString;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyClass;
import org.jruby.RubyString;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.javasupport.util.RuntimeHelpers;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;

/**
 * Class for Nokogiri::XML::DocumentFragment
 * 
 * @author sergio
 * @author Yoko Harada <yokolet@gmail.com>
 */
@JRubyClass(name="Nokogiri::XML::DocumentFragment", parent="Nokogiri::XML::Node")
public class XmlDocumentFragment extends XmlNode {
    private XmlElement fragmentContext = null;

    public XmlDocumentFragment(Ruby ruby) {
        this(ruby, getNokogiriClass(ruby, "Nokogiri::XML::DocumentFragment"));
    }

    public XmlDocumentFragment(Ruby ruby, RubyClass klazz) {
        super(ruby, klazz);
    }

    @JRubyMethod(name="new", meta = true, required=1, optional=2)
    public static IRubyObject rbNew(ThreadContext context, IRubyObject cls, IRubyObject[] args) {
        
        if(args.length < 1) {
            throw context.getRuntime().newArgumentError(args.length, 1);
        }

        if(!(args[0] instanceof XmlDocument)){
            throw context.getRuntime().newArgumentError("first parameter must be a Nokogiri::XML::Document instance");
        }

        XmlDocument doc = (XmlDocument) args[0];
        
        // make wellformed fragment, ignore invalid namespace, or add appropriate namespace to parse
        if (args.length > 1 && args[1] instanceof RubyString) {
            args[1] = trim(context, doc, (RubyString)args[1]);
            if (XmlDocumentFragment.isTag((RubyString)args[1])) {
                args[1] = RubyString.newString(context.getRuntime(), addNamespaceDeclIfNeeded(doc, rubyStringToString(args[1])));
            }
        }

        XmlDocumentFragment fragment = (XmlDocumentFragment) NokogiriService.XML_DOCUMENT_FRAGMENT_ALLOCATOR.allocate(context.getRuntime(), (RubyClass)cls);
        fragment.setDocument(context, doc);
        fragment.setNode(context, doc.getDocument().createDocumentFragment());

        //TODO: Get namespace definitions from doc.
        if (args.length == 3 && args[2] != null && args[2] instanceof XmlElement) {
            fragment.fragmentContext = (XmlElement)args[2];
        }
        RuntimeHelpers.invoke(context, fragment, "initialize", args);
        return fragment;
    }
    
    private static IRubyObject trim(ThreadContext context, XmlDocument xmlDocument, RubyString str) {
        // checks whether schema is given. if exists, allows whitespace processing to a parser
        Document document = (Document)xmlDocument.node;
        if (document.getDoctype() != null) return str;
        // strips trailing \n off forcefully
        // not to return new object in case of no chomp needed, chomp! is used here.
        IRubyObject result;
        if (context.getRuntime().is1_9()) result = str.chomp_bang19(context);
        else result = str.chomp_bang(context);
        return result.isNil() ? str : result;
    }

    private static boolean isTag(RubyString ruby_string) {
        String str = rubyStringToString(ruby_string);
        if (str.startsWith("<") && str.endsWith(">")) return true;
        return false;
    }

    private static Pattern qname_pattern = Pattern.compile("[^</:>\\s]+:[^</:>=\\s]+");
    private static Pattern starttag_pattern = Pattern.compile("<[^</>]+>");

    private static boolean isNamespaceDefined(String qName, NamedNodeMap nodeMap) {
        if (isNamespace(qName.intern())) return true;
        for (int i=0; i < nodeMap.getLength(); i++) {
            Attr attr = (Attr)nodeMap.item(i);
            if (isNamespace(attr.getNodeName())) {
                String localPart = getLocalNameForNamespace(attr.getNodeName());
                if (getPrefix(qName).equals(localPart)) {
                    return true;
                }
            }
        }
        return false;
    }
    
    private static String addNamespaceDeclIfNeeded(XmlDocument doc, String tags) {
        if (doc.getDocument() == null) return tags;
        if (doc.getDocument().getDocumentElement() == null) return tags;
        Matcher matcher = starttag_pattern.matcher(tags);
        Map<String, String> rewriteTable = new HashMap<String, String>();
        while(matcher.find()) {
            String start_tag = matcher.group();
            Matcher matcher2 = qname_pattern.matcher(start_tag);
            while(matcher2.find()) {
                String qName = matcher2.group();
                NamedNodeMap nodeMap = doc.getDocument().getDocumentElement().getAttributes();
                if (isNamespaceDefined(qName, nodeMap)) {
                    String namespaceDecl = getNamespceDecl(getPrefix(qName), nodeMap);
                    if (namespaceDecl != null) {
                        rewriteTable.put("<"+qName+">", "<"+qName + " " + namespaceDecl+">");
                    }
                }
            }
        }
        Set<String> keys = rewriteTable.keySet();
        for (String key : keys) {
            tags = tags.replace(key, rewriteTable.get(key));
        }
        
        return tags;
    }
    
    private static String getNamespceDecl(String prefix, NamedNodeMap nodeMap) {
        for (int i=0; i < nodeMap.getLength(); i++) {
            Attr attr = (Attr)nodeMap.item(i);
            if (prefix.equals(attr.getLocalName())) {
                return attr.getName() + "=\"" + attr.getValue() + "\"";
            }
        }
        return null;
    }

    public XmlElement getFragmentContext() {
        return fragmentContext;
    }

    //@Override
    public void add_child(ThreadContext context, XmlNode child) {
        // Some magic for DocumentFragment

        Ruby ruby = context.getRuntime();
        XmlNodeSet children = (XmlNodeSet) child.children(context);

        long length = children.length();

        RubyArray childrenArray = children.convertToArray();

        if(length != 0) {
            for(int i = 0; i < length; i++) {
                XmlNode item = (XmlNode) ((XmlNode) childrenArray.aref(ruby.newFixnum(i))).dup_implementation(context, true);
                add_child(context, item);
            }
        }
    }

    @Override
    public void relink_namespace(ThreadContext context) {
        ((XmlNodeSet) children(context)).relink_namespace(context);
    }
}
