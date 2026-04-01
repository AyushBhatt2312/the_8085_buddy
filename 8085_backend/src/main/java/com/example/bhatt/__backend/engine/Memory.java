package com.example.bhatt.__backend.engine;

/**
 * Simulates the 8085's 64 KB address space.
 * Uses int[] to avoid Java signed-byte issues — each cell holds 0–255.
 */
public class Memory {

    private static final int SIZE = 65536; // 64 KB
    private final int[] data;

    public Memory() {
        data = new int[SIZE];
    }

    /** Reads a byte from the given address (masked to 16 bits). */
    public int read(int address) {
        return data[address & 0xFFFF] & 0xFF;
    }

    /** Writes a byte to the given address (both address and value masked). */
    public void write(int address, int value) {
        data[address & 0xFFFF] = value & 0xFF;
    }

    /** Clears all memory to zero. */
    public void reset() {
        java.util.Arrays.fill(data, 0);
    }

    /** Returns the underlying array (for bulk state snapshot). */
    public int[] getRawData() {
        return data;
    }
}
