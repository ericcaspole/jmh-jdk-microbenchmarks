/*
 * Copyright (c) 2020 Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.bench.java.lang;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.infra.Blackhole;

import org.openjdk.bench.util.InMemoryJavaCompiler;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Thread)
public class DynamicLoading {
  static byte[][] compiledClasses;
  static int index = 0;
  static Object[] out;

  @Param({ "50" })
  public int numberOfClasses;

  @Param({ "10" })
  public int instances;

  static String B(int count) {
    return new String("public class B" + count + " {"
            + "   static int intField;"
            + "   public B" + count +"() {"
            + "       intField = -1;"
            + "   }"
            + "   public static void compiledMethod() { "
            + "       intField++;"
            + "   }"
            + "}");
  }

  static class BenchLoader extends ClassLoader {

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
      if (name.equals("B" + index)) {
        assert compiledClasses[index] != null;
        return defineClass(name, compiledClasses[index], 0, (compiledClasses[index]).length);
      } else {
        return super.findClass(name);
      }
    }
  }

  @Setup
  public void setupClasses() throws Exception {
    compiledClasses = new byte[numberOfClasses][];
    out = new Object[numberOfClasses];
    for (int i = 0; i < numberOfClasses; i++) {
      compiledClasses[i] = InMemoryJavaCompiler.compile("B" + i, B(i));
    }
  }

//  @Benchmark
//  public Object[] loadAndNewInstance(Blackhole bh) throws IllegalAccessException,
//          InstantiationException, NoSuchMethodException, InvocationTargetException, ClassNotFoundException {
//    
//    DynamicLoading.BenchLoader loader = new DynamicLoading.BenchLoader();
//
//    // Load and start all the classes.
//    for (index = 0; index < compiledClasses.length; index++) {
//      String name = new String("B" + index);
//      Class c = loader.findClass(name);
//      out[index] = c.newInstance();
//    }
//    return out;
//  }

  @Benchmark
  public Object[] loadAndSeveralNewInstance(Blackhole bh) throws IllegalAccessException,
          InstantiationException, NoSuchMethodException, InvocationTargetException, ClassNotFoundException {
    
    DynamicLoading.BenchLoader loader = new DynamicLoading.BenchLoader();

    // Load and start all the classes.
    for (index = 0; index < compiledClasses.length; index++) {
      String name = new String("B" + index);
      Class c = loader.findClass(name);
      for (int j=0; j<instances; j++) {
        bh.consume( c.newInstance() );
      }
    }
    return out;
  }

}