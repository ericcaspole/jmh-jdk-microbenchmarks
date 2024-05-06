/*
* Copyright (c) 2024, Oracle and/or its affiliates. All rights reserved.
* DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
*
* This code is free software; you can redistribute it and/or modify it
* under the terms of the GNU General Public License version 2 only, as
* published by the Free Software Foundation.
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
import java.util.concurrent.TimeUnit;

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

@State(Scope.Benchmark)
@Threads(1)
@Warmup(iterations = 4, time = 2)
@Measurement(iterations = 4, time = 2)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Fork(value = 2)
public class VolatileLoop {

    //    The purpose of this test is simply to show dense sequences of
    //    volatile instructions in the disassembly. Rnu with -prof perfasm

    volatile boolean bDone = false;

             long padAA = 0;
             long padAB = 0;
             long padAC = 0;
             long padAD = 0;
             long padAE = 0;
             long padAF = 0;
             long padAG = 0;
             long padAH = 0;
             long padAI = 0;
             long padAJ = 0;
             long padAK = 0;
             long padAL = 0;
             long padAM = 0;
             long padAN = 0;
             long padAO = 0;
             long padAP = 0;
             long padAQ = 0;
             long padAR = 0;
             long padAS = 0;
             long padAT = 0;

             long padBA = 0;
             long padBB = 0;
             long padBC = 0;
             long padBD = 0;
             long padBE = 0;
             long padBF = 0;
             long padBG = 0;
             long padBH = 0;
             long padBI = 0;
             long padBJ = 0;
             long padBK = 0;

             volatile int iDone = 0;

             long padBL = 0;
             long padBM = 0;
             long padBN = 0;
             long padBO = 0;
             long padBP = 0;
             long padBQ = 0;
             long padBR = 0;
             long padBS = 0;
             long padBT = 0;


             long padCA = 0;
             long padCB = 0;
             long padCC = 0;
             long padCD = 0;
             long padCE = 0;
             long padCF = 0;
             long padCG = 0;
             long padCH = 0;
             long padCI = 0;
             long padCJ = 0;
             long padCK = 0;
             long padCL = 0;
             long padCM = 0;
             long padCN = 0;
             long padCO = 0;
             long padCP = 0;
             long padCQ = 0;
             long padCR = 0;
             long padCS = 0;
             long padCT = 0;

    volatile long lDone = 0;

    @State(Scope.Thread)
    public static class ThreadStuff {
        public int a, b;

        @Setup(Level.Iteration)
        public void doSetup() {
            a = 0;
            b = 2;
        }
    }

    @Benchmark
    public void bDoit(ThreadStuff ts) {
        if (bDone == false) {
            ts.a ++;
        }
    }

    @Benchmark
    public void iDoit(ThreadStuff ts) {
        if (iDone == 0) {
            ts.a ++;
        }
    }

    @Benchmark
    public void lDoit(ThreadStuff ts) {
        if (lDone == 0) {
            ts.a ++;
        }
    }

    @Benchmark
    public void bSwap(ThreadStuff ts) {
        if (bDone == false) {
            ts.a ++;
        }
        bDone = ! bDone;
    }

    @Benchmark
    public void iSwap(ThreadStuff ts) {
        if (iDone == 0) {
            ts.a ++;
        }
        iDone = 1 - iDone;
    }

    @Benchmark
    public void lSwap(ThreadStuff ts) {
        if (lDone == 0) {
            ts.a ++;
        }
        lDone = 1 - lDone;
    }
}