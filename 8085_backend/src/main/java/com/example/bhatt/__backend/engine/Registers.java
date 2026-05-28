package com.example.bhatt.__backend.engine;


public class Registers {

    // 8-bit general-purpose registers
    private int a; // Accumulator
    private int b;
    private int c;
    private int d;
    private int e;
    private int h;
    private int l;

    // 16-bit special registers
    private int pc; // Program Counter (0x0000–0xFFFF)
    private int sp; // Stack Pointer  (0x0000–0xFFFF)

    public Registers() {
        reset();
    }


    public void reset() {
        a = 0;
        b = 0;
        c = 0;
        d = 0;
        e = 0;
        h = 0;
        l = 0;
        pc = 0;
        sp = 0xFFFF;
    }

    // ── Register pair helpers ────────────────────────────────────────────

    public int getHL() {
        return ((h & 0xFF) << 8) | (l & 0xFF);
    }

    public int getBC() {
        return ((b & 0xFF) << 8) | (c & 0xFF);
    }

    public int getDE() {
        return ((d & 0xFF) << 8) | (e & 0xFF);
    }

    public void setHL(int value) {
        h = (value >> 8) & 0xFF;
        l = value & 0xFF;
    }

    public void setBC(int value) {
        b = (value >> 8) & 0xFF;
        c = value & 0xFF;
    }

    public void setDE(int value) {
        d = (value >> 8) & 0xFF;
        e = value & 0xFF;
    }

    // ── Generic register access by index (for MOV/MVI decode) ────────────

    public int getByCode(int code) {
        return switch (code) {
            case 0 -> b;
            case 1 -> c;
            case 2 -> d;
            case 3 -> e;
            case 4 -> h;
            case 5 -> l;
            case 7 -> a;
            default -> throw new IllegalArgumentException("Invalid register code: " + code);
        };
    }

    public void setByCode(int code, int value) {
        int masked = value & 0xFF;
        switch (code) {
            case 0 -> b = masked;
            case 1 -> c = masked;
            case 2 -> d = masked;
            case 3 -> e = masked;
            case 4 -> h = masked;
            case 5 -> l = masked;
            case 7 -> a = masked;
            default -> throw new IllegalArgumentException("Invalid register code: " + code);
        }
    }

    // ── Standard getters / setters ───────────────────────────────────────

    public int getA()  { return a; }
    public int getB()  { return b; }
    public int getC()  { return c; }
    public int getD()  { return d; }
    public int getE()  { return e; }
    public int getH()  { return h; }
    public int getL()  { return l; }
    public int getPc() { return pc; }
    public int getSp() { return sp; }

    public void setA(int v)  { a = v & 0xFF; }
    public void setB(int v)  { b = v & 0xFF; }
    public void setC(int v)  { c = v & 0xFF; }
    public void setD(int v)  { d = v & 0xFF; }
    public void setE(int v)  { e = v & 0xFF; }
    public void setH(int v)  { h = v & 0xFF; }
    public void setL(int v)  { l = v & 0xFF; }
    public void setPc(int v) { pc = v & 0xFFFF; }
    public void setSp(int v) { sp = v & 0xFFFF; }
}
