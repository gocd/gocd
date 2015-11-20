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

package nokogiri;

import static nokogiri.internals.NokogiriHelpers.getNokogiriClass;
import static nokogiri.internals.NokogiriHelpers.nodeListToRubyArray;

import java.util.List;

import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyClass;
import org.jruby.RubyObject;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.javasupport.util.RuntimeHelpers;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Class for Nokogiri::XML::NodeSet
 *
 * @author sergio
 * @author Yoko Harada <yokolet@gmail.com>
 */
@JRubyClass(name="Nokogiri::XML::NodeSet")
public class XmlNodeSet extends RubyObject implements NodeList {
    private List<?> list;
    private RubyArray nodes;
    private IRubyObject doc;
    
    public XmlNodeSet(Ruby ruby, RubyClass klazz) {
        super(ruby, klazz);
    }

    /**
     * Create and return a copy of this object.
     *
     * @return a clone of this object
     */
    @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    void setNodes(RubyArray array) {
        this.nodes = array;

        IRubyObject first = array.first();
        initialize(array.getRuntime(), first);
    }

    private void setReference(XmlNodeSet reference) {
        this.nodes = null;
        IRubyObject first = reference.nodes.first();
        initialize(reference.getRuntime(), first);
    }
    
    public void setNodeList(NodeList nodeList) {
        setNodes(nodeListToRubyArray(getRuntime(), nodeList));
    }
    
    public void initialize(Ruby ruby, IRubyObject refNode) {
        if (refNode instanceof XmlNode) {
            XmlNode n = (XmlNode)refNode;
            doc = n.document(ruby.getCurrentContext());
            setInstanceVariable("@document", doc);
            if (doc != null) {
                RuntimeHelpers.invoke(ruby.getCurrentContext(), doc, "decorate", this);
            }
        }
    }

    public static IRubyObject newEmptyNodeSet(ThreadContext context) {
        return (XmlNodeSet)NokogiriService.XML_NODESET_ALLOCATOR.allocate(context.getRuntime(), getNokogiriClass(context.getRuntime(), "Nokogiri::XML::NodeSet"));
    }

    public long length() {
        if (nodes == null) return 0L;
        return nodes.length().getLongValue();
    }

    public void relink_namespace(ThreadContext context) {
        List<?> n = nodes.getList();

        for (int i = 0; i < n.size(); i++) {
            if (n.get(i) instanceof XmlNode) {
                ((XmlNode) n.get(i)).relink_namespace(context);
            }
        }
    }

    @JRubyMethod(name="&")
    public IRubyObject and(ThreadContext context, IRubyObject nodeSet){
        if (nodes == null) setNodes(RubyArray.newEmptyArray(context.getRuntime()));
        return newXmlNodeSet(context, (RubyArray) nodes.op_and(asXmlNodeSet(context, nodeSet).nodes));
    }

    @JRubyMethod
    public IRubyObject delete(ThreadContext context, IRubyObject node_or_namespace){
        if (nodes == null) return context.getRuntime().getNil();
        if (node_or_namespace instanceof XmlNamespace) {
            ((XmlNamespace)node_or_namespace).deleteHref();
        }
        return nodes.delete(context, asXmlNodeOrNamespace(context, node_or_namespace), Block.NULL_BLOCK);
    }

    @JRubyMethod
    public IRubyObject dup(ThreadContext context){
        if (nodes == null) setNodes(RubyArray.newEmptyArray(context.getRuntime()));
        return newXmlNodeSet(context, nodes.aryDup());
    }

    @JRubyMethod(name = "include?")
    public IRubyObject include_p(ThreadContext context, IRubyObject node_or_namespace){
        if (nodes == null) setNodes(RubyArray.newEmptyArray(context.getRuntime()));
        return nodes.include_p(context, asXmlNodeOrNamespace(context, node_or_namespace));
    }

    @JRubyMethod(name = {"length", "size"})
    public IRubyObject length(ThreadContext context) {
        if (nodes != null) return nodes.length();
        else return context.getRuntime().newFixnum(0);
    }

    @JRubyMethod(name="-")
    public IRubyObject op_diff(ThreadContext context, IRubyObject nodeSet){
        XmlNodeSet xmlNodeSet = newXmlNodeSet(context, this);
        if (nodes == null) setNodes(RubyArray.newEmptyArray(context.getRuntime()));
        xmlNodeSet.setNodes((RubyArray) nodes.op_diff(asXmlNodeSet(context, nodeSet).nodes));
        return xmlNodeSet;
    }

    @JRubyMethod(name={"|", "+"})
    public IRubyObject op_or(ThreadContext context, IRubyObject nodeSet){
        if (nodes == null) setNodes(RubyArray.newEmptyArray(context.getRuntime()));
        return newXmlNodeSet(context, (RubyArray) nodes.op_or(asXmlNodeSet(context, nodeSet).nodes));
    }

    @JRubyMethod(name = {"push", "<<"})
    public IRubyObject push(ThreadContext context, IRubyObject node_or_namespace) {
        if (nodes == null) setNodes(RubyArray.newEmptyArray(context.getRuntime()));
        nodes.append(asXmlNodeOrNamespace(context, node_or_namespace));
        return this;
    }

    @JRubyMethod(name={"[]", "slice"})
    public IRubyObject slice(ThreadContext context, IRubyObject indexOrRange){
        IRubyObject result;
        if (nodes == null) return context.getRuntime().getNil();
        if (context.getRuntime().is1_9()) {
            result = nodes.aref19(indexOrRange);
        } else {
            result = nodes.aref(indexOrRange);
        }
        if (result instanceof RubyArray) {
            return newXmlNodeSet(context, (RubyArray)result);
        } else {
            return result;
        }
    }

    @JRubyMethod(name={"[]", "slice"})
    public IRubyObject slice(ThreadContext context, IRubyObject start, IRubyObject length){
        IRubyObject result;
        if (nodes == null) return context.getRuntime().getNil();
        if (context.getRuntime().is1_9()) {
            result = nodes.aref19(start, length);
        } else {
            result = nodes.aref(start, length);
        }
        if (result instanceof RubyArray) return newXmlNodeSet(context, (RubyArray)result);
        else return context.getRuntime().getNil();
    }

    @JRubyMethod(name = {"to_a", "to_ary"})
    public IRubyObject to_a(ThreadContext context) {
        return nodes;
    }

    @JRubyMethod(name = {"unlink", "remove"})
    public IRubyObject unlink(ThreadContext context){
        if (nodes == null) setNodes(RubyArray.newEmptyArray(context.getRuntime()));
        IRubyObject[] arr = this.nodes.toJavaArrayUnsafe();
        long length = arr.length;
        for (int i = 0; i < length; i++) {
            if (arr[i] instanceof XmlNode) {
                ((XmlNode) arr[i] ).unlink(context);
            }
        }
        return this;
    }

    public static XmlNodeSet newXmlNodeSet(ThreadContext context, RubyArray array) {
        XmlNodeSet xmlNodeSet = (XmlNodeSet)NokogiriService.XML_NODESET_ALLOCATOR.allocate(context.getRuntime(), getNokogiriClass(context.getRuntime(), "Nokogiri::XML::NodeSet"));
        xmlNodeSet.setNodes(array);
        return xmlNodeSet;
    }

    private XmlNodeSet newXmlNodeSet(ThreadContext context, XmlNodeSet reference) {
        XmlNodeSet xmlNodeSet = (XmlNodeSet)NokogiriService.XML_NODESET_ALLOCATOR.allocate(context.getRuntime(), getNokogiriClass(context.getRuntime(), "Nokogiri::XML::NodeSet"));
        xmlNodeSet.setReference(reference);
        return xmlNodeSet;
    }

    private IRubyObject asXmlNodeOrNamespace(ThreadContext context, IRubyObject possibleNode) {
        if (possibleNode instanceof XmlNode || possibleNode instanceof XmlNamespace) {
            return possibleNode;
        } else {
            throw context.getRuntime().newArgumentError("node must be a Nokogiri::XML::Node or Nokogiri::XML::Namespace");
        }
    }

    private XmlNodeSet asXmlNodeSet(ThreadContext context, IRubyObject possibleNodeSet) {
//        if(!(possibleNodeSet instanceof XmlNodeSet)) {
        if(!RuntimeHelpers.invoke(context, possibleNodeSet, "is_a?",
                getNokogiriClass(context.getRuntime(), "Nokogiri::XML::NodeSet")).isTrue()) {
            throw context.getRuntime().newArgumentError("node must be a Nokogiri::XML::NodeSet");
        }
        XmlNodeSet xmlNodeSet = (XmlNodeSet)possibleNodeSet;
        if (xmlNodeSet.nodes == null) xmlNodeSet.setNodes(RubyArray.newEmptyArray(context.getRuntime()));
        return xmlNodeSet;
    }
    
    public int getLength() {
        if (nodes == null) return 0 ;
        return nodes.size();
    }
    
    public Node item(int index) {
        if (nodes == null) return null ;
        Object n = nodes.get(index);
        if (n instanceof XmlNode) return ((XmlNode)n).node;
        if (n instanceof XmlNamespace) return ((XmlNamespace)n).getNode();
        return null;
    }
}
