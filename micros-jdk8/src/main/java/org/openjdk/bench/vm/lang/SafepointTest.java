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

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

@State(Scope.Benchmark)
@Warmup(iterations = 5, time = 5)
@Measurement(iterations = 5, time = 5)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Fork(value = 2)
public class SafepointTest {
    @Param({"50"})
    private int nThreads;

    @Param({"true","false"})
    private boolean withThreadInspection;
    
    @Param({"1000"})
    private long inspectionDelay;

    @Param({"60"})
    private long consumeCycles;
    
    /*
    This JMH measures the effect of periodically inducing ThreadDump
    safepoints during a CPU-intensive operation.
    
    When the parameter introspectionDelay is true, the setup will create
    nThreads number of threads and periodically (every inspectionDelay ms)
    use the ThreadMXBean interface to get all the stacks of those threads.
    
    The measurement is of a particular CPU-intensive operation. We can see
    the practical effect of calling the thread dump so frequently by seeing
    the difference in the testPI method when introspectionDelay is true oZ false.
    */

    private long[] tids;
    ThreadMXBean bean = ManagementFactory.getThreadMXBean();

    @Setup
    public void setup() throws IOException {
        if (withThreadInspection) {
            tids = new long[nThreads];
            for (int i = 0; i < nThreads; i++) {
                Thread t = new Thread(() -> {
                        while (true) try { Thread.sleep(10000); } catch (Exception e) {}
                });
                t.setDaemon(true);
                tids[i] = t.getId();
                t.start();
            }
            Thread t = new Thread(() -> {
                while (true) {
                    try { 
                        for (int i = 0; i < tids.length; i++) bean.getThreadInfo(tids[i], 100);
                        Thread.sleep(inspectionDelay);
                    } catch (Exception e) {} 
                }
            });
            t.setDaemon(true);
            t.start();
        }
    }

    @Benchmark
    public void testConsume(Blackhole bh) {
        Blackhole.consumeCPU(consumeCycles);
    }
}
