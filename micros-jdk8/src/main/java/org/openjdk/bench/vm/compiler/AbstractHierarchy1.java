/*
 * Copyright (c) 2021 Oracle and/or its affiliates. All rights reserved.
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

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
public class AbstractHierarchy1 {


    abstract class AbstractBase {
        abstract public int getLong();
        abstract public int getInt();
        abstract public int get1();
        abstract public int getNumber() ;
        abstract public int getOtherNumber() ;
    }

    abstract  class AbstractExtender1 extends AbstractBase {
         public int get1() {
            return 1;
        }
    }

    abstract class AbstractExtender3 extends AbstractExtender1 {
        public int getInt() {
            return 7;
        }
    }

    abstract class AbstractExtender4 extends AbstractExtender3 {
        public int getLong() {
            return 8;
        }
    }

    class Concrete1 extends AbstractExtender4  {
        public int getNumber() {
            return 7;
        }
        public int getOtherNumber() {
            return 7;
        }
    }

    class AltConcrete1 extends AbstractExtender4  {
        public int getNumber() {
            return 7;
        }
        public int getOtherNumber() {
            return 7;
        }
    }

    class ConcreteSub1 extends Concrete1 {
    }
    
    class ConcreteSub2 extends ConcreteSub1 {
      
    }
    
    class AltConcreteSub1 extends AltConcrete1 {

    }

    public AbstractBase  ab1, ab2, ab3;
    
    public AltConcreteSub1  ac1;
    
    public AbstractBase[] abarr = new AbstractBase[4];

  @Setup
  public void setupSubclass() {

    ab1 = new ConcreteSub2();
    ab2 = new ConcreteSub2();
    ab3 = new ConcreteSub1();
    ac1 = new AltConcreteSub1();

      // good
//    abarr[0] = ac1;
//    abarr[1] = ab1;
//    abarr[2] = ab2;
//    abarr[3] = ab3;


      // good
    abarr[0] = ac1;
    abarr[1] = ab3;
    abarr[2] = ab2;
    abarr[3] = ab1;

  }

    @Benchmark
    public long testSingleAbs1() {
      long total = 0;
      for (AbstractBase s : abarr) {
      total =+  s.getLong() + s.get1() + s.getInt() + s.getNumber() + s.getOtherNumber();
      }
      return total;
    }

}
