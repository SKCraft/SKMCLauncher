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

import java.util.Iterator;
import java.util.NoSuchElementException;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class IterableNodeList implements NodeList, Iterable<Node> {
    
    private NodeList list;
    
    public IterableNodeList(NodeList list) {
        this.list = list;
    }

    @Override
    public Node item(int index) {
        return list.item(index);
    }

    @Override
    public int getLength() {
        return list.getLength();
    }

    @Override
    public Iterator<Node> iterator() {
        final int len = getLength();
        
        return new Iterator<Node>() {
            private int index = 0;

            @Override
            public boolean hasNext() {
                return index < len;
            }

            @Override
            public Node next() {
                Node node = item(index);
                if (node == null) {
                    throw new NoSuchElementException();
                }
                index++;
                return node;
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

}
