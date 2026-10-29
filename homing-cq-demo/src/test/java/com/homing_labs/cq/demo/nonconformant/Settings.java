package com.homing_labs.cq.demo.nonconformant;

/**
 * Settings anyone can change from anywhere: public static fields that are not final.
 */
public final class Settings {

    public static String region = "sg";
    public static boolean verbose;

    private Settings() {
    }
}
