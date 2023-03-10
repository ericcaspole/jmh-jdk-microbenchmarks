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
@Warmup(iterations = 20, time = 3)
@Measurement(iterations = 8, time = 2)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
public class BuildAndCall93 {

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

    List<Map> mapList = new ArrayList();
//    final String k = "key";
//    final String v = "value";

    
  static String B(int count) {
    return    "import java.util.*; "
            + " "
            + "public class B" + count + " {"
            + " "
            + " "
            + "   public Integer get( Map m, String k, Integer depth) { "
//            + "         System.out.println ( m + \" / \" + k);"
            + "       if (depth > 0) {"
            + "         return (Integer) m.get(k) + get2(m, k, --depth);"
            + "       } else {"
            + "         return (Integer) m.get(k)+ 10;"            
            + "       }"
            + "   }"
            + " "
            + "   public Integer get2( Map m, String k, Integer depth) { "
//            + "         System.out.println ( m + \" / \" + k);"
            + "       if (depth > 0) {"
            + "         return (Integer) m.get(k) + get3(m, k, --depth);"
            + "       } else {"
            + "         return (Integer) m.get(k)+ 20;"            
            + "       }"
            + "   }"
            + " "
            + "   public Integer get3( Map m, String k, Integer depth) { "
//            + "         System.out.println ( m + \" / \" + k);"
            + "       if (depth > 0) {"
            + "         return (Integer) m.get(k) + get4(m, k, --depth);"
            + "       } else {"
            + "         return (Integer) m.get(k)+ 30;"            
            + "       }"
            + "   }"
            + " "
            + " "
            + "   public Integer get4( Map m, String k, Integer depth) { "
//            + "         System.out.println ( m + \" / \" + k);"
            + "       if (depth > 0) {"
            + "         return (Integer) m.get(k) + get5(m, k, --depth);"
            + "       } else {"
            + "         return (Integer) m.get(k)+ 40;"            
            + "       }"
            + "   }"
            + " "
            + " "
            + "   public Integer get5( Map m, String k, Integer depth) { "
//            + "         System.out.println ( m + \" / \" + k);"
            + "       if (depth > 0) {"
            + "         return (Integer) m.get(k) + get6(m, k, --depth);"
            + "       } else {"
            + "         return (Integer) m.get(k)+ 50;"            
            + "       }"
            + "   }"
            + " "
            + "   public Integer get6( Map m, String k, Integer depth) { "
//            + "         System.out.println ( m + \" / \" + k);"
            + "       if (depth > 0) {"
            + "         return (Integer) m.get(k) + get(m, k, --depth);"
            + "       } else {"
            + "         return (Integer) m.get(k)+ 60;"            
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

  BuildAndCall93.BenchLoader loader1 = new BuildAndCall93.BenchLoader();

  String k = "key";
  Integer v = 1000;

    static String nextText(int size) {
    ThreadLocalRandom tlr = ThreadLocalRandom.current();
    String word = tlr.ints(97, 123).limit(size).boxed().
            map(x -> x.toString()).
            map(x -> (new Character((char) Integer.parseInt(x))).toString()).
            collect(Collectors.joining());

    return word;
  }

  final String methodNames[] = {
    "get",
    "get2",
    "get3",
    "get4",
    "get5",
    "get6"
  };

  
    
  @Setup(Level.Trial)
  public void setupClasses() throws Exception {
    compiledClasses = new byte[numberOfClasses][];
    loadedClasses = new Class[numberOfClasses];
    classNames = new String[numberOfClasses];
    receivers1 = new Object[numberOfClasses];

    mapList.add( new HashMap());
    mapList.add( new LinkedHashMap());
    mapList.add( new WeakHashMap());

    mapList.get(0).put(k, v);
    mapList.get(1).put(k, v);
    mapList.get(2).put(k, v);

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
          methods[m] = c.getMethod(methodNames[m], java.util.Map.class, String.class, Integer.class);
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
                    mi[m].invoke(r, mapList.get(0), k, 5);
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
        int whichMap = tlr.nextInt(mapList.size());
        
        Integer result = (Integer) m.invoke(r,mapList.get(whichMap),k, recurse);
        
        assert result != null && result >= v;
//      System.out.println( "result = " + result);
        sum += result;
      } catch (Exception e) {
        System.out.println("Exception = " + e);
        e.printStackTrace();
      }
    }
    return sum;
  }

  @Benchmark
  @Threads(4)
  @Fork(value = 2, jvmArgs = {"-XX:ReservedCodeCacheSize=1g", "-XX:+UseLargePages", "-Xlog:pagesize*=debug"})
  public void doWork4tLgPg(Blackhole bh) throws Exception {
    work(bh);
  }

  @Benchmark
  @Threads(2)
  @Fork(value = 2, jvmArgs = {"-XX:ReservedCodeCacheSize=1g", "-XX:+UseLargePages", "-Xlog:pagesize*=debug"})
  public void doWork2tLgPg(Blackhole bh) throws Exception {
    work(bh);
  }

  @Benchmark
  @Threads(1)
  @Fork(value = 2, jvmArgs = {"-XX:ReservedCodeCacheSize=1g", "-XX:+UseLargePages", "-Xlog:pagesize*=debug"})
  public void doWork1tLgPg(Blackhole bh) throws Exception {
    work(bh);
  }

  @Benchmark
  @Threads(4)
  @Fork(value = 2, jvmArgs = {"-XX:ReservedCodeCacheSize=1g", "-Xlog:pagesize*=debug" })
  public void doWork4tDefPg(Blackhole bh) throws Exception {
    work(bh);
  }

  @Benchmark
  @Threads(2)
  @Fork(value = 2, jvmArgs = {"-XX:ReservedCodeCacheSize=1g", "-Xlog:pagesize*=debug" })
  public void doWork2tDefPg(Blackhole bh) throws Exception {
    work(bh);
  }

  @Benchmark
  @Threads(1)
  @Fork(value = 2, jvmArgs = {"-XX:ReservedCodeCacheSize=1g", "-Xlog:pagesize*=debug" })
  public void doWork1tDefPg(Blackhole bh) throws Exception {
    work(bh);
  }
}