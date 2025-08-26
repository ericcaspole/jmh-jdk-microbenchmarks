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

public abstract class ShopBase {

    final static String defaultOrderSize = "250";
    final static String defaultItems = "1500"; // 250"; // "200";
    final static String defaultCatalogs =  "400"; // 250"; // "200";

  enum CatalogItem {
    StockId("Stock Id"),
    Name("Name"),
    Description("Description"),
    Brand("Brand"),
    Size("Size"),
    Color("Color"),
    Weight("Weight"),
    ShipsFrom("ShipsFrom"),
    MadeIn("MadeIn"),
    Language("Language"),
    Warranty("Warranty"),
    WaterResistance("WaterResistance"),
    ChargerType("ChargerType"),
    Price("Price");

    String name;

    CatalogItem(String s) {
      this.name = s;
    }

    public String getName() {
      return name;
    }

  }

  static String nextText(int size) {
    ThreadLocalRandom tlr = ThreadLocalRandom.current();

    String word = tlr.ints(97, 123).limit(size).boxed().
            map(x -> x.toString()).
            map(x -> (new Character((char) Integer.parseInt(x))).toString()).
            collect(Collectors.joining());

    return word;
  }

  final int nameLength = 64;
  final int descLength = 256;

  static int browse;

  Map<Integer,Map> masterCatalogs;
  Map<Integer,Map> warehouseQuantities;
  Map<Integer,Map> priceLists;
  Class mapClass;
  List<String>    itemIds = new ArrayList<>();


  void setupImpl(int items, int catalogs, /* int browse, */ int orderSize, String map) throws IOException,
          ClassNotFoundException, InstantiationException,  IllegalAccessException {
    mapClass = Class.forName(map);

    warehouseQuantities = new HashMap();
    masterCatalogs = new HashMap();
    priceLists = new HashMap();
    Map<String, AtomicInteger> currWarehouseQuantities = new HashMap();
    Map<String, Integer> currPriceList = (Map) mapClass.newInstance();

    // Used a fixed size of order for easier comparisons of profiles
    browse = orderSize * 4;
    assert browse >= orderSize : "browse too low";
    ThreadLocalRandom tlr = ThreadLocalRandom.current();

    for (int j = 0; j < catalogs; j++) {
      Map<String, Map> currCatalog = (Map) mapClass.newInstance();
      for (int i = 0; i < items; i++) {
        Map<CatalogItem, Object> catalogItem = (Map) mapClass.newInstance();
        String name = nextText(tlr.nextInt(10, nameLength));
        String id = Integer.toString(i) + mapClass.getName();
        if ( !itemIds.contains(id) ) {
          itemIds.add(id);
        }
        catalogItem.put(CatalogItem.StockId, id);
        catalogItem.put(CatalogItem.Name, name);
        catalogItem.put(CatalogItem.Description, nextText(tlr.nextInt(5, descLength)));
        // Price is updated later
        catalogItem.put(CatalogItem.Price, new Integer(-1));
        catalogItem.put(CatalogItem.Color, nextText(tlr.nextInt(5, nameLength)));
        catalogItem.put(CatalogItem.Size, nextText(tlr.nextInt(5, nameLength)));
        catalogItem.put(CatalogItem.Weight, (Integer) tlr.nextInt(descLength) + 1);
        catalogItem.put(CatalogItem.Brand, nextText(tlr.nextInt(5, nameLength)));

        assert  currCatalog.get(id) == null : "conflict";
        currCatalog.put(id, catalogItem);

        currPriceList.put(id, tlr.nextInt(descLength) + 1);
        currWarehouseQuantities.put(id, new AtomicInteger(0));
      }

      priceLists.put(j, currPriceList);
      masterCatalogs.put(j, currCatalog);
      warehouseQuantities.put(j, currWarehouseQuantities);
    }

    // Try to settle down the footprint before the test starts
    System.gc();

  }

  static String getItemId(Map<String, Map> localCatalog) {
    ThreadLocalRandom tlr = ThreadLocalRandom.current();
    int i = tlr.nextInt(localCatalog.size());
    return Integer.toString(i) + localCatalog.getClass().getName();
  }

  int updateItemQuantity(int localCatalog, Map browsedItem, int orderQuantity) {
    boolean success = false;
    Map<String, AtomicInteger> currWarehouseQuantities = warehouseQuantities.get(localCatalog);
    assert currWarehouseQuantities != null : "currWarehouseQuantities is null for " + localCatalog;
    AtomicInteger itemQuantity = currWarehouseQuantities.get((String) browsedItem.get(CatalogItem.StockId));
    assert itemQuantity != null;

    int origQ = itemQuantity.get();
    int expectedNewQ = 0;
    while (!success && origQ >= 0) {
      expectedNewQ = origQ + orderQuantity;
      success = itemQuantity.compareAndSet(origQ, expectedNewQ);
      if (success) {
        // ok and uncontended
      } else {
        // contended
        origQ = itemQuantity.get();
      }
    }

    if (success) {
      assert expectedNewQ >= 0 : "Should not be " + expectedNewQ;
      return orderQuantity;
    } else {
      return 0;
    }

      // return orderQuantity;
  }

    long browseItemFields(Map<CatalogItem, Object> browsedItem) {

        int z = 0;
        Object[] fields;
        synchronized (browsedItem) {
            fields = browsedItem.values().toArray(new Object[0]);
        }

        for (int i = 0; i < fields.length; i++) {
            if (fields[i] instanceof java.lang.String) {
                String field = (String) fields[i];
                if (field.contains("z")) {
                    z++;
                }
            }
        }
        return z;
    }

  Map<String, Map> receiveCatalog(int which) {
//    assert masterCatalogs.get(which).entrySet().size() == items : which + " has size " + masterCatalogs.get(which).entrySet().size() ;
    return masterCatalogs.get(which);
  }

  Integer findHighestPriceItem(Map<String, Map> cartItems) {
    java.util.Map[] items = cartItems.values().toArray(new Map[0]);
    int max = 0;

    for (int i = 0; i < items.length; i++) {
      Map order = (Map) items[i];
      int price = (Integer) order.get(CatalogItem.Price);
      if ( price > max  ) {
        max = price;
      }
    }

    return max;
  }

  int orderSummary(Map<String, Map> order) {
          int orderShippingWeight = 0;
    int orderTotalPrice = 0;

    for(Map orderItem : order.values()) {

      int w = (Integer) orderItem.get(CatalogItem.Weight);
      assert w > 0 : "Weight should not be 0";
      orderShippingWeight += w;
      int p = (Integer) orderItem.get(CatalogItem.Price);
      assert p > 0 : "Price should not be 0";
      orderTotalPrice += p;
    }

    assert orderTotalPrice > 0;
    assert orderShippingWeight > 0;
    return orderTotalPrice + orderShippingWeight; // + findHighestPriceItem(order);

  }

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    static Integer browseItemFieldsByValues(Map<CatalogItem, Object> browsedItem) {
        int z = 0;

        assert browsedItem.size() != 0;
        assert browsedItem.values() != null;
        final Object[] fields  = browsedItem.values().toArray(new Object[0]);
        for (int i = 0; i < fields.length; i++) {
            if (fields[i] instanceof java.lang.String) {
                String field = (String) fields[i];
                if (field.contains("z")) {
                    z++;
                }
            }
        }
        return z;
    }

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    static Integer browseItemFieldsByIterator(Map<CatalogItem, Object> browsedItem) {
        int z = 0;

        Iterator vi = browsedItem.values().iterator();
        while (vi.hasNext()) {
            Object e = vi.next();
            if (e instanceof java.lang.String) {
                if (((String)e).contains("z")) {
                    z++;
                }
            }
        }
        return z;
    }

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    static Integer  findHighestPriceItemByIterator(Map<String, Map> cartItems) {
        int max = 0;
        Iterator vi = cartItems.values().iterator();
        while (vi.hasNext()) {
            Map order = (Map) vi.next();
            int price = (Integer) order.get(CatalogItem.Price);
            if (price > max) {
                max = price;
            }
        }

        assert max > 0 : "max price should be > 0";
        return max;
    }

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    static Integer findHighestPriceItemByValues(Map<String, Map> cartItems) {
        int max = 0;
        java.util.Map[] items = cartItems.values().toArray(new Map[0]);
        for (int i = 0; i < items.length; i++) {
            Map order = (Map) items[i];
            int price = (Integer) order.get(CatalogItem.Price);
            if (price > max) {
                max = price;
            }
        }

        assert max > 0 : "max price should be > 0";
        return max;
    }
    
  Map<CatalogItem, Object> createItem(Class mapClass, int i) throws IOException,
          ClassNotFoundException,
          InstantiationException,  IllegalAccessException {
    ThreadLocalRandom tlr = ThreadLocalRandom.current();

      Map<CatalogItem, Object> catalogItem = (Map) mapClass.newInstance();
        String name = nextText(tlr.nextInt(10, nameLength));
        String id = Integer.toString(i);
        catalogItem.put(CatalogItem.StockId, id);
        catalogItem.put(CatalogItem.Name, name);
        catalogItem.put(CatalogItem.Description, nextText(tlr.nextInt(5, descLength)));
        // Price is updated later
        catalogItem.put(CatalogItem.Price, new Integer(-1));
        catalogItem.put(CatalogItem.Color, nextText(tlr.nextInt(5, nameLength)));
        catalogItem.put(CatalogItem.Size, nextText(tlr.nextInt(5, nameLength)));
        catalogItem.put(CatalogItem.Weight, (Integer) tlr.nextInt(descLength) + 1);
        catalogItem.put(CatalogItem.Brand, nextText(tlr.nextInt(5, nameLength)));

        return catalogItem;
  }

}
