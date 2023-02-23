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

/**
 *
 * @author ecaspole
 */
@State(Scope.Thread)
public class BuildAndCall4 {

   @Param({ "1"  , /*  "25" */ "500" })
   public int numberOfClasses;

   @Param({"2000"})
   public int numberOfStrings;

   @Param({"128"})
   public int strSize;

     byte[][] compiledClasses;
     Class[] loadedClasses;
     String[] classNames;

     byte[][] compiledClasses2;
     Class[] loadedClasses2;
     String[] classNames2;

     Method[] methods1;
     Method[] methods2;

     Object[] receivers1;
     Object[] receivers2;

     int index = 0;

     static String[] searchStrings;


    static String B(int count) {
        return "public class B" + count + " {"
                + "   static int intField = 0;"
                + " "
                + "   public static Boolean compiledMethod(String s) { "
                + "       assert  s != null;"
//                + "       System.out.println( s );"
                + "       Boolean x = s.contains(\"z\");"
                + "       Boolean y = otherMethod(s);"
                + "       intField += ((x == Boolean.TRUE && y == Boolean.TRUE) ? 1 : -1);"
                + "       return x || y;"
                + "   }"
                + " "
                + "   public static Boolean otherMethod(String s) { "
                + "       assert  s != null;"
//                + "       System.out.println( s );"
                + "       Boolean x = s.contains(\"y\");"
                + "       intField += (x == Boolean.TRUE ? 1 : -1);"
                + "       return x;"
                + "   }"
                + "}";
    }

    static String A(int count) {
        return "public class A" + count + " {"
                + "   static int intField = 0;"
                + " "
                + "   public static Boolean compiledMethod(String s) { "
                + "       assert  s != null;"
//                + "       System.out.println( s );"
                + "       Boolean x = s.contains(\"z\");"
                + "       Boolean y = otherMethod(s);"
                + "       intField += ((x == Boolean.TRUE && y == Boolean.TRUE) ? 1 : -1);"
                + "       return x || y;"
                + "   }"
                + " "
                + "   public static Boolean otherMethod(String s) { "
                + "       assert  s != null;"
//                + "       System.out.println( s );"
                + "       Boolean x = s.contains(\"y\");"
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
                assert compiledClasses[index]  != null;
                return defineClass(name, compiledClasses[index] , 0,
                        (compiledClasses[index]).length);
            } else if (name.equals(classNames2[index] )) {
                              assert compiledClasses2[index]  != null;
                return defineClass(name, compiledClasses2[index] , 0,
                        (compiledClasses2[index]).length);

            } else {
                return super.findClass(name);
            }
        }
    }

  BuildAndCall4.BenchLoader loader1 = new BuildAndCall4.BenchLoader();

  static String nextText(int size) {
    ThreadLocalRandom tlr = ThreadLocalRandom.current();
    String word = tlr.ints(97, 123).limit(size).boxed().
            map(x -> x.toString()).
            map(x -> (new Character((char) Integer.parseInt(x))).toString()).
            collect(Collectors.joining());

    return word;
  }

    @Setup(Level.Trial)
    public void setupClasses() throws Exception {
        compiledClasses = new byte[numberOfClasses][];
        loadedClasses = new Class[numberOfClasses];
        classNames = new String[numberOfClasses];
        methods1 = new Method[numberOfClasses];
        receivers1 = new Object[numberOfClasses];

        compiledClasses2 = new byte[numberOfClasses][];
        loadedClasses2 = new Class[numberOfClasses];
        classNames2 = new String[numberOfClasses];
        methods2 = new Method[numberOfClasses];
        receivers2 = new Object[numberOfClasses];

        for (int i = 0; i < numberOfClasses; i++) {
            classNames[i] = "B" + i;
            classNames2[i] = "A" + i;
            compiledClasses[i] = InMemoryJavaCompiler.compile(classNames[i], B(i));
            compiledClasses2[i] = InMemoryJavaCompiler.compile(classNames2[i], A(i));
        }

        searchStrings = new String[numberOfStrings];
        for (int j=0 ; j<numberOfStrings ; j++) {
          searchStrings[j] = nextText(strSize);
        }

        for (index = 0; index < compiledClasses.length; index++) {
            Class c = loader1.findClass(classNames[index]);
            Class c2 = loader1.findClass(classNames2[index]);
            loadedClasses[index] = c;
            loadedClasses2[index] = c2;
            receivers1[index] = c.newInstance();
            receivers2[index] = c2.newInstance();
            methods1[index] = c.getMethod("compiledMethod", String.class);
            methods2[index] = c2.getMethod("compiledMethod", String.class);

        // Warmup the methods to get compiled
        IntStream.range(0, 12000)/* .parallel() */.forEach(x -> {
          try {
            methods1[index].invoke(receivers1[index], nextText(64));
            methods2[index].invoke(receivers2[index], nextText(64));
          } catch (Exception e) {
            System.out.println("Exception = " + e);
          }
        });
      }
    }


  Integer work(Blackhole bh) throws Exception {
      Integer sum = 0;
      Boolean result = Boolean.FALSE;
      ThreadLocalRandom tlr = ThreadLocalRandom.current();
      int whichStr = tlr.nextInt(numberOfStrings);
      // Call each method once
      for (index = 0; index < compiledClasses.length; index++) {
              try {
                  result = (Boolean) methods1[index].invoke(receivers1[index], searchStrings[whichStr]);
                  result |= (Boolean) methods2[index].invoke(receivers2[index], searchStrings[whichStr]);
                  sum += ( result == true ? 1: 0);
              } catch (Exception e) {
                  System.out.println("Exception = " + e);
              }

      }
      return sum;
  }


  @Benchmark
  @Threads(4)
  @Warmup(iterations = 5, time = 2)
  @Measurement(iterations = 8, time = 2)
  @BenchmarkMode(Mode.AverageTime)
  @OutputTimeUnit(TimeUnit.MICROSECONDS)
  public void doWork4t(Blackhole bh)  throws Exception {
      work(bh);
  }

  @Benchmark
  @Threads(1)
  @Warmup(iterations = 5, time = 2)
  @Measurement(iterations = 8, time = 2)
  @BenchmarkMode(Mode.AverageTime)
  @OutputTimeUnit(TimeUnit.MICROSECONDS)
  public void doWork1t(Blackhole bh)  throws Exception {
      work(bh);
  }

    // Calls random methods on each invocation
    Integer work2(Blackhole bh) throws Exception {
        Integer sum = 0;
        Boolean result = Boolean.FALSE;
        ThreadLocalRandom tlr = ThreadLocalRandom.current();
          int which = tlr.nextInt(compiledClasses.length);
          int whichStr = tlr.nextInt(numberOfStrings);
            try {
                result = (Boolean) methods1[which].invoke(receivers1[which], searchStrings[whichStr]);
                result |= (Boolean) methods2[which].invoke(receivers2[which], searchStrings[whichStr]);
                sum += ( result == true ? 1: 0);
            } catch (Exception e) {
                System.out.println("Exception = " + e);
            }
        return sum;
    }

  @Benchmark
  @Threads(4)
  @Warmup(iterations = 5, time = 2)
  @Measurement(iterations = 8, time = 2)
  @BenchmarkMode(Mode.AverageTime)
  @OutputTimeUnit(TimeUnit.MICROSECONDS)
  public void callRandom4t(Blackhole bh)  throws Exception {
      work2(bh);
  }

  @Benchmark
  @Threads(1)
  @Warmup(iterations = 5, time = 2)
  @Measurement(iterations = 8, time = 2)
  @BenchmarkMode(Mode.AverageTime)
  @OutputTimeUnit(TimeUnit.MICROSECONDS)
  public void callRandom1t(Blackhole bh)  throws Exception {
      work2(bh);
  }
}
