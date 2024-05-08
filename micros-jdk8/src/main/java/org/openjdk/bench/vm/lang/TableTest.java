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

import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import jdk.jfr.internal.JVM;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

@State(Scope.Benchmark)
@Warmup(iterations = 5, time = 5)  // Need plenty of warmup
@Measurement(iterations = 5, time = 5)
@Fork(value = 2, jvmArgs = { "-Xlog:gc", 
        "--add-exports=jdk.jfr/jdk.jfr.internal=ALL-UNNAMED",
        "-Xmx8g",  "-Xms8g", "-XX:+UseLargePages",
        "-XX:+AlwaysPreTouch" })
public class TableTest {

    @Param({"115"})
    private int eventTypeId;

    @Param({"1000000"})
    private int numStrings;
    
    /*
    
    JMH to measure emitEvent for JFR. This can work for both StringTable and
    SymbolTable statistics by adjusting the eventId parameter. (However, the size
    of the string table can be set by adjusting the number of interned strings created
    during setup; there is no corresponding setup for the size of the symbol table).

    It is written as single method because the eventId parameter changes between JVM releases,
    so that value cannot be hardcoded.

    To run with JDK17+ include
    --jvmArgs "--add-exports jdk.jfr/jdk.jfr.internal=ALL-UNNAMED"
    in the JFR arguments.

    Also the interface for this changes in JDK 23, so to build for JDK 23 you
    need to change the source code as mentioned in the comments.

    Event ids change between JVM versions. Here are some values:

    StringTable statistics:
    JDK 8 EPP: 100
    JDK 17: 101
    JDK 21: 115
    JDK 23: 116 (NOTE: Requires code change)

    SymbolTable statistics:
    JDK 8 EPP: 99
    JDK 17: 100
    JDK 21: 114
    JDK 23: 115 (NOTE: Requires code change)

    Sample command line:
    ./jdk-17.0.4.1/bin/java -jar jmh-table-reduced.jar --jvmArgs '-XX:ReservedCodeCacheSize=1g -XX:-SegmentedCodeCache -Xmx8g -Xms8g -XX:+AlwaysPreTouch --add-exports jdk.jfr/jdk.jfr.internal=ALL-UNNAMED' --prof perfnorm -p eventTypeId=101

    Or run perf stat directly:
    perf stat ./jdk-17.0.4.1/bin/java -XX:ReservedCodeCacheSize=1g -XX:-SegmentedCodeCache -Xmx8g -Xms8g -XX:+AlwaysPreTouch --add-exports jdk.jfr/jdk.jfr.internal=ALL-UNNAMED -jar jmh-table-reduced.jar -p eventTypeId=101 -f 0

    */

    private JVM jvm;
    
    static final String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz1234567890";

    static String nextText(int minSize, int maxSize) {
        ThreadLocalRandom tlr = ThreadLocalRandom.current();

        String word = tlr.ints(0, alphabet.length()).limit(tlr.nextInt(minSize, maxSize)).boxed().
                map(x -> alphabet.charAt(x)).
                map(x -> x.toString()).
                collect(Collectors.joining());

        return word;
    }
    

    @Setup
    public void setup() throws ClassNotFoundException, InstantiationException, IllegalAccessException {
        jvm = JVM.getJVM();
        for (int i = 0; i < numStrings; i++) {
            nextText(80,512).intern();
        }
    }

    @Benchmark
    public void testTable(Blackhole bh) {
        boolean b = jvm.emitEvent(eventTypeId, JVM.counterTime(), 0);
        // For JDK23+, the getJVM() doesn't exist and instead emitEvent is static on the class.
        // We could have one version using reflection for both, but then we'd be measuring reflection invocation time
        // boolean b = JVM.emitEvent(eventTypeId, JVM.counterTime(), 0);
        bh.consume(b);
    }
}
