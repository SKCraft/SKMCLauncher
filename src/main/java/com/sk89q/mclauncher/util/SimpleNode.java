/*
 * SK's Minecraft Launcher
 * Copyright (C) 2010, 2011 Albert Pham <http://www.sk89q.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
*/

package com.sk89q.mclauncher.util;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Helper class for easy modification of nodes.
 * 
 * @author sk89q
 */
public class SimpleNode {
    
    private Document doc;
    private Element node;
    
    /**
     * Construct with the document and node.
     * 
     * @param doc document
     * @param node node
     */
    public SimpleNode(Document doc, Element node) {
        this.doc = doc;
        this.node = node;
    }

    /**
     * Add a node at the end to a node.
     * 
     * @param name name of node
     * @return new node
     */
    public SimpleNode addNode(String name) {
        return new SimpleNode(doc, XmlUtils.addNode(doc, node, name));
    }
    
    /**
     * Add value to the node. If the value is null, no value is added.
     * 
     * @param text text
     * @return this object
     */
    public SimpleNode addValue(String text) {
        if (text == null) return this;
        XmlUtils.addText(doc, node, text);
        return this;
    }
    
    /**
     * Add value to the node.
     * 
     * @param val value
     * @return this object
     */
    public SimpleNode addValue(boolean val) {
        addValue(val ? "true" : "false");
        return this;
    }
    
    /**
     * Set an attribute. If the text is null, no attribute is added.
     * 
     * @param name attribute name
     * @param text attribute text
     * @return this object
     */
    public SimpleNode setAttr(String name, String text) {
        if (text == null) return this;
        XmlUtils.setAttr(doc, node, name, text);
        return this;
    }

    /**
     * Get the node.
     * 
     * @return node
     */
    public Element getNode() {
        return node;
    }
    
}
