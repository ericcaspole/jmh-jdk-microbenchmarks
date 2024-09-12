package org.openjdk.bench.vm.compiler;

import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.CompilerControl;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Group;
import org.openjdk.jmh.annotations.GroupThreads;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OperationsPerInvocation;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

@BenchmarkMode(Mode.Throughput)
@State(Scope.Benchmark)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 8, time = 4)
@Measurement(iterations = 6, time = 3)
public class DoubleDMB2 {

  class A {

    final String bz = new String("Hi there");
    final String bb = new String("Hi there to you too");
  }

  class C {

    private volatile A a;

    public A getA() {
      if (a == null) {
        a = new A();
      }
      return a;
    }
    
    void setA(A aa) {
      a = aa;
    }
  }

  static C c = null;

  @Setup
  public void setup() {
    c = new C();
  }

  @CompilerControl(CompilerControl.Mode.DONT_INLINE)
  void action(Blackhole b) throws Exception {
    
    A a = c.getA();

    b.consume( a.bb );
    b.consume(a.bz);
    
    c.setA(null);
  }

  @Benchmark
  @Fork(value = 1, jvmArgsAppend = {
    "-XX:+UnlockDiagnosticVMOptions", "-XX:+AlwaysMergeDMB"})
  public void plusAlwaysMergeDMB(Blackhole b) throws Exception {

    action(b);
  }

  @Benchmark
  @Fork(value = 1, jvmArgsAppend = {
    "-XX:+UnlockDiagnosticVMOptions", "-XX:-AlwaysMergeDMB"})
  public void minusAlwaysMergeDMB(Blackhole b) throws Exception {

    action(b);
  }

}
