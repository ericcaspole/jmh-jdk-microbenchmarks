/*
 * Copyright (c) 2024 Oracle and/or its affiliates. All rights reserved.
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

import java.lang.invoke.*;
import java.lang.management.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadLocalRandom;

import java.util.concurrent.atomic.AtomicInteger;
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
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.infra.ThreadParams;

import org.openjdk.bench.util.InMemoryJavaCompiler;

@State(Scope.Benchmark)
@Warmup(iterations = 25, time = 20)
@Measurement(iterations = 25, time = 20)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
//@Threads(Threads.HALF_MAX)
@Threads(6)
public class LoaderStressWithReflectionCalls {

    // 10000 seems to be a good number for high code cache occupancy
    @Param({"125000"})
    public int classes;

    // The range of these classes to use in the measured phase after the warm up
    @Param({"25000"})
    public int rangeOfClasses;

    // 6g live of 12g heap for actual app
    @Param({"200"})
    public int instances;

    // Deep stacks are common in complex enterprise apps
//    @Param({"175"})
    @Param({"90"})
    public int recurse;

    // 350 is close to an actual app run log thread count
    @Param({"350"})
    public int bgThreads;

    @Param({  "true" })
    public boolean stacksBean;

    @Param({ "true" })
    public boolean doThrows;

    @Param({"1100000"})
    private int stringCount;

    @Param({"48"})
    private int strLength;

    @Param({"5000"})
    private int replacements;

    byte[][] compiledClasses;
    Map<String, Class> loadedClasses;
    String[] classNames;

    static final BlockingDeque<String> strings = new LinkedBlockingDeque<>();

    int index = 0;
    Map<Class, Method[]> methodMap = new ConcurrentHashMap<>();
    List<Map> mapList = new ArrayList();
    Map<Class, Map<String, Object>> instList = new ConcurrentHashMap<>();

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
        return "import java.util.*; "
                + "import java.nio.file.*;"
                + "import java.lang.reflect.*;"
//                + "import java.util.function.*;"
                + " "
                + "public class B" + count + " {"
                + " "
                + " "
                + newLine
                + " "
                + "   public String toString() {"
                + "     return this.getClass().getName();"
                + "   }"
                + " "
                + newLine
                + " "
                + newLine
                + "   static int myId" + " = " + count + ";"
                + newLine
                + " "
                + "   public synchronized int getMyId() {"
                + "   return myId;"
                + "   }"
                + " "
                + "   static int intFieldA" + filler + " = 0;"

                + "    static int staticPadAA" + filler + " = 0;"
//                + "    static int staticPadAB" + filler + " = 0;"
//                + "    static int staticPadAC" + filler + " = 0;"
//                + "    static int staticPadAD" + filler + " = 0;"
//                + "    static int staticPadAE" + filler + " = 0;"
//                + "    static int staticPadAF" + filler + " = 0;"
//                + "    static int staticPadAG" + filler + " = 0;"
//                + "    static int staticPadAH" + filler + " = 0;"
//                + "    static int staticPadAI" + filler + " = 0;"
//                + "    static int staticPadAJ" + filler + " = 0;"
//                + "    static int staticPadAK" + filler + " = 0;"
//                + "    static int staticPadAL" + filler + " = 0;"
//                + "    static int staticPadAM" + filler + " = 0;"
//                + "    static int staticPadAN" + filler + " = 0;"
//                + "    static int staticPadAO" + filler + " = 0;"
//                + "    static int staticPadAP" + filler + " = 0;"
//                + "    static int staticPadAQ" + filler + " = 0;"
//                + "    static int staticPadAR" + filler + " = 0;"
//                + "    static int staticPadAS" + filler + " = 0;"
//                + "    static int staticPadAT" + filler + " = 0;"
                + " "
                + "   static int intFieldB" + filler + " = 0;"

                + "    static int staticPadBA" + filler + " = 0;"
//                + "    static int staticPadBB" + filler + " = 0;"
//                + "    static int staticPadBC" + filler + " = 0;"
//                + "    static int staticPadBD" + filler + " = 0;"
//                + "    static int staticPadBE" + filler + " = 0;"
//                + "    static int staticPadBF" + filler + " = 0;"
//                + "    static int staticPadBG" + filler + " = 0;"
//                + "    static int staticPadBH" + filler + " = 0;"
//                + "    static int staticPadBI" + filler + " = 0;"
//                + "    static int staticPadBJ" + filler + " = 0;"
//                + "    static int staticPadBK" + filler + " = 0;"
//                + "    static int staticPadBL" + filler + " = 0;"
//                + "    static int staticPadBM" + filler + " = 0;"
//                + "    static int staticPadBN" + filler + " = 0;"
//                + "    static int staticPadBO" + filler + " = 0;"
//                + "    static int staticPadBP" + filler + " = 0;"
//                + "    static int staticPadBQ" + filler + " = 0;"
//                + "    static int staticPadBR" + filler + " = 0;"
//                + "    static int staticPadBS" + filler + " = 0;"
//                + "    static int staticPadBT" + filler + " = 0;"
                + "   static int intFieldC" + filler + " = 0;"

                + "    static int staticPadCA" + filler + " = 0;"
//                + "    static int staticPadCB" + filler + " = 0;"
//                + "    static int staticPadCC" + filler + " = 0;"
//                + "    static int staticPadCD" + filler + " = 0;"
//                + "    static int staticPadCE" + filler + " = 0;"
//                + "    static int staticPadCF" + filler + " = 0;"
//                + "    static int staticPadCG" + filler + " = 0;"
//                + "    static int staticPadCH" + filler + " = 0;"
//                + "    static int staticPadCI" + filler + " = 0;"
//                + "    static int staticPadCJ" + filler + " = 0;"
//                + "    static int staticPadCK" + filler + " = 0;"
//                + "    static int staticPadCL" + filler + " = 0;"
//                + "    static int staticPadCM" + filler + " = 0;"
//                + "    static int staticPadCN" + filler + " = 0;"
//                + "    static int staticPadCO" + filler + " = 0;"
//                + "    static int staticPadCP" + filler + " = 0;"
//                + "    static int staticPadCQ" + filler + " = 0;"
//                + "    static int staticPadCR" + filler + " = 0;"
//                + "    static int staticPadCS" + filler + " = 0;"
//                + "    static int staticPadCT" + filler + " = 0;"
                + "   static int intFieldD" + filler + " = 0;"
                + " "
                + " "
                + " "
                + " "
                + "   volatile int instA = 0;"
                + " "
                + "    int padAA" + filler + " = 0;"
//                + "    int padAB" + filler + " = 0;"
//                + "    int padAC" + filler + " = 0;"
//                + "    int padAD" + filler + " = 0;"
//                + "    int padAE" + filler + " = 0;"
//                + "    int padAF" + filler + " = 0;"
//                + "    int padAG" + filler + " = 0;"
//                + "    int padAH" + filler + " = 0;"
//                + "    int padAI" + filler + " = 0;"
//                + "    int padAJ" + filler + " = 0;"
//                + "    int padAK" + filler + " = 0;"
//                + "    int padAL" + filler + " = 0;"
//                + "    int padAM" + filler + " = 0;"
//                + "    int padAN" + filler + " = 0;"
//                + "    int padAO" + filler + " = 0;"
//                + "    int padAP" + filler + " = 0;"
//                + "    int padAQ" + filler + " = 0;"
//                + "    int padAR" + filler + " = 0;"
//                + "    int padAS" + filler + " = 0;"
//                + "    int padAT" + filler + " = 0;"
                + " "
                + "    volatile int instB = 0;"
                + " "
                + "    int padBA" + filler + " = 0;"
//                + "    int padBB" + filler + " = 0;"
//                + "    int padBC" + filler + " = 0;"
//                + "    int padBD" + filler + " = 0;"
//                + "    int padBE" + filler + " = 0;"
//                + "    int padBF" + filler + " = 0;"
//                + "    int padBG" + filler + " = 0;"
//                + "    int padBH" + filler + " = 0;"
//                + "    int padBI" + filler + " = 0;"
//                + "    int padBJ" + filler + " = 0;"
//                + "    int padBK" + filler + " = 0;"
//                + "    int padBL" + filler + " = 0;"
//                + "    int padBM" + filler + " = 0;"
//                + "    int padBN" + filler + " = 0;"
//                + "    int padBO" + filler + " = 0;"
//                + "    int padBP" + filler + " = 0;"
//                + "    int padBQ" + filler + " = 0;"
//                + "    int padBR" + filler + " = 0;"
//                + "    int padBS" + filler + " = 0;"
//                + "    int padBT" + filler + " = 0;"
                + " "
                + "    volatile int instC = 0;"
                + " "
                + "    int padCA" + filler + " = 0;"
//                + "    int padCB" + filler + " = 0;"
//                + "    int padCC" + filler + " = 0;"
//                + "    int padCD" + filler + " = 0;"
//                + "    int padCE" + filler + " = 0;"
//                + "    int padCF" + filler + " = 0;"
//                + "    int padCG" + filler + " = 0;"
//                + "    int padCH" + filler + " = 0;"
//                + "    int padCI" + filler + " = 0;"
//                + "    int padCJ" + filler + " = 0;"
//                + "    int padCK" + filler + " = 0;"
//                + "    int padCL" + filler + " = 0;"
//                + "    int padCM" + filler + " = 0;"
//                + "    int padCN" + filler + " = 0;"
//                + "    int padCO" + filler + " = 0;"
//                + "    int padCP" + filler + " = 0;"
//                + "    int padCQ" + filler + " = 0;"
//                + "    int padCR" + filler + " = 0;"
//                + "    int padCS" + filler + " = 0;"
//                + "    int padCT" + filler + " = 0;"
                + " "
                + "    volatile int instD = 0;"
                + " "
                + " "
            + "    volatile Method targetMethod = null;"
            + " "
            + "    volatile Object target = null;"
            + " "
            + "   public void setTarget( Object t ) {"
            + "     target = t;"
            + "   }"
            + " "
            + "   public void setMethod( Method m) {"
            + "     targetMethod = m;"
            + "   }"
            + " "

                + newLine
                + "   public Integer get( Integer depth) throws Throwable { "
                + newLine
                + "       if (depth > 0) {"
                + newLine
                + "         instA += ((depth % 2) + intFieldA" + filler + " );"
                + newLine
//                + "         if ( " + doThrows + " == true && depth > 80 && instA % 587 == 0 ) {"
//                + " "
//                + "             synchronized(this) {"
//                + " "
//                + "                 int x = getMyId();"
//                + "                 instA = 0;"
//                + newLine
//                + "                 throw new Exception(\"Test exception: \" +  x  + \" - \" + this.getClass().getName() );"
//                + "             }"
//                + newLine
//                + "         }"
                + "         return  get2( --depth);"
//                + " "
//                + "         int newDepth =  next" + filler + ".applyAsInt( --depth );"
//                + "         return  get2( newDepth);"
                + newLine
                + newLine
                + "       } else {"
                + newLine
                + "         return  instA + getMyId();"
                + "       }"
                + "   }"
                + " "
                + " "
                + " "
                            + "   public Integer get2( Integer depth) throws Throwable { "
                            + "       if (depth > 0 ) {"
                            + "         instB += (getMyId() + intFieldB" + filler + " + 1 );"
//                            + "         if ( " + doThrows + " == true && depth > 80 && instB % 509 == 0 ) {"
//                            + "           int x = instB;"
//                            + "           instB = 0;"
//                            + newLine
//                            + "           throw new Exception(\"Test exception: \" +  x  + \" - \" + this.getClass().getName() );"
//                            + newLine
//                            + "         }"
//                            + " "
                            + "         return  get3( --depth);"
//                            + "         int newDepth =  next" + filler + ".applyAsInt( --depth );"
//                            + "         return  get3( newDepth);"
                            + newLine
                            + newLine
                            + "       } else {"
                            + "         return  instB;"
                            + "       }"
                            + "   }"
                            + " "
                            + " "
                            + "   public Integer get3( Integer depth) throws Throwable { "
                            + "       if (depth > 0 ) {"
                            + "         instC += ((depth % 2) + intFieldC" + filler + " );"
//                            + "         return  get4( --depth);"
                            + "         return  get11( --depth);"
//                            + "         int newDepth =  next" + filler + ".applyAsInt( --depth );"
//                            + "         return  get4( newDepth);"
//                            + "         return  get11( newDepth);"
                            + "       } else {"
                            + "         return  instC;"
                            + "       }"
                            + "   }"
                            + " "
                            + " "
                            + " "


                                // Not used but make class bigger

                
                            + " "
                            + "   public Integer get4( Integer depth) throws Throwable { "
                            + "       if (depth > 0 ) {"
                            + "         instC += ((depth % 2) + intFieldD" + filler + " );"
                            + "         return  get5( --depth);"
                            + "       } else {"
                            + "         return  instD;"
                            + "       }"
                            + "   }"
                            + " "
                            + " "
                            + " "
                            + "   public Integer get5( Integer depth) throws Throwable { "
                            + "       if (depth > 0 ) {"
                            + "         instC += ((depth % 2) + intFieldA" + filler + " );"
                            + "         return  get6( --depth);"
                            + "       } else {"
                            + "         return  instA;"
                            + "       }"
                            + "   }"
                            + " "
                            + " "
                            + " "
                            + "   public Integer get6( Integer depth) throws Throwable { "
                            + "       if (depth > 0 ) {"
                            + "         instC += ((depth % 2) + intFieldB" + filler + " );"
                            + "         return  get7( --depth);"
                            + "       } else {"
                            + "         return  instB;"
                            + "       }"
                            + "   }"
                            + " "
                            + " "
                            + " "
                            + "   public Integer get7( Integer depth) throws Throwable { "
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
                + " "
                + "   public Integer get11( Integer depth) throws Throwable { "
                + newLine
//                + "       if ( depth > 0 && target != null) {"
                + "       if ( depth > 0 ) {"
                + newLine
                + " "
            + "         instD += (getMyId() + intFieldD" + filler + " );"
//            + "         System.out.println ( this.getClass().getName() + \" - \" + depth);"
            + newLine
            + newLine
            + "         return  (Integer) targetMethod.invoke( target, --depth);"
                + newLine
                + "       } else {"
                + " "
                + newLine
                + "         return  instD + getMyId();"
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
            if (name.equals(classNames[index])) {
                assert compiledClasses[index] != null;
                return defineClass(name, compiledClasses[index],
                        0,
                        (compiledClasses[index]).length);
            } else {
                return super.findClass(name);
            }
        }
    }

//    StackStressLoaders.BenchLoader loader1 = new StackStressLoaders.BenchLoader();
    List<LoaderStressWithReflectionCalls.BenchLoader> loaders = new LinkedList<>();

    final String k = "key";
    final Integer v = 1000;

    final String methodNames[] = {
        "get",};

    ThreadMXBean tb;
//    List<GarbageCollectorMXBean> gcBeans;
    ThreadInfo[] ti;
    long[] possiblyDeadlockedIds;
    ReentrantLock dumpLock, checkLock;

    ExecutorService tpe = null;
    volatile boolean doBackground = true;

    class BackgroundWorker implements Runnable {

        public void run() {
            boolean yieldOrSpin = false;
            while (doBackground == true) {
                try {

                    // Keep building up string table
                    if (strings.size() < stringCount) {
                        addOneString();
                    }

                    executeOne();

                    // Remove the tail to keep circulating through memory
                    // And increase allocation rate during steady state
                    String lower = strings.removeLast().toLowerCase(Locale.US).intern();
                    int l = lower.length();
                    if (yieldOrSpin) {
                        Thread.sleep(250 + l);
                    } else {
                        Blackhole.consumeCPU(1000 + l);
                    }
                    yieldOrSpin = !yieldOrSpin;

                } catch (Throwable t) {
                    // some throws happen by design, carry on
//          System.out.println(Thread.currentThread().getName() + " - Exception = " + t);
                }
            }
        }

    }

    @TearDown(Level.Trial)
    public void shutDownT() throws Exception {
        doBackground = false;
        tpe.awaitTermination(9999, TimeUnit.SECONDS);
    }

    @TearDown(Level.Iteration)
    public void shutDownI(Blackhole bh) throws Exception {
        System.out.println("String list actual size:" + strings.size());
        // Do the stack dump stuff to churn memory
        dump(bh);
    }

    String addOneString() {
        ThreadLocalRandom tlr = ThreadLocalRandom.current();
        int currLength = tlr.nextInt(strLength / 2) + tlr.nextInt(strLength / 2) + 1;
        assert currLength > 0 && currLength < 86 : "currLength=" + currLength;
        String s = nextText(currLength).intern();
        strings.addFirst(s);
        return s;
    }

    @Setup(Level.Trial)
    public void setupClasses() throws Exception {

        System.getProperties().list(System.out);

        int sz = (int) ((double) stringCount * 0.95);
        IntStream.range(0, sz).parallel().forEach(n -> {
            addOneString();
        });

        if (rangeOfClasses > classes) {
            rangeOfClasses = classes - 1;
            System.out.println("Reset rangeOfClasses to " + rangeOfClasses);
        }


        tb = ManagementFactory.getThreadMXBean();
//        gcBeans = ManagementFactory.getGarbageCollectorMXBeans();
        dumpLock = new ReentrantLock();
        checkLock = new ReentrantLock();

        compiledClasses = new byte[classes][];
        loadedClasses = new ConcurrentHashMap<>();
        classNames = new String[classes];

        mapList.add(new HashMap());
        mapList.add(new LinkedHashMap());
        mapList.add(new WeakHashMap());

        mapList.get(0).put(k, v);
        mapList.get(1).put(k, v);
        mapList.get(2).put(k, v);

        MethodHandles.Lookup publicLookup = MethodHandles.publicLookup();

        final long start = System.currentTimeMillis();
        AtomicInteger runningTotal = new AtomicInteger(0);

        // -Djava.util.concurrent.ForkJoinPool.common.parallelism=9
        // may help on big machines
        IntStream.range(0, classes).parallel().forEach(i -> {
            classNames[i] = "B" + i;
            compiledClasses[i] = InMemoryJavaCompiler.compile(classNames[i].intern(),
//                    B(i, nextText(25).intern(), doThrows));
                    B(i, nextText(16).intern(), doThrows));
            int n = runningTotal.incrementAndGet();
            if (n % 2000 == 0 && n > 0) {
                System.out.println(n + " classes compiled in " + (System.currentTimeMillis() - start) + "ms");
            }
        });

        System.out.println("Classes compiled.");
        IntStream.range(0, 4).forEach(n ->{
            loaders.add( new BenchLoader());
        });        

        for (index = 0; index < compiledClasses.length/4; index++) {
            Class c = loaders.get(0).findClass(classNames[index]);
            loadedClasses.put(Integer.toString(index), c);
        }
        for (; index < compiledClasses.length/2; index++) {
            Class c = loaders.get(1).findClass(classNames[index]);
            loadedClasses.put(Integer.toString(index), c);
        }

        for (; index < compiledClasses.length/4 + compiledClasses.length/2; index++) {
            Class c = loaders.get(2).findClass(classNames[index]);
            loadedClasses.put(Integer.toString(index), c);
        }
        for (; index < compiledClasses.length; index++) {
            Class c = loaders.get(3).findClass(classNames[index]);
            loadedClasses.put(Integer.toString(index), c);
        }
        
        // Now the classes are spread across 4 loaders

        final long start2 = System.currentTimeMillis();
        AtomicInteger runningTotal2 = new AtomicInteger(0);

        IntStream.range(0, classes).parallel().forEach(cc -> {

            // Build the list of objects of this class
            ConcurrentHashMap<String, Object> receivers1 = new ConcurrentHashMap<>();

            Class c = loadedClasses.get(Integer.toString(cc));
            assert c != null : "No class? " + c;

            IntStream.range(0, instances).forEach(j -> {
                        try {
                            Object inst = c.newInstance();
                            receivers1.put(Integer.toString(j), inst);
                        } catch (Exception e) {
                            System.out.println("Exception = " + e);
                            e.printStackTrace();
                            System.exit(-1);
                        }
                    });
            instList.put(c, receivers1);

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

            methodMap.put(c, methods);

            int n = runningTotal2.incrementAndGet();
            if (n % 2000 == 0 && n > 0) {
                System.out.println(n + " classes prepared in " + (System.currentTimeMillis() - start2) + "ms");
            }

        });

        System.out.println("Classes and Objects created.");
        final long start3 = System.currentTimeMillis();
        AtomicInteger runningTotal3 = new AtomicInteger(0);

    // Now that all of the objects are created fill in the targets and targetMethods on each object
    MethodType sObj = MethodType.methodType(void.class, Object.class);
    MethodType sMethod = MethodType.methodType(void.class, Method.class);

    IntStream.range(0, compiledClasses.length).parallel().forEach(c -> {
      IntStream.range(0, instances).forEach(x -> {
        ThreadLocalRandom tlr = ThreadLocalRandom.current();
        try {
          // Get the instance we are going to set
//          Class currClass = loadedClasses[c];
          Class currClass = loadedClasses.get(Integer.toString(c));
          assert currClass != null : "No class? " + c;
          Object currObj = instList.get(currClass).get(Integer.toString(x));
          assert currObj != null : "No instance of " + currClass + " at " + x;

          // For each instance of class C
          //  choose a random class
          Class rClass = chooseClass();
          assert rClass != null;
          //  choose a random instance of that class
          Object rObj = chooseInstance(rClass);
          assert rObj != null;
          //  set the target as that instance

          MethodHandle mh1 = publicLookup.findVirtual(currClass, "setTarget", sObj);
          MethodHandle mh2 = publicLookup.findVirtual(currClass, "setMethod", sMethod);

          // Fill in the target and MH to call it
          assert currObj != null && rObj != null;
          mh1.invoke(currObj, rObj);
          Method[] methodsArray = methodMap.get(rClass);
          assert methodsArray != null : "No methods for " + rClass;
          Method tm = methodsArray[0];
          assert tm != null && currObj != null;
          mh2.invoke(currObj, tm);
        } catch (Throwable e) {
          System.out.println("Exception = " + e);
          e.printStackTrace();
          System.exit(-1);
        }

      });
    });

    System.out.println("Target Objects updated.");

    // Warmup the methods to get compiled
//        IntStream.range(0, compiledClasses.length). /* parallel(). */forEach(c -> {
        IntStream.range(0, rangeOfClasses).forEach(c -> {
                    IntStream.range(0, methodNames.length).forEach(m -> {
                        try {
                            Class cc = chooseClass();
                            assert cc != null;
                            Object r = chooseInstance(cc);
                            assert r != null;
                            Method mh = chooseMethod(cc);
                            assert mh != null;
                            IntStream.range(0, 1000).parallel().forEach(x -> {
                                try {
                                    mh.invoke(r, 5);
                                    
//                                    executeOne();                                    
                                    
                                } catch (Throwable e) {
                                }
                            });
                        } catch (Throwable e) {
                            System.out.println("Exception = " + e);
                            e.printStackTrace();
                            System.exit(-1);
                        }
                    });
                    int n = runningTotal3.incrementAndGet();
                    if (n % 2000 == 0 && n > 0) {
                        System.out.println(n + " classes warmed up in " + (System.currentTimeMillis() - start3) + "ms");
                    }
                });

        System.out.println("Warmup methods completed.");

        doBackground = true;

        tpe = Executors.newFixedThreadPool(bgThreads);
        for (int i = 0; i < bgThreads; i++) {
            tpe.execute(new BackgroundWorker());
        }
        // Shutdown input on the thread queue and wait for the jobs to complete
        tpe.shutdown();

        System.out.println("Background threads created.");

        System.gc();

    }

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    Class chooseClass() {
        ThreadLocalRandom tlr = ThreadLocalRandom.current();
        int whichClass = tlr.nextInt(rangeOfClasses);
        return loadedClasses.get(Integer.toString(whichClass).intern());

    }

    
    AtomicInteger replaceCounter = new AtomicInteger(0);


    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    Object chooseInstance(Class c) {
        ThreadLocalRandom tlr = ThreadLocalRandom.current();
        int whichInst = tlr.nextInt(instances);
        Map<String, Object> iMap = instList.get(c);
        String iKey = Integer.toString(whichInst).intern();
        assert iMap != null : "No insts for " + c + " / " + iKey;
        int r = replaceCounter.incrementAndGet();
        if (r == replacements) {
            try {
                // Churn these object lists
                Object inst = c.newInstance();
                iMap.remove(iKey);
                iMap.put(iKey, inst);
                replaceCounter.set(0);
                long t = System.currentTimeMillis();
//                System.out.println(t +": Replaced " + c.getName() + " " + iKey);        
            } catch (Throwable e) {}
        }
        return iMap.get(iKey);
    }

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    Method chooseMethod(Class c) {
        ThreadLocalRandom tlr = ThreadLocalRandom.current();
        int whichM = tlr.nextInt(methodNames.length);
        return methodMap.get(c)[whichM];
    }

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    Integer callTheMethod(Method m, Object r) throws Throwable {
        return (Integer) m.invoke(r, recurse);
    }

    boolean check() {
        ThreadLocalRandom tlr = ThreadLocalRandom.current();
        if (tlr.nextInt(100) < 15) {
            if (checkLock.tryLock()) {
                Path path = FileSystems.getDefault().getPath(".", "micros-jdk8-1.0-SNAPSHOT.jar");
                return Files.exists(path, LinkOption.NOFOLLOW_LINKS);
            }
        }
        return false;
    }

    void dump(Blackhole bh) {
        if (stacksBean == true) {
                if (dumpLock.tryLock()) {
                    bh.consume( tb.dumpAllThreads(true, true) );
                    possiblyDeadlockedIds = tb.findDeadlockedThreads();
                    bh.consume( possiblyDeadlockedIds );
                }
        }
    }

    int executeOne() throws Throwable {
        Class c = chooseClass();
        assert c != null;
        Object r = chooseInstance(c);
        Method m = chooseMethod(c);
        assert m != null;
        return callTheMethod(m, r);
    }

    Integer work(Blackhole bh, int id) /* throws Exception */ {
        Integer sum = 0;
        try {
            sum += executeOne();
        } catch (Throwable e) {
            //        System.out.println(Thread.currentThread().getName() + " - Exception = " + e);
            //        e.printStackTrace();
        }

        return check() == true ? sum : 0;
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    @Fork(value = 4, jvmArgs = {"-XX:+UseLargePages",
        "-XX:ReservedCodeCacheSize=2g", "-XX:InitialCodeCacheSize=2g",
        "-XX:+UnlockDiagnosticVMOptions", "-XX:+PrintCodeCache",
        "-XX:StartFlightRecording=delay=90s,dumponexit=true",
        "-Xmx12g", "-Xms12g", "-XX:+AlwaysPreTouch",
        "-XX:NativeMemoryTracking=summary", "-XX:+PrintNMTStatistics"})
    public void doWorkJfr(Blackhole bh, ThreadParams thdp) throws Exception {
        work(bh, thdp.getThreadIndex());
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    @Fork(value = 4, jvmArgs = {"-XX:+UseLargePages",
        "-XX:ReservedCodeCacheSize=2g", "-XX:InitialCodeCacheSize=2g",
        "-XX:+UnlockDiagnosticVMOptions", "-XX:+PrintCodeCache",
        "-Xmx12g", "-Xms12g", "-XX:+AlwaysPreTouch",
        "-XX:NativeMemoryTracking=summary", "-XX:+PrintNMTStatistics"})
    public void doWorkNoJfr(Blackhole bh, ThreadParams thdp) throws Exception {
        work(bh, thdp.getThreadIndex());
    }
}
