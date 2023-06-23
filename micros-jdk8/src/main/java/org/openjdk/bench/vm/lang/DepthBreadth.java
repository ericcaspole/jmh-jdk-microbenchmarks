/*
 * Copyright (c) 2023, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package org.openjdk.bench.vm.lang;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

@State(Scope.Thread)
@Warmup(iterations = 6, time = 3)
@Measurement(iterations = 5, time = 5)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class DepthBreadth {
  
  @Param({"8"})
  public int width;

  @Param({"8"})
  public int depth;

  @Param({"java.util.ArrayList", "java.util.LinkedList"})
  String listType;
  
  List<A> getNewList() {
    try {
    Class<?> c = Class.forName(listType);
    return (List<A>) c.newInstance();
    } catch(Exception e) {
      System.exit(-1);
    }
    return null;
  }

  
  static class A {
    List<A> children;
    
    int a01;
    int a02;
    int a03;
    int a04;
    
    int a05;
    int a06;
    int a07;
    int a08;
    
    int a09;
    int a10;
    int a11;
    int a12;
    
    int a13;
    int a14;
    int a15;
    int a16;
    
    int a17;
    int a18;
    int a19;
    int a20;
    
    
    public A(List<A> ch) {
      children = ch;
    }
  
    public A() {
    }
   
    void setChildren(List<A> c) {
      children= c;
      a20 = children.size();
    }

    List<A>  getChildren() {
      return children;
    }

  }


  A root = null;

//  A setupA(int depth  ) throws Exception {
//    A a = new A();
//    if (depth > 0) {
//      List<A> children = getNewList();
//      int d = depth - 1;
//      for (int j = 0; j < width; j++) {
//        children.add(setupA(d ));
//      }
//      a.setChildren(children);
//    }
//    return a;
//  }

  A setupA(int depth  ) {
    A a = new A();
    if (depth > 0) {
      int d = depth - 1;

        List<A> ch0 = IntStream.range(0, width).
                parallel().mapToObj( i -> setupA(d )).collect(Collectors.toList());

        List<A> children = getNewList();
        children.addAll(ch0);
        
        a.setChildren(children);
    }
    return a;
  }

  AbstractQueue<A> queue;

  @Setup
  public void setup() throws Exception {
    int d = depth;
    queue = new LinkedBlockingQueue<>();
//    queue = new ArrayBlockingQueue<>(8192*64);
    root = setupA(d );
    assert root != null;
    
    int s1 = getDepthFirstSum(root);
    int s2 = getBreadthFirstSum(root);
    assert s1 == s2 : "Sums not equal";
    
    System.gc();
  }

  int getDepthFirstSum(A a) {

    if (a.getChildren() == null || a.getChildren().size() == 0) {
      // Bottom most node with no children
      return a.a20;
    } else {
      int sum = 0;
      
      // Travel down to bottom most nodes
      List<A> ch = a.getChildren(); 
      for (int j = 0; j < ch.size(); j++) {
        sum += getDepthFirstSum(ch.get(j));
      }
      
      // Add the current node now
      return sum + a.a20;
    }
  }
  
  int getBreadthFirstSum(A a) {
    int sum = 0; //a.a20;

    try {

      assert queue.size() == 0 : "should be empty";
      queue.add(a);
      while (queue.peek() != null) {
        A a1 = queue.remove();

        // Visit this node
        sum += a1.a20;
        List<A> ch = a1.getChildren();

        if (ch != null && ch.size() > 0) {
          for (int j = 0; j < ch.size(); j++) {
            // Push each child node
            queue.add(ch.get(j));
          }

        }
      }

    } catch (Exception e) {
      System.exit(-1);
    }

    assert queue.size() == 0 : "should be empty";

    return sum;
  }
  
  
  @Benchmark
  public int depthFirst() {
    int t =   getDepthFirstSum(root);
    assert t != 0;
//    System.out.println("d = " + t);
    return t;
  }

  @Benchmark
  public int breadthFirst() {
    int t =   getBreadthFirstSum(root);
    assert t != 0;
//    System.out.println("b = " + t);
    return t;
  }
  
}
