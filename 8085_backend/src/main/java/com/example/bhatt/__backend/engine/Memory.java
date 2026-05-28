package com.example.bhatt.__backend.engine;

public class Memory {

    private static final int SIZE = 65536; // 64 KB
    private final int[] data;

    public Memory() {
        data = new int[SIZE];
    }

    public int read(int address) {
        return data[address & 0xFFFF] & 0xFF;
    }


    public void write(int address, int value) {
        data[address & 0xFFFF] = value & 0xFF;
    }


    public void reset() {
        java.util.Arrays.fill(data, 0);
    }


    public int[] getRawData() {
        return data;
    }
}
