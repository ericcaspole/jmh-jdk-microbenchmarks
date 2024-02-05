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
package org.openjdk.bench.vm.compiler;

import java.lang.invoke.*;
import java.lang.management.*;
import java.lang.reflect.Method;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.CompilerControl;
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

@State(Scope.Benchmark)
@Warmup(iterations = 10, time = 3)
@Measurement(iterations = 10, time = 3)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class MethodHandleStress {

  @Param({  "100" })
  public int numberOfClasses;

  @Param({"47"})
  public int recurse;

  @Param({"10"})
  public int instanceCount;

  byte[][] compiledClasses;
  Class[] loadedClasses;
  String[] classNames;

  int index = 0;
  Map<Object, MethodHandle[]> methodMap = new HashMap<>();
  List<Map> mapList = new ArrayList();
  Map<Class,List> instList = new HashMap<>();

    static final String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";

    static String nextText(int size) {
        ThreadLocalRandom tlr = ThreadLocalRandom.current();
    
        String word = tlr.ints(0,52).limit(size).boxed().
                map(x -> alphabet.charAt(x)).
                map(x -> x.toString()).
                collect(Collectors.joining());
    
        return word;
      }

    static String B(int count, String filler) {
      return    "import java.util.*; "
            + " "
            + "public class B" + count + " {"
            + " "
            + "   static int intFieldA" + filler + " = 0;"
            + "   static int intFieldB" + filler + " = 0;"
            + "   static int intFieldC" + filler + " = 0;"
            + "   static int intFieldD" + filler + " = 0;"
            + " "
            + " "
            + "   int instA = 0;"
                + " "
                + "    int padAA" + filler + " = 0;"
                + "    int padAB" + filler + " = 0;"
                + "    int padAC" + filler + " = 0;"
                + "    int padAD" + filler + " = 0;"
                + "    int padAE" + filler + " = 0;"
                + "    int padAF" + filler + " = 0;"
                + "    int padAG" + filler + " = 0;"
                + "    int padAH" + filler + " = 0;"
                + "    int padAI" + filler + " = 0;"
                + "    int padAJ" + filler + " = 0;"
                + "    int padAK" + filler + " = 0;"
                + "    int padAL" + filler + " = 0;"
                + "    int padAM" + filler + " = 0;"
                + "    int padAN" + filler + " = 0;"
                + "    int padAO" + filler + " = 0;"
                + "    int padAP" + filler + " = 0;"
                + "    int padAQ" + filler + " = 0;"
                + "    int padAR" + filler + " = 0;"
                + "    int padAS" + filler + " = 0;"
                + "    int padAT" + filler + " = 0;"
                + " "
                + "    int instB = 0;"
                + " "
                + "    int padBA" + filler + " = 0;"
                + "    int padBB" + filler + " = 0;"
                + "    int padBC" + filler + " = 0;"
                + "    int padBD" + filler + " = 0;"
                + "    int padBE" + filler + " = 0;"
                + "    int padBF" + filler + " = 0;"
                + "    int padBG" + filler + " = 0;"
                + "    int padBH" + filler + " = 0;"
                + "    int padBI" + filler + " = 0;"
                + "    int padBJ" + filler + " = 0;"
                + "    int padBK" + filler + " = 0;"
                + "    int padBL" + filler + " = 0;"
                + "    int padBM" + filler + " = 0;"
                + "    int padBN" + filler + " = 0;"
                + "    int padBO" + filler + " = 0;"
                + "    int padBP" + filler + " = 0;"
                + "    int padBQ" + filler + " = 0;"
                + "    int padBR" + filler + " = 0;"
                + "    int padBS" + filler + " = 0;"
                + "    int padBT" + filler + " = 0;"
                + " "
                + "    int instC = 0;"
                + " "
                + "    int padCA" + filler + " = 0;"
                + "    int padCB" + filler + " = 0;"
                + "    int padCC" + filler + " = 0;"
                + "    int padCD" + filler + " = 0;"
                + "    int padCE" + filler + " = 0;"
                + "    int padCF" + filler + " = 0;"
                + "    int padCG" + filler + " = 0;"
                + "    int padCH" + filler + " = 0;"
                + "    int padCI" + filler + " = 0;"
                + "    int padCJ" + filler + " = 0;"
                + "    int padCK" + filler + " = 0;"
                + "    int padCL" + filler + " = 0;"
                + "    int padCM" + filler + " = 0;"
                + "    int padCN" + filler + " = 0;"
                + "    int padCO" + filler + " = 0;"
                + "    int padCP" + filler + " = 0;"
                + "    int padCQ" + filler + " = 0;"
                + "    int padCR" + filler + " = 0;"
                + "    int padCS" + filler + " = 0;"
                + "    int padCT" + filler + " = 0;"
            + " "
            + "   int instD = 0;"
            + " "
            + " "
            + "   public Integer get( Map m, String k, Integer depth) { "
//            + "         System.out.println ( m + \" / \" + k);"
            + "       if (depth > 0) {"
            + "         instA += ((depth % 2) + intFieldA" + filler + " );"
            + "         return (Integer) m.get(k) + get2(m, k, --depth);"
            + "       } else {"
            + "         intFieldA" + filler + "  = depth;"
            + "         return (Integer) m.get(k)+ 10;"
            + "       }"
            + "   }"
            + " "
            + "   public synchronized Integer get2( Map m, String k, Integer depth) { "
//            + "         System.out.println ( m + \" / \" + k);"
            + "       if (depth > 0) {"
            + "         instB += ((depth % 2) + intFieldB" + filler + " );"
            + "         return (Integer) m.get(k) + get3(m, k, --depth);"
            + "       } else {"
            + "         intFieldB" + filler + "  = depth;"
            + "         return (Integer) m.get(k)+ 20;"
            + "       }"
            + "   }"
            + " "
            + "   public Integer get3( Map m, String k, Integer depth) { "
//            + "         System.out.println ( m + \" / \" + k);"
            + "       if (depth > 0) {"
            + "         instC += ((depth % 2) + intFieldC" + filler + " );"
            + "         return (Integer) m.get(k) + get4(m, k, --depth);"
            + "       } else {"
            + "         intFieldC" + filler + "  = depth;"
            + "         return (Integer) m.get(k)+ 30;"
            + "       }"
            + "   }"
            + " "
            + " "
            + "   public Integer get4( Map m, String k, Integer depth) { "
//            + "         System.out.println ( m + \" / \" + k);"
            + "       if (depth > 0) {"
            + "         instD += ((depth % 2) + intFieldD" + filler + " );"
            + "         return (Integer) m.get(k) + get5(m, k, --depth);"
            + "       } else {"
            + "         intFieldD" + filler + "  = depth;"
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
            + "         return (Integer) m.get(k)+ instA;"
            + "       }"
            + "   }"
            + " "
            + "   public Integer get6( Map m, String k, Integer depth) { "
//            + "         System.out.println ( m + \" / \" + k);"
            + "       if (depth > 0) {"
            + "         return (Integer) m.get(k) + get7(m, k, --depth);"
            + "       } else {"
            + "         return (Integer) m.get(k)+ instB;"
            + "       }"
            + "   }"
            + " "
            + "   public  Integer get7( Map m, String k, Integer depth) { "
//            + "         System.out.println ( m + \" / \" + k);"
            + "       if (depth > 0) {"
            + "         return (Integer) m.get(k) + get8(m, k, --depth);"
            + "       } else {"
            + "         return (Integer) m.get(k)+ instB;"
            + "       }"
            + "   }"
            + " "
            + "   public Integer get8( Map m, String k, Integer depth) { "
//            + "         System.out.println ( m + \" / \" + k);"
            + "       if (depth > 0) {"
            + "         return (Integer) m.get(k) + get9(m, k, --depth);"
            + "       } else {"
            + "         return (Integer) m.get(k)+ instB;"
            + "       }"
            + "   }"
            + " "
            + "   public synchronized Integer get9( Map m, String k, Integer depth) { "
//            + "         System.out.println ( m + \" / \" + k);"
            + "       if (depth > 0) {"
            + "         return (Integer) m.get(k) + get10(m, k, --depth);"
            + "       } else {"
            + "         return (Integer) m.get(k)+ instB;"
            + "       }"
            + "   }"
            + " "
            + "   public Integer get10( Map m, String k, Integer depth) { "
            + "       if (depth > 0) {"
            + "         return (Integer) m.get(k) + get11(m, k, --depth);"
            + "       } else {"
            + "         return (Integer) m.get(k)+ instB;"
            + "       }"
            + "   }"
            + " "
            + "   public Integer get11( Map m, String k, Integer depth) { "
            + "       if (depth > 0) {"
            + "         return (Integer) m.get(k) + get12(m, k, --depth);"
            + "       } else {"
            + "         return (Integer) m.get(k)+ instB;"
            + "       }"
            + "   }"
            + " "
            + "   public Integer get12( Map m, String k, Integer depth) { "
            + " try { "
            + "     if (depth % 19 == 0 ) { Thread.sleep((depth % 11) /* + 11 */); } "
            + " } catch (Exception e) {} "
            + " "
            + "       if (depth > 0) {"
            + "         return (Integer) m.get(k) + get(m, k, --depth);"
            + "       } else {"
            + "         return (Integer) m.get(k)+ instB;"
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

  MethodHandleStress.BenchLoader loader1 = new MethodHandleStress.BenchLoader();

  final String k = "key";
  final Integer v = 1000;

  final String methodNames[] = {
    "get",
  };

  ThreadMXBean tb;
  ThreadInfo[] ti;
  ReentrantLock dumpLock;

  @Setup(Level.Trial)
  public void setupClasses() throws Exception {

    tb = ManagementFactory.getThreadMXBean();
    dumpLock = new ReentrantLock();

    compiledClasses = new byte[numberOfClasses][];
    loadedClasses = new Class[numberOfClasses];
    classNames = new String[numberOfClasses];

    mapList.add( new HashMap());
    mapList.add( new LinkedHashMap());
    mapList.add( new WeakHashMap());

    mapList.get(0).put(k, v);
    mapList.get(1).put(k, v);
    mapList.get(2).put(k, v);
    
    MethodHandles.Lookup publicLookup = MethodHandles.publicLookup();


    for (int i = 0; i < numberOfClasses; i++) {
      classNames[i] = "B" + i;
//            compiledClasses[i] = InMemoryJavaCompiler.compile(classNames[i], B(i));
            compiledClasses[i] = InMemoryJavaCompiler.compile(classNames[i].intern(),
                    B(i, nextText(25).intern()));
    }

    for (index = 0; index < compiledClasses.length; index++) {
      Class c = loader1.findClass(classNames[index]);
      loadedClasses[index] = c;

      List<Object> receivers1 = new LinkedList<>();
      for (int j=0; j< instanceCount; j++) {
        receivers1.add( c.newInstance());
      }
      instList.put(c, receivers1);

      MethodHandle[] methods = new MethodHandle[methodNames.length];
      IntStream.range(0, methodNames.length).forEach(m -> {
        try {
//          methods[m] = c.getMethod(methodNames[m], java.util.Map.class, String.class, Integer.class);

            MethodType mt = MethodType.methodType(Integer.class, Map.class, String.class, Integer.class);
            MethodHandle mh = publicLookup.findVirtual(c, "get", mt);
            methods[m] =  mh;
            
        } catch (Exception e) {
          System.out.println("Exception = " + e);
          e.printStackTrace();
          System.exit(-1);
        }
      });

      methodMap.put(receivers1.get(0).getClass(), methods);

    // Warmup the methods to get compiled
//      IntStream.range(0, methodNames.length).parallel().forEach(m -> {
//        IntStream.range(0, 12000).forEach(x -> {
//                  try {
//                    Object r = instList.get(c).get(0);
//                    Method[] mi = methodMap.get(r.getClass());
//                    mi[m].invoke(r, mapList.get(0), k, 5);
//                  } catch (Exception e) {
//                    System.out.println("Exception = " + e);
//                    e.printStackTrace();
//                    System.exit(-1);
//                  }
//                });
//
//              });
    }

    System.gc();
  }

  @CompilerControl(CompilerControl.Mode.DONT_INLINE)
  Class chooseClass() {
    ThreadLocalRandom tlr = ThreadLocalRandom.current();
    int whichClass = tlr.nextInt(numberOfClasses);
    return loadedClasses[whichClass];
  }

  @CompilerControl(CompilerControl.Mode.DONT_INLINE)
  Object chooseInstance(Class c) {
    ThreadLocalRandom tlr = ThreadLocalRandom.current();
    int whichInst = tlr.nextInt(instanceCount);
    return instList.get(c).get(whichInst);
  }

  @CompilerControl(CompilerControl.Mode.DONT_INLINE)
  MethodHandle chooseMethod(Class c) {
    ThreadLocalRandom tlr = ThreadLocalRandom.current();
    int whichM = tlr.nextInt(methodNames.length);
    return methodMap.get(c)[whichM];
  }

  @CompilerControl(CompilerControl.Mode.DONT_INLINE)
  Map chooseMap() {
    ThreadLocalRandom tlr = ThreadLocalRandom.current();
        int whichMap = tlr.nextInt(mapList.size());
        return mapList.get(whichMap);
  }

  @CompilerControl(CompilerControl.Mode.DONT_INLINE)
  Integer callTheMethod(MethodHandle m, Object r, String k, Map map, int recurse) throws Throwable {
//    return  (Integer) m.invoke(r, map, k, recurse);
    return  (Integer) m.invoke(r, map, k, recurse);
  }
  
  boolean check() {
    Path path = FileSystems.getDefault().getPath(".", "micros-jdk8-1.0-SNAPSHOT.jar");
    return Files.exists(path, LinkOption.NOFOLLOW_LINKS);
  }
  
  void dump() {
    ThreadLocalRandom tlr = ThreadLocalRandom.current();
    if (tlr.nextInt(100) < 15) {
      if (dumpLock.tryLock()) {
        ti = tb.dumpAllThreads(true, true);
      }
    }
  }

  Integer work(Blackhole bh) throws Exception {
    Integer sum = 0;
    ThreadLocalRandom tlr = ThreadLocalRandom.current();
    
    dump();

    // Call a random method of a random class up to the specified range
    for (int index = 0; index < compiledClasses.length; index++) {
      try {

        Class c = chooseClass();
        Object r = chooseInstance(c);
        MethodHandle m = chooseMethod(c);
        assert m != null;
        Map map = chooseMap();
        
        Integer result = callTheMethod(m, r, k, map, tlr.nextInt(recurse));
        assert result != null && result >= v;
        sum += result;
      } catch (Throwable e) {
        System.out.println("Exception = " + e);
        e.printStackTrace();
      }
    }
    return check() == true ? sum : 0;
  }


  @Benchmark
  public void doWork(Blackhole bh) throws Exception {
    work(bh);
  }
}