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
@Warmup(iterations = 5, time = 2)
@Measurement(iterations = 6, time = 2)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Fork(value = 2)
public class BuildAndCall6 {

  @Param({ "025", "100", "250", "500" })
  public int numberOfClasses;

  @Param({"25"})
  public int numberOfStrings;

  @Param({"128"})
  public int strSize;

  byte[][] compiledClasses;
  Class[] loadedClasses;
  String[] classNames;

  Object[] receivers1;

  int index = 0;
  Map<Object, Method[]> table1 = new HashMap<>();
  Map<Integer, Object> r1 = new HashMap<>();
  Map<Integer, String> strings = new HashMap<>();

  static String[] searchStrings;

  static String B(int count) {
    return "public class B" + count + " {"
            + "   static int intField = 0;"
            + " "
            + "   public static Boolean compiledMethod(String s) { "
            + "       assert  s != null;"
            //                + "       System.out.println( s );"
            + "       Boolean x = s.contains(\"z\");"
            + "       Boolean y = containsY(s);"
            + "       intField += ((x == Boolean.TRUE && y == Boolean.TRUE) ? 1 : -1);"
            + "       return x || y;"
            + "   }"
            + " "
            + "   public static Boolean containsZ(String s) { "
            + "       assert  s != null;"
            //                + "       System.out.println( s );"
            + "       Boolean x = s.contains(\"z\");"
            + "       Boolean y = containsY(s);"
            + "       intField += ((x == Boolean.TRUE && y == Boolean.TRUE) ? 1 : -1);"
            + "       return x || y;"
            + "   }"
            + " "
            + "   public static Boolean containsY(String s) { "
            + "       assert  s != null;"
            //                + "       System.out.println( s );"
            //                + "       (new Throwable()).printStackTrace();"
            + "       Boolean y = s.contains(\"y\");"
            + "       Boolean x = containsX(s);"
            + "       intField += (x == Boolean.TRUE ? 1 : -1);"
            + "       return y||x;"
            + "   }"
            + " "
            + "   public static Boolean containsX(String s) { "
            + "       assert  s != null;"
            //                + "       System.out.println( s );"
            //                + "       (new Throwable()).printStackTrace();"
            + "       Boolean x = s.contains(\"x\");"
            + "       Boolean w = containsW(s);"
            + "       intField += (x == Boolean.TRUE ? 1 : -1);"
            + "       return x||w;"
            + "   }"
            + " "
            + "   public static Boolean containsW(String s) { "
            + "       assert  s != null;"
            //                + "       System.out.println( s );"
            //                + "       if (s.length() > 64 ) {"
            //                + "         (new Throwable()).printStackTrace();"
            //                + "       }"
            + "       Boolean x = s.contains(\"w\");"
            + "       intField += (x == Boolean.TRUE ? 1 : -1);"
            + "       return x;"
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

  BuildAndCall6.BenchLoader loader1 = new BuildAndCall6.BenchLoader();

  static String nextText(int size) {
    ThreadLocalRandom tlr = ThreadLocalRandom.current();
    String word = tlr.ints(97, 123).limit(size).boxed().
            map(x -> x.toString()).
            map(x -> (new Character((char) Integer.parseInt(x))).toString()).
            collect(Collectors.joining());

    return word;
  }

  String methodNames[] = {
    "containsZ",
    "containsY",
    "containsX",
    "containsW"
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

    searchStrings = new String[numberOfStrings];
    for (int j = 0; j < numberOfStrings; j++) {
      searchStrings[j] = nextText(strSize);
      strings.put(j, nextText(strSize));
    }

    for (index = 0; index < compiledClasses.length; index++) {
      Class c = loader1.findClass(classNames[index]);
      loadedClasses[index] = c;
      receivers1[index] = c.newInstance();

      r1.put(index, receivers1[index]);

      Method[] methods = new Method[4];
      IntStream.range(0, methodNames.length).forEach(m -> {
        try {
          methods[m] = c.getMethod(methodNames[m], String.class);
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
                    mi[m].invoke(r, nextText(64));
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
        int whichStr = tlr.nextInt(1, numberOfStrings);
        int whichM = tlr.nextInt(4);
        Object r = r1.get(index);
        String s = strings.get(whichStr);
        Method m = table1.get(r)[whichM];
        assert m != null;
//      System.out.println(m.getName());
        
        Boolean result = (Boolean) m.invoke(r, s);
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

  // Calls a random method on a random class instance on each invocation
  Integer work2(Blackhole bh) throws Exception {
    Integer sum = 0;
    Boolean result = Boolean.FALSE;
    ThreadLocalRandom tlr = ThreadLocalRandom.current();
    int which = tlr.nextInt(compiledClasses.length);
    int whichStr = tlr.nextInt(1, numberOfStrings);
    int whichM = tlr.nextInt(4);

    try {
      assert receivers1[which] != null;
      assert strings.get(whichStr) != null : whichStr + " no string";
      Object r = r1.get(which);
      String s = strings.get(whichStr);
      Method m = table1.get(r)[whichM];
            
      result = (Boolean) m.invoke(r, s);
      sum += (result == true ? 1 : 0);
    } catch (Exception e) {
      System.out.println("Exception = " + which);
      e.printStackTrace();
      System.exit(-1);

    }
    return sum;
  }

  @Benchmark
  @Threads(4)
  public void doRandom4t(Blackhole bh) throws Exception {
    work2(bh);
  }

  @Benchmark
  @Threads(1)
  public void doRandom1t(Blackhole bh) throws Exception {
    work2(bh);
  }
}
