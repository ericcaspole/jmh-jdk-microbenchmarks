/*
 * Copyright (c) 2023 Oracle and/or its affiliates. All rights reserved.
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

import java.security.*;
import java.net.*;
import java.io.*;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

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

import org.openjdk.bench.util.InMemoryJavaCompiler;

@State(Scope.Thread)
@Warmup(iterations = 10, time = 2)
@Measurement(iterations = 7, time = 2)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Fork(value = 2)
public class BuildAndCall70 {

  @Param({ "025", "100", "250", "500" })
  public int numberOfClasses;

  @Param({"20"})
  public int recurse;

  byte[][] compiledClasses;
  Class[] loadedClasses;
  String[] classNames;

  Object[] receivers1;

  int index = 0;
  Map<Object, Method[]> table1 = new HashMap<>();
  Map<Integer, Object> r1 = new HashMap<>();

  static String B(int count) {
    return "public class B" + count + " {"
            + "   static int intFieldA = 0;"
            + "   static int intFieldB = 0;"
            + "   static int intFieldC = 0;"
            + "   static int intFieldD = 0;"
            + "   int instA = 0;"
            + "   int instB = 0;"
            + "   int instC = 0;"
            + "   int instD = 0;"
            + " "
            + " "
            + "   public Boolean deep( Integer depth) { "
//            + "       System.out.println( this.getClass().getName() + depth );"
            + "       if (depth > 0) {"
            + "         instA += ((depth % 2) + intFieldA);"
            + "         return deepB(--depth);"
            + "       } else {"
            + "         intFieldA = depth;"
            + "         return instA % 2 == 0;"
            + "       }"
            + "   }"
            + " "
            + " "
            + "   public Boolean deepB( Integer depth) { "
//            + "       System.out.println( this.getClass().getName() + depth );"
            + "       if (depth > 0) {"
            + "         instB += ((depth % 2) + intFieldB);"
            + "         return deepC(--depth);"
            + "       } else {"
            + "         intFieldB = depth;"
            + "         return instB % 2 == 0;"
            + "       }"
            + "   }"
            + " "
            + " "
            + "   public Boolean deepC( Integer depth) { "
//            + "       System.out.println( this.getClass().getName() + depth );"
            + "       if (depth > 0) {"
            + "         instC += ((depth % 2) + intFieldC);"
            + "         return deepD(--depth);"
            + "       } else {"
            + "         intFieldC = depth;"
            + "         return instC % 2 == 0;"
            + "       }"
            + "   }"
            + " "
            + "   public Boolean deepD( Integer depth) { "
//            + "       System.out.println( this.getClass().getName() + depth );"
            + "       if (depth > 0) {"
            + "         instD += ((depth % 2) + intFieldD);"
            + "         return deep(--depth);"
            + "       } else {"
            + "         intFieldD = depth;"
            + "         return instD % 2 == 0;"
            + "       }"
            + "   }"
            + "}";
  }
  
  
  class BenchLoader extends ClassLoader {

    BenchLoader() {
      super();
    }

    BenchLoader(ClassLoader parent) {
      super(parent);
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
      if (name.equals(classNames[index] /* "B" + index */)) {
        assert compiledClasses[index] != null;
        return defineClass(name, compiledClasses[index],
                0,
                (compiledClasses[index]).length);
      } else {
        return super.findClass(name);
      }
    }
  }

  BuildAndCall70.BenchLoader loader1 = new BuildAndCall70.BenchLoader();

  static String nextText(int size) {
    ThreadLocalRandom tlr = ThreadLocalRandom.current();
    String word = tlr.ints(97, 123).limit(size).boxed().
            map(x -> x.toString()).
            map(x -> (new Character((char) Integer.parseInt(x))).toString()).
            collect(Collectors.joining());

    return word;
  }

  final String methodNames[] = {
    "deep",
    "deepB",
    "deepC",
    "deepD"
  };

  @Setup(Level.Trial)
  public void setupClasses() throws Exception {
    compiledClasses = new byte[numberOfClasses][];
    loadedClasses = new Class[numberOfClasses];
    classNames = new String[numberOfClasses];
    receivers1 = new Object[numberOfClasses];

    for (int i = 0; i < numberOfClasses; i++) {
      classNames[i] = "B" + i;
      compiledClasses[i] = InMemoryJavaCompiler.compile(classNames[i], B(i));
    }


    for (index = 0; index < compiledClasses.length; index++) {
      Class c = loader1.findClass(classNames[index]);
      loadedClasses[index] = c;
      receivers1[index] = c.newInstance();

      r1.put(index, receivers1[index]);

      Method[] methods = new Method[methodNames.length];
      IntStream.range(0, methodNames.length).forEach(m -> {
        try {
          methods[m] = c.getMethod(methodNames[m], Integer.class);
        } catch (Exception e) {
          System.out.println("Exception = " + e);
          e.printStackTrace();
          System.exit(-1);

        }
      });

      table1.put(receivers1[index], methods);

      // Warmup the methods to get compiled
      IntStream.range(0, 12000)/* .parallel() */.forEach(x -> {
                IntStream.range(0, methodNames.length).forEach(m -> {
                  try {
                    Object r = receivers1[index];
                    Method[] mi = table1.get(r);
                    mi[m].invoke(r, 32);
                  } catch (Exception e) {
                    System.out.println("Exception = " + e);
                    e.printStackTrace();
                    System.exit(-1);
                  }
                });

              });
    }

    System.gc();
  }

  Integer work(Blackhole bh) throws Exception {
    Integer sum = 0;
    ThreadLocalRandom tlr = ThreadLocalRandom.current();
    // Call a random method of each class once
    for (index = 1; index < compiledClasses.length; index++) {
      try {
        int whichM = tlr.nextInt(methodNames.length);
        Object r = r1.get(index);
        Method m = table1.get(r)[whichM];
        assert m != null;
//      System.out.println(m.getName());
        
        Boolean result = (Boolean) m.invoke(r, recurse);
        sum += (result == true ? 1 : 0);
      } catch (Exception e) {
        System.out.println("Exception = " + e);
      }
    }
    return sum;
  }

  @Benchmark
  @Threads(4)
  public void doWork4t(Blackhole bh) throws Exception {
    work(bh);
  }

  @Benchmark
  @Threads(2)
  public void doWork2t(Blackhole bh) throws Exception {
    work(bh);
  }

  @Benchmark
  @Threads(1)
  public void doWork1t(Blackhole bh) throws Exception {
    work(bh);
  }
}