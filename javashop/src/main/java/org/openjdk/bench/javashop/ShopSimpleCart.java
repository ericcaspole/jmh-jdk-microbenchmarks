/*
 * Copyright (c) 2025 Oracle and/or its affiliates. All rights reserved.
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

package org.openjdk.bench.javashop;

import java.io.*;
import java.util.*;
import java.util.function.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.stream.*;
import java.util.concurrent.TimeUnit;
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
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.infra.ThreadParams;

@State(Scope.Benchmark)
public class ShopSimpleCart extends ShopBase {

  @Param({ ShopBase.defaultCatalogs })
  static int catalogs;

  @Param({ ShopBase.defaultItems })
  static int items;

  @Param({ShopBase.defaultOrderSize})
  static int orderSize;

  @Param({ "java.util.HashMap",
    "java.util.LinkedHashMap",
    "java.util.TreeMap",
    "java.util.WeakHashMap",
    "java.util.concurrent.ConcurrentHashMap"})
  static String map;

  @Param({"true" , "false" })
  static Boolean useIterators;

//  @Param({"true" , "false" })
//  static Boolean containsForCart;

  Function<Map<CatalogItem, Object>, Integer> browseI = x -> ShopBase.browseItemFieldsByIterator(x);
  Function<Map<CatalogItem, Object>, Integer> browseV = x -> ShopBase.browseItemFieldsByValues(x);

  Function<Map<CatalogItem, Object>, Integer> browseFunc;
  Function<Map<String, Map>, Integer> findFunc;
  
  Function<Map<String, Map>, Integer> hiPriceI = x -> ShopBase.findHighestPriceItemByIterator(x);
  Function<Map<String, Map>, Integer> hiPriceV = x -> ShopBase.findHighestPriceItemByValues(x);

  static Boolean itemInCartByContains(Map<String, Map> order, Map<CatalogItem, Object> browsedItem) {
      return order.values().contains(browsedItem);
  }

  static Boolean itemInCartByGet(Map<String, Map> order, Map<CatalogItem, Object> browsedItem) {
      String key = (String) browsedItem.get(CatalogItem.StockId);
      return order.get(key) != null;
  }

  BiFunction<Map<String, Map>, Map<CatalogItem, Object>, Boolean> cartC =
                        (x,y) -> itemInCartByContains(x,y);

  BiFunction<Map<String, Map>, Map<CatalogItem, Object>, Boolean> cartG =
                        (x,y) -> itemInCartByGet(x,y);

  BiFunction<Map<String, Map>, Map<CatalogItem, Object>, Boolean> cartFunc;

  @Setup(Level.Trial)
  public void setup() throws IOException,
          ClassNotFoundException, InstantiationException,
          IllegalAccessException {
      setupImpl( items,  catalogs, orderSize, map);

      browseFunc = useIterators ? browseI : browseV;
      cartFunc = useIterators ? cartC : cartG;
      findFunc = useIterators ?  hiPriceI : hiPriceV;
  }


  @Benchmark
  @BenchmarkMode(Mode.Throughput)
  @OutputTimeUnit(TimeUnit.SECONDS)
  @Warmup(iterations = 3, time = 5)
  @Measurement(iterations = 4, time = 5)
  @Fork(value = 3)
  @Threads(Threads.HALF_MAX)
  public Integer throughputMax(Blackhole bh) throws InterruptedException,
          ClassNotFoundException, InstantiationException,  IllegalAccessException {
    return shop(/* bh */);
  }

//  @Benchmark
//  @BenchmarkMode(Mode.AverageTime)
//  @OutputTimeUnit(TimeUnit.MICROSECONDS)
//  @Warmup(iterations = 3, time = 5)
//  @Measurement(iterations = 4, time = 5)
//  @Fork(value = 3)
//  @Threads(Threads.MAX)
//  public Integer latencyMax(Blackhole bh) throws InterruptedException,
//          ClassNotFoundException, InstantiationException,  IllegalAccessException {
//    return shop(/* bh */);
//  }

  @Benchmark
  @BenchmarkMode(Mode.Throughput)
  @OutputTimeUnit(TimeUnit.SECONDS)
  @Warmup(iterations = 3, time = 5)
  @Measurement(iterations = 4, time = 5)
  @Fork(value = 3)
  @Threads(1)
  public Integer throughputOne(Blackhole bh) throws InterruptedException,
          ClassNotFoundException, InstantiationException,  IllegalAccessException {
    return shop(/* bh */);
  }

//  @Benchmark
//  @BenchmarkMode(Mode.AverageTime)
//  @OutputTimeUnit(TimeUnit.MICROSECONDS)
//  @Warmup(iterations = 3, time = 4)
//  @Measurement(iterations = 5, time = 4)
//  @Fork(value = 2)
//  @Threads(1)
//  public Integer latencyOne(Blackhole bh) throws InterruptedException,
//          ClassNotFoundException, InstantiationException,  IllegalAccessException {
//    return shop(/* bh */);
//  }


  Map<CatalogItem, Object> getBrowsedItem(Map<String, Map> localCatalog, String key) {
      return localCatalog.get( key );
  }


    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    Map<String, Map> addItemToOrder(Map<String, Map> order,
                        int catalogIndex,
                        Map<CatalogItem, Object> browsedItem) {

        String key = (String) browsedItem.get(CatalogItem.StockId);

        if (cartFunc.apply(order, browsedItem) == false) {
            // Add the price to the item
            Integer price = (Integer) priceLists.get(catalogIndex).get(key);
            if ((Integer) browsedItem.get(CatalogItem.Price) == -1) {
                synchronized (browsedItem) {
                    browsedItem.put(CatalogItem.Price, price);
                }
            }
            assert (Integer) browsedItem.get(CatalogItem.Price) > 0 : "Price should be >0 now.";
            // Order more than 1!!
            ThreadLocalRandom tlr = ThreadLocalRandom.current();
            final int orderQuantity = tlr.nextInt(10) + 1;
            if (updateItemQuantity(catalogIndex, browsedItem, orderQuantity) == orderQuantity) {
                order.put((String) browsedItem.get(CatalogItem.StockId), browsedItem);
            }
        }
        return order;
    }

    Integer findHighestPriceItem(Map<String, Map> cartItems) {
        return findFunc.apply(cartItems);
    }

    public Integer shop(/* Blackhole bh */) throws /* InterruptedException, */
            ClassNotFoundException, InstantiationException, IllegalAccessException {
        ThreadLocalRandom tlr = ThreadLocalRandom.current();
        Map<String, Map> order = (Map) mapClass.newInstance();

        for (int i = 0; i < browse; i++) {
            // Get a random item from any catalog
            int catalogIndex = tlr.nextInt(catalogs);
            Map<String, Map> localCatalog = receiveCatalog(catalogIndex);
            String key = getItemId(localCatalog);
            Map<CatalogItem, Object> browsedItem = getBrowsedItem(localCatalog, key);

            assert browsedItem != null : "Should have an item for key:" + key + " from " + localCatalog;
            assert browsedItem.get(CatalogItem.Name) != null : " Should have a name";

            long browsed = browseFunc.apply(browsedItem);

            if (order.size() < orderSize && browsed >= 0) {
                order = addItemToOrder(order, catalogIndex, browsedItem);
            }
        }

        assert order.size() > 0 : "Order is " + order.size();

        return orderSummary(order) + findHighestPriceItem(order);
    }


}
