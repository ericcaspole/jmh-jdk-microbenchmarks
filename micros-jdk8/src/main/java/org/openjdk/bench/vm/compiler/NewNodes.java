/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.openjdk.bench.vm.compiler;

/**
 *
 * @author ecaspole
 */

import java.util.concurrent.ThreadLocalRandom;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OperationsPerInvocation;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Warmup;

/**
 * Benchmark measuring System.arraycopy in different ways.
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 8, time = 4)
@Measurement(iterations = 6, time = 3)

@State(Scope.Benchmark)
public class NewNodes {

    @Param({"6"})
    int depth;

    Node  root;
   
    @Setup
    public void setup() {
      root = null;
    }

    @Benchmark
    public void nodes0(Blackhole b) {
      Node n = null;
      for (int i=0; i<depth; i++ ) {
        n = new Node(i == 0 ? null : n);
      }
      b.consume(n);
    }   

    @Benchmark
    public Node nodes1() {
      Node n = null;
      for (int i=0; i<depth; i++ ) {
        n = new Node(i == 0 ? null : n);
      }
      return n;
    }   

    @Benchmark
    public Node nodes2() {
      Node n = null;
      for (int i=0; i<depth; i++ ) {
        n = new Node(i == 0 ? null : n);
      }
      root = n;
      return root;
    }   


    @Benchmark
    public Node nodes3() {
      for (int i=0; i<depth; i++ ) {
        root = new Node(i == 0 ? null : root);
      }
      return root;
    }   
}