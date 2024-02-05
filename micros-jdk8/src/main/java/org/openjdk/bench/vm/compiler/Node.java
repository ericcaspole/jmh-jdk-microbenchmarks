/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.openjdk.bench.vm.compiler;

/**
 *
 * @author ecaspole
 */

public class Node {
  Node left;
//  Node right;

  int value;

  Node(int depth) {
    value = depth;
    if (depth > 0) {
      int d = depth - 1;
      left = new Node(d);
//      right = new Node(d);
    }
  }

  Node(Node n) {
    value = -1;
    left = n;
  }
}
