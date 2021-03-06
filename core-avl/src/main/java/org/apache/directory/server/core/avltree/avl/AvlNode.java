/*
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 *
 */
package org.apache.directory.server.core.avltree.avl;


/**
 * AVL Tree Set node
 * 
 * @author Vladimir Lysyy (http://bobah.net)
 *
 */
final class AvlNode<T extends Comparable<T>>
{
    public AvlNode<T> parent = null;
    public AvlNode<T> left = null;
    public AvlNode<T> right = null;

    public int height = 0;
    public int balance = 0;
    public T value = null;


    public AvlNode( AvlNode<T> parent, T value )
    {
        this.parent = parent;
        this.value = value;
    }


    public AvlNode<T> reset( AvlNode<T> parent, T value )
    {
        this.parent = parent;
        left = null;
        right = null;

        height = 0;
        this.value = value;

        return this;
    }
}