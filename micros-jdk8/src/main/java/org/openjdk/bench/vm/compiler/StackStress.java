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
import java.lang.reflect.Constructor;
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
import static org.openjdk.bench.vm.compiler.StringTableStress.nextText;

@State(Scope.Benchmark)
@Warmup(iterations = 25, time = 3)
@Measurement(iterations = 25, time = 3)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Fork(value = 2, jvmArgsAppend = { "-XX:+UseLargePages",
    "-XX:ReservedCodeCacheSize=1g", "-XX:InitialCodeCacheSize=1g",
     "-XX:+UnlockDiagnosticVMOptions", "-XX:+PrintCodeCache", "-XX:-SegmentedCodeCache",
     "-XX:StartFlightRecording=delay=45s,dumponexit=true",
    "-Xmx12g", "-Xms12g", "-XX:+AlwaysPreTouch" })
public class StackStress {

  @Param({  "10000" })
  public int numberOfClasses;

  @Param({"125"})
  public int recurse;

  // 6g live of 12g heap for FA
  @Param({"1800"})
  public int instanceCount;

  @Param({"true" /* , "false" */})
  public boolean dumpStacksBean;

  @Param({"true" /* , "false" */})
  public boolean doThrows;

  @Param({"1100000"})
  private int stringCount;

  @Param({"85"})
  private int strLength;

  byte[][] compiledClasses;
  Class[] loadedClasses;
  String[] classNames;

  List<String>  strings = new ArrayList<>();

  int index = 0;
  Map<Object, MethodHandle[]> methodMap = new HashMap<>();
  List<Map> mapList = new ArrayList();
  Map<Class,List<Object>> instList = new HashMap<>();
  
  static String newLine = System.getProperty("line.separator");

  static final String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";

  static String nextText(int size) {
    ThreadLocalRandom tlr = ThreadLocalRandom.current();

    String word = tlr.ints(0, 52).limit(size).boxed().
            map(x -> alphabet.charAt(x)).
            map(x -> x.toString()).
            collect(Collectors.joining());

    return word;
  }

    static String B(int count, String filler, boolean doThrows) {
      return    "import java.util.*; "
            +  "import java.nio.file.*;"
            +  "import java.lang.invoke.*;"

            + " "
            + "public class B" + count + " {"
            + " "
            + " "
            + "    volatile Object target = null;"
            + " "
            + "    volatile MethodHandle targetMethod = null;"
            + " "
            + " "
            + "   public void setTarget( Object t ) {"
            + "     target = t;"
            + "   }"
            + " "
            + "   public void setMethod( MethodHandle m) {"
            + "     targetMethod = m;"
            + "   }"
            + " "
              + newLine
            + " "
            + "   public String toString() {"
              + "   return this.getClass().getName() + \" target=\" + target.getClass().getName() + \", targetMethod=\" + targetMethod;"
            + "   }"
            + " "
              + newLine
              + "   static int intFieldA" + filler + " = 0;"
                + "    static int staticPadAA" + filler + " = 0;"
                + "    static int staticPadAB" + filler + " = 0;"
                + "    static int staticPadAC" + filler + " = 0;"
                + "    static int staticPadAD" + filler + " = 0;"
                + "    static int staticPadAE" + filler + " = 0;"
                + "    static int staticPadAF" + filler + " = 0;"
                + "    static int staticPadAG" + filler + " = 0;"
                + "    static int staticPadAH" + filler + " = 0;"
                + "    static int staticPadAI" + filler + " = 0;"
                + "    static int staticPadAJ" + filler + " = 0;"
                + "    static int staticPadAK" + filler + " = 0;"
                + "    static int staticPadAL" + filler + " = 0;"
                + "    static int staticPadAM" + filler + " = 0;"
                + "    static int staticPadAN" + filler + " = 0;"
                + "    static int staticPadAO" + filler + " = 0;"
                + "    static int staticPadAP" + filler + " = 0;"
                + "    static int staticPadAQ" + filler + " = 0;"
                + "    static int staticPadAR" + filler + " = 0;"
                + "    static int staticPadAS" + filler + " = 0;"
                + "    static int staticPadAT" + filler + " = 0;"
            + " "

            + "   static int intFieldB" + filler + " = 0;"
                + "    static int staticPadBA" + filler + " = 0;"
                + "    static int staticPadBB" + filler + " = 0;"
                + "    static int staticPadBC" + filler + " = 0;"
                + "    static int staticPadBD" + filler + " = 0;"
                + "    static int staticPadBE" + filler + " = 0;"
                + "    static int staticPadBF" + filler + " = 0;"
                + "    static int staticPadBG" + filler + " = 0;"
                + "    static int staticPadBH" + filler + " = 0;"
                + "    static int staticPadBI" + filler + " = 0;"
                + "    static int staticPadBJ" + filler + " = 0;"
                + "    static int staticPadBK" + filler + " = 0;"
                + "    static int staticPadBL" + filler + " = 0;"
                + "    static int staticPadBM" + filler + " = 0;"
                + "    static int staticPadBN" + filler + " = 0;"
                + "    static int staticPadBO" + filler + " = 0;"
                + "    static int staticPadBP" + filler + " = 0;"
                + "    static int staticPadBQ" + filler + " = 0;"
                + "    static int staticPadBR" + filler + " = 0;"
                + "    static int staticPadBS" + filler + " = 0;"
                + "    static int staticPadBT" + filler + " = 0;"

            + "   static int intFieldC" + filler + " = 0;"
                + "    static int staticPadCA" + filler + " = 0;"
                + "    static int staticPadCB" + filler + " = 0;"
                + "    static int staticPadCC" + filler + " = 0;"
                + "    static int staticPadCD" + filler + " = 0;"
                + "    static int staticPadCE" + filler + " = 0;"
                + "    static int staticPadCF" + filler + " = 0;"
                + "    static int staticPadCG" + filler + " = 0;"
                + "    static int staticPadCH" + filler + " = 0;"
                + "    static int staticPadCI" + filler + " = 0;"
                + "    static int staticPadCJ" + filler + " = 0;"
                + "    static int staticPadCK" + filler + " = 0;"
                + "    static int staticPadCL" + filler + " = 0;"
                + "    static int staticPadCM" + filler + " = 0;"
                + "    static int staticPadCN" + filler + " = 0;"
                + "    static int staticPadCO" + filler + " = 0;"
                + "    static int staticPadCP" + filler + " = 0;"
                + "    static int staticPadCQ" + filler + " = 0;"
                + "    static int staticPadCR" + filler + " = 0;"
                + "    static int staticPadCS" + filler + " = 0;"
                + "    static int staticPadCT" + filler + " = 0;"

            + "   static int intFieldD" + filler + " = 0;"
            + " "
            + " "
            + " "
//            + "   Integer instA = new Integer( " + count + ");"
            + "   volatile Integer instA = 0;"
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
//                + "    Integer instB = new Integer(" + count + 1 + ");"
                + "    volatile Integer instB = 0;"
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
//                + "    Integer instC = new Integer(" + count + 2 + ");"
                + "    volatile Integer instC = 0;"
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
//                + "    Integer instD = new Integer(" + count + 3 + ");"
                + "    volatile Integer instD = 0;"
            + " "
            + " "

              + newLine
            + "   public Integer get( Integer depth) throws Throwable { "
              + newLine
            + "       if (depth > 0) {" 
              + newLine
              + newLine
//            + "         (new Throwable()).printStackTrace();"
              + newLine
            + "         instA += ((depth % 2) + intFieldA" + filler + " );"
              + newLine
//            + "         System.out.println ( \" ### get: this = \" + this + \", depth =  \" + depth);"
            + "         return  get2( --depth);"
              + newLine
            + "       } else {"
              + newLine
            + "         return  instA;"
            + "       }"
            + "   }"
            + " "
            + " "
            + "   public Integer get2( Integer depth) throws Throwable { "
//            + "         System.out.println ( m + \" / \" + k);"
            + "       if (depth > 0 ) {"
//            + "         instB += ((depth % 2) + intFieldB" + filler + " );"
            + "         instB += ((depth % 2) + intFieldB" + filler + " + 1 );"
            + "         if ( " + doThrows + " == true && depth > 80 && instB % 509 == 0 ) {"
            + "           int x = instB;"
            + "           instB = 0;"                
            + "           throw new Exception(\"Test exception: \" + x );"         
            + "         }"
            + "         return  get3( --depth);"
            + "       } else {"
            + "         return  instB;"
            + "       }"
            + "   }"
            + " "
            + " "
            + " "
            + "   public Integer get3( Integer depth) throws Throwable { "
//            + "         System.out.println ( m + \" / \" + k);"
            + "       if (depth > 0 ) {"
            + "         instC += ((depth % 2) + intFieldC" + filler + " );"
            + "         return  get4( --depth);"
            + "       } else {"
            + "         return  instC;"
            + "       }"
            + "   }"
            + " "
            + " "
            + " "
            + " "
            + "   public Integer get4( Integer depth) throws Throwable { "
//            + "         System.out.println ( m + \" / \" + k);"
            + "       if (depth > 0 ) {"
            + "         instC += ((depth % 2) + intFieldD" + filler + " );"
//            + "         System.out.println ( \" ### get4: depth =  \" + depth);"
            + "         return  get5( --depth);"
            + "       } else {"
            + "         return  instD;"
            + "       }"
            + "   }"
            + " "
            + " "
            + " "
            + "   public Integer get5( Integer depth) throws Throwable { "
//            + "         System.out.println ( m + \" / \" + k);"
            + "       if (depth > 0 ) {"
            + "         instC += ((depth % 2) + intFieldA" + filler + " );"
//            + "         System.out.println ( \" ### get5: depth =  \" + depth);"
            + "         return  get6( --depth);"
            + "       } else {"
            + "         return  instA;"
            + "       }"
            + "   }"
            + " "
            + " "
            + " "
            + "   public Integer get6( Integer depth) throws Throwable { "
//            + "         System.out.println ( m + \" / \" + k);"
            + "       if (depth > 0 ) {"
            + "         instC += ((depth % 2) + intFieldB" + filler + " );"
//            + "         System.out.println ( \" ### get6: depth =  \" + depth);"
            + "         return  get7( --depth);"
            + "       } else {"
            + "         return  instB;"
            + "       }"
            + "   }"
            + " "
            + " "
            + " "
            + "   public Integer get7( Integer depth) throws Throwable { "
//            + "         System.out.println ( m + \" / \" + k);"
            + "       if (depth > 0 ) {"
            + "         instC += ((depth % 2) + intFieldA" + filler + " );"
            + "         return  get8( --depth);"
            + "       } else {"
            + "         return  instA;"
            + "       }"
            + "   }"
            + " "
            + " "
            + " "
            + "   public Integer get8( Integer depth) throws Throwable { "
//            + "         System.out.println ( m + \" / \" + k);"
            + "       if (depth > 0 ) {"
            + "         instC += ((depth % 2) + intFieldB" + filler + " );"
            + "         return  get9( --depth);"
            + "       } else {"
            + "         return  instB;"
            + "       }"
            + "   }"
            + " "
            + " "
            + "   public Integer get9( Integer depth) throws Throwable { "
            + "       if (depth > 0 ) {"
            + "         instC += ((depth % 2) + intFieldC" + filler + " );"
            + "         return  get10( --depth);"
            + "       } else {"
            + "         return  instC;"
            + "       }"
            + "   }"
            + " "
            + " "
            + " "
            + " "
            + "   public Integer get10( Integer depth) throws Throwable { "
            + "       if (depth > 0 ) {"
            + "         instC += ((depth % 2) + intFieldD" + filler + " );"
            + "         return  get11( --depth);"
            + "       } else {"
            + "         return  instD;"
            + "       }"
            + "   }"
            + " "
            + " "
            + "   public Integer get11( Integer depth) throws Throwable { "
            + newLine
            + "       assert  target != null : this.getClass().getName();"
            + "       assert  targetMethod != null : this.getClass().getName();"
            + newLine
            + "       if ( depth > 0 && target != null) {"
            + newLine
            + "         instD += ((depth % 2) + intFieldD" + filler + " );"
//            + "         System.out.println ( this.getClass().getName() + \" - \" + depth);"
            + newLine
            + newLine
            + "         return  (Integer) targetMethod.invoke( target, --depth);"
            + newLine
            + "       } else {"
            + "         return  instD;"
            + "       }"
            + "   }"
            + " "
            + " "
            + " "
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
      if (name.equals(classNames[index] )) {
        assert compiledClasses[index] != null;
        return defineClass(name, compiledClasses[index],
                0,
                (compiledClasses[index]).length);
      } else {
        return super.findClass(name);
      }
    }
  }

  StackStress.BenchLoader loader1 = new StackStress.BenchLoader();

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

    IntStream.range(0, stringCount).parallel().forEach(n -> {
      String s = nextText(strLength).intern();
      strings.add(s);
    });

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

//    for (int i = 0; i < numberOfClasses; i++) {
//      classNames[i] = "B" + i;
//      compiledClasses[i] = InMemoryJavaCompiler.compile(classNames[i].intern(),
//                    B(i, nextText(25).intern(), doThrows));
//    }
    IntStream.range(0, numberOfClasses).parallel().forEach(i -> {
      classNames[i] = "B" + i;
      compiledClasses[i] = InMemoryJavaCompiler.compile(classNames[i].intern(),
                    B(i, nextText(25).intern(), doThrows));
    });

    for (index = 0; index < compiledClasses.length; index++) {
      Class c = loader1.findClass(classNames[index]);
      loadedClasses[index] = c;

      // Build the list of objects of this class
      List<Object> receivers1 = new LinkedList<>();
//      List<Object> receivers1 = new ArrayList<>();
//      for (int j = 0; j < instanceCount; j++) {
      IntStream.range(0, instanceCount).parallel().forEach(j -> {
        try{
          Object inst = c.newInstance();
          synchronized (receivers1) {
            receivers1.add(c.newInstance());
          }
        } catch (Exception e) {
                    System.out.println("Exception = " + e);
          e.printStackTrace();
          System.exit(-1);

        }
      });
      instList.put(c, receivers1);

      MethodHandle[] methods = new MethodHandle[methodNames.length];
      MethodType mt = MethodType.methodType(Integer.class, Integer.class);
      IntStream.range(0, methodNames.length).forEach(m -> {
        try {
          MethodHandle mh = publicLookup.findVirtual(c, methodNames[0], mt);
          methods[m] = mh;
        } catch (Exception e) {
          System.out.println("Exception = " + e);
          e.printStackTrace();
          System.exit(-1);
        }
      });

      methodMap.put(receivers1.get(0).getClass(), methods);

    }

    // Now that all of the objects are created fill in the targets and targetMethods on each object
    MethodType sObj = MethodType.methodType(void.class, Object.class);
    MethodType sMethod = MethodType.methodType(void.class, MethodHandle.class);

    IntStream.range(0, compiledClasses.length).parallel().forEach(c -> {
      IntStream.range(0, instanceCount).forEach(x -> {
        ThreadLocalRandom tlr = ThreadLocalRandom.current();
        try {
          // Get the instance we are going to set
          Class currClass = loadedClasses[c];
          Object currObj = instList.get(currClass).get(x);

          // For each instance of class C
          //  choose a random class
          Class rClass = loadedClasses[tlr.nextInt(loadedClasses.length)];
          //  choose a random instance of that class
          Object rObj = instList.get(rClass).get(tlr.nextInt(instanceCount));
          //  set the target as that instance

          MethodHandle mh1 = publicLookup.findVirtual(currClass, "setTarget", sObj);
          MethodHandle mh2 = publicLookup.findVirtual(currClass, "setMethod", sMethod);

          // Fill in the target and MH to call it
          assert currObj != null && rObj != null;
          mh1.invoke(currObj, rObj);

          MethodHandle tm = methodMap.get(rClass)[0];
          assert tm != null && currObj != null;
          mh2.invoke(currObj, tm);
        } catch (Throwable e) {
          System.out.println("Exception = " + e);
          e.printStackTrace();
          System.exit(-1);
        }

      });
    });

    System.gc();

    // Warmup the methods to get compiled
    IntStream.range(0, compiledClasses.length). parallel(). forEach(c -> {
      IntStream.range(0, methodNames.length).forEach(m -> {
          try {
            Object r = (instList.get(loadedClasses[c])).get(0);
            assert r != null;
            MethodHandle[] mi = methodMap.get(r.getClass());
            assert mi != null && mi[m] != null;
            MethodHandle mh = mi[m];
            assert mh != null;
            IntStream.range(0, 1000).forEach(x -> {
              try {
                mh.invoke(r,  8);
              } catch (Throwable e) {
                System.out.println("Exception = " + e);
                e.printStackTrace();
                System.exit(-1);
              }
            });
          } catch (Throwable e) {
            System.out.println("Exception = " + e);
            e.printStackTrace();
            System.exit(-1);
          }
      });
    });

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
  Integer callTheMethod(MethodHandle m, Object r, String k, Map map, int recurse) /* throws Throwable */ {
    try {
    return  (Integer) m.invoke(r,  recurse);
    } catch (Throwable t) {
//      System.out.println("### callTheMethod threw : " + t +", m = " + m + ", r = " + r);
    } 
    return 0;
  }

  boolean check() {
    Path path = FileSystems.getDefault().getPath(".", "micros-jdk8-1.0-SNAPSHOT.jar");
    return Files.exists(path, LinkOption.NOFOLLOW_LINKS);
  }

  void dump() {
    if (dumpStacksBean == true) {
      ThreadLocalRandom tlr = ThreadLocalRandom.current();
      if (tlr.nextInt(100) < 15) {
        if (dumpLock.tryLock()) {
          ti = tb.dumpAllThreads(true, true);
        }
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