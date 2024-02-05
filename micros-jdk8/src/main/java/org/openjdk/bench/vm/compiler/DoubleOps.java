
/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.openjdk.bench.vm.compiler;

/**
 *
 * @author ecaspole
 */

import java.util.concurrent.atomic.AtomicInteger;
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
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.DoubleSupplier;
import java.util.stream.DoubleStream;
import java.util.stream.Collectors;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 4, time = 4)
@Measurement(iterations = 6, time = 3)
@Fork(value = 2)
@State(Scope.Benchmark)
public class DoubleOps {

    @Param({"250"})
    int size;
    
    List<Double>  list = new ArrayList<>();
    
    volatile int j = 0;
    DoubleStream ds = null;
    
    long start, end;
    double diff;
   
      DoubleSupplier d = ()  -> { 
        diff = (diff > 2.0 ? diff - 0.0091 : diff + diff + 0.0083);
        
//        System.out.println(diff);
        
        return diff;
      };

    @Setup
    public void setup() {
      start = System.currentTimeMillis();
              try {
          Thread.sleep(387);
        } catch(Exception e) {}
       end = System.currentTimeMillis();
       
       diff = (double) ((double)end / (double) start);

    }

    @Benchmark
    public OptionalDouble average(Blackhole b) {
      return (DoubleStream.generate( d )).limit(size).average();
    }   

    @Benchmark
    public OptionalDouble max(Blackhole b) {
      return (DoubleStream.generate( d )).limit(size).max();
    }   

    @Benchmark
    public OptionalDouble min(Blackhole b) {
      return (DoubleStream.generate( d )).limit(size).min();
    }   

    @Benchmark
    public DoubleStream sorted(Blackhole b) {
      return (DoubleStream.generate( d )).limit(size).sorted();
    }   

    @Benchmark
    public DoubleStream distinct(Blackhole b) {
      return (DoubleStream.generate( d )).limit(size).distinct();
    }   

    @Benchmark
    public DoubleSummaryStatistics summaryStatistics(Blackhole b) {
      return (DoubleStream.generate( d )).limit(size).summaryStatistics();
    }   

}