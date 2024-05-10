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

import java.util.ArrayList;
import java.util.Deque;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
//import org.apache.commons.lang3.RandomStringUtils;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

@Warmup(iterations = 10)
@Measurement(iterations = 5, time = 65)
@BenchmarkMode(Mode.SampleTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
public class InternOrMap {

    //    Note: this is an example JMH for intern, it may need updates to
    //    run with multiple threads

    private static class Counter {
        private int current = 0;
        public int nextInRange(int range) {
            int r;
            if (range < current) {
                r = current++;
            }
            else {
                r = current = 0;
            }
            return r;
        }
    }

    @State(Scope.Benchmark)
    public static class StateHolder {
        @Param({"1000000"})
        public int numStrings;
        @Param({"10"})
        public int pctAlloc;

        private static final int NSTRINGS = 2000000;

        public final Deque<String> deque = new ConcurrentLinkedDeque<>();
        public final Counter r = new Counter();
        private final ArrayList<char[]> randomData = new ArrayList<>(NSTRINGS);

    static final String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz1234567890";

    static String nextText(int minSize, int maxSize) {
        ThreadLocalRandom tlr = ThreadLocalRandom.current();

        String word = tlr.ints(0, alphabet.length()).limit(tlr.nextInt(minSize, maxSize)).boxed().
                map(x -> alphabet.charAt(x)).
                map(x -> x.toString()).
                collect(Collectors.joining());

        return word;
    }

        public void setup(Map<String,String> map) {
            for (int i = 0; i < NSTRINGS; i++) {
//                randomData.add(RandomStringUtils.randomAscii(5, 256).toCharArray());
                randomData.add(nextText(5, 256).toCharArray());
            }
            Random random = new Random();
            for (int i = 0; i < numStrings; i++) {
                String s = new String(randomData.get(random.nextInt(NSTRINGS)));
                deque.add(s.intern());
                if (map != null) map.put(s, s);
            }
        }

        private int dataIndex = 0;
        private int nIterations = 1;
        public char[] getRandomData() {
            char[] c = randomData.get(dataIndex);
            dataIndex += 13;
            if (dataIndex >= NSTRINGS) {
                dataIndex = nIterations++;
            }
            return c;
        }

    }

    public static class StateIntern extends StateHolder {
        @Setup
        public void setup() {
            super.setup(null);
        }

    }

    @Benchmark
    @Fork(value = 2, jvmArgs = {"-XX:ReservedCodeCacheSize=1g",
        "-XX:+UnlockDiagnosticVMOptions",
        "-XX:-UseDynamicNumberOfCompilerThreads",
           "-XX:-SegmentedCodeCache",
           "-Xmx8g",
           "-Xms8g",
           "-XX:+AlwaysPreTouch",
           "-XX:StartFlightRecording:settings=none,+jdk.StringTableStatistics#enabled=true,+jdk.StringTableStatistics#period=10s",
           "-XX:+UseParallelGC"})
    public void testInternWithJFR(Blackhole bh, StateIntern si) {
        doTest(bh, si);
    }

    @Benchmark
    @Fork(value = 2, jvmArgs = {"-XX:ReservedCodeCacheSize=1g",
        "-XX:+UnlockDiagnosticVMOptions",
        "-XX:-UseDynamicNumberOfCompilerThreads",
           "-XX:-SegmentedCodeCache",
           "-Xmx8g",
           "-Xms8g",
           "-XX:+AlwaysPreTouch",
           "-XX:+UseParallelGC"})
    public void testInternWithoutJFR(Blackhole bh, StateIntern si) {
        doTest(bh, si);
    }

    private void doTest(Blackhole bh, StateIntern si) {
        if (si.r.nextInRange(100) < si.pctAlloc) {
            String s = new String(si.getRandomData());
            si.deque.add(s.intern());
            bh.consume(si.deque.remove());
        }
        else {
            String s = si.deque.removeFirst();
            si.deque.add(s);
            bh.consume(s.intern());
        }
    }

    @State(Scope.Benchmark)
    public static class StateConcurrent extends StateHolder {
        private WeakHashMap<String,String> canonicalStrings;

        @Setup
        public void setup() {
            canonicalStrings = new WeakHashMap<>(60013);
            super.setup(canonicalStrings);
        }
    }

    @Benchmark
    @Fork(value = 2, jvmArgs = {"-XX:ReservedCodeCacheSize=1g",
        "-XX:+UnlockDiagnosticVMOptions",
        "-XX:-UseDynamicNumberOfCompilerThreads",
           "-XX:-SegmentedCodeCache",
           "-Xmx8g",
           "-Xms8g",
           "-XX:+AlwaysPreTouch",
           "-XX:StartFlightRecording:settings=none,+jdk.StringTableStatistics#enabled=true,+jdk.StringTableStatistics#period=10s",
           "-XX:+UseParallelGC"})
    public void testMapWithJFR(Blackhole bh, StateConcurrent si) {
        doTest(bh, si);
    }

    @Benchmark
    @Fork(value = 2, jvmArgs = {"-XX:ReservedCodeCacheSize=1g",
        "-XX:+UnlockDiagnosticVMOptions",
        "-XX:-UseDynamicNumberOfCompilerThreads",
           "-XX:-SegmentedCodeCache",
           "-Xmx8g",
           "-Xms8g",
           "-XX:+AlwaysPreTouch",
           "-XX:+UseParallelGC"})
    public void testMapWithoutJFR(Blackhole bh, StateConcurrent si) {
        doTest(bh, si);
    }

    private void doTest(Blackhole bh, StateConcurrent si) {
        if (si.r.nextInRange(100) < si.pctAlloc) {
            String s = new String(si.getRandomData());
            si.deque.add(s);
            synchronized(si.canonicalStrings) {
                si.canonicalStrings.put(s, s);
            }
            bh.consume(si.deque.remove());
        }
        else {
            String s = si.deque.removeFirst();
            si.deque.add(s);
            bh.consume(si.canonicalStrings.get(s));
        }
    }
}
