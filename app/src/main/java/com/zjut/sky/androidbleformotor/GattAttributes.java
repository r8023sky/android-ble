package com.zjut.sky.androidbleformotor;

import java.util.HashMap;

/**
 * Created by sky on 2016/11/21.
 */

public class GattAttributes {

    private static HashMap<String, String> attributes = new HashMap();
    //public static String HEART_RATE_MEASUREMENT = "0000C004-0000-1000-8000-00805f9b34fb";
    public static String CLIENT_CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805f9b34fb";
    public static String CHARACTERISTIC_MOTOR = "0000fff6-0000-1000-8000-00805f9b34fb";


    static {

        attributes.put("0000fff3-0000-1000-8000-00805f9b34fb", "Alarm");

        attributes.put("0000fff5-0000-1000-8000-00805f9b34fb", "Write Data");

        attributes.put("0000fff6-0000-1000-8000-00805f9b34fb", "Read Data");
        attributes.put("0000fff7-0000-1000-8000-00805f9b34fb", "Receive Data");
        attributes.put("0000fff8-0000-1000-8000-00805f9b34fb","Write Notify");
        attributes.put("0000fff9-0000-1000-8000-00805f9b34fb", "Message");

    }

    public static String lookup(String uuid, String defaultName) {
        String name = attributes.get(uuid);
        return name == null ? defaultName : name;
    }
}
