package org.sutormin.nanocraft;

public class Main {
  public static void main(String[] args) {
    System.out.println("NanoCraft 1.0.0; LWJGL " + org.lwjgl.Version.getVersion());
    new NanoCraft().run();
  }
}