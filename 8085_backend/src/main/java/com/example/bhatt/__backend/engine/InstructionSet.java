package com.example.bhatt.__backend.engine;

import java.util.HashMap;
import java.util.Map;

/**
 * Provides opcode-to-mnemonic mappings for the complete 8085 instruction set.
 * Used for disassembly/display purposes.
 */
public class InstructionSet {

    private static final Map<Integer, String> OPCODE_MAP = new HashMap<>();

    static {
        // ── NOP / HLT ────────────────────────────────────────────────────
        OPCODE_MAP.put(0x00, "NOP");
        OPCODE_MAP.put(0x76, "HLT");

        String[] regNames = {"B", "C", "D", "E", "H", "L", "M", "A"};
        String[] rpNames  = {"B", "D", "H", "SP"};

        // ── MVI r, data ──────────────────────────────────────────────────
        for (int r = 0; r < 8; r++) {
            OPCODE_MAP.put(0x06 | (r << 3), "MVI " + regNames[r]);
        }

        // ── MOV dst, src (01 DDD SSS, excluding 0x76=HLT) ──────────────
        for (int dst = 0; dst < 8; dst++) {
            for (int src = 0; src < 8; src++) {
                int opcode = 0x40 | (dst << 3) | src;
                if (opcode == 0x76) continue;
                OPCODE_MAP.put(opcode, "MOV " + regNames[dst] + "," + regNames[src]);
            }
        }

        // ── LXI rp, data16 ──────────────────────────────────────────────
        for (int rp = 0; rp < 4; rp++) {
            OPCODE_MAP.put(0x01 | (rp << 4), "LXI " + rpNames[rp]);
        }

        // ── Direct addressing ────────────────────────────────────────────
        OPCODE_MAP.put(0x3A, "LDA");
        OPCODE_MAP.put(0x32, "STA");
        OPCODE_MAP.put(0x2A, "LHLD");
        OPCODE_MAP.put(0x22, "SHLD");

        // ── Indirect load/store ──────────────────────────────────────────
        OPCODE_MAP.put(0x0A, "LDAX B");
        OPCODE_MAP.put(0x1A, "LDAX D");
        OPCODE_MAP.put(0x02, "STAX B");
        OPCODE_MAP.put(0x12, "STAX D");

        // ── Exchange / SP / PC ───────────────────────────────────────────
        OPCODE_MAP.put(0xEB, "XCHG");
        OPCODE_MAP.put(0xE3, "XTHL");
        OPCODE_MAP.put(0xF9, "SPHL");
        OPCODE_MAP.put(0xE9, "PCHL");

        // ── ADD / ADC / SUB / SBB / ANA / XRA / ORA / CMP r ────────────
        String[] aluNames = {"ADD", "ADC", "SUB", "SBB", "ANA", "XRA", "ORA", "CMP"};
        for (int op = 0; op < 8; op++) {
            for (int r = 0; r < 8; r++) {
                OPCODE_MAP.put(0x80 | (op << 3) | r, aluNames[op] + " " + regNames[r]);
            }
        }

        // ── Immediate ALU ────────────────────────────────────────────────
        OPCODE_MAP.put(0xC6, "ADI");
        OPCODE_MAP.put(0xCE, "ACI");
        OPCODE_MAP.put(0xD6, "SUI");
        OPCODE_MAP.put(0xDE, "SBI");
        OPCODE_MAP.put(0xE6, "ANI");
        OPCODE_MAP.put(0xEE, "XRI");
        OPCODE_MAP.put(0xF6, "ORI");
        OPCODE_MAP.put(0xFE, "CPI");

        // ── INR / DCR r ─────────────────────────────────────────────────
        for (int r = 0; r < 8; r++) {
            OPCODE_MAP.put(0x04 | (r << 3), "INR " + regNames[r]);
            OPCODE_MAP.put(0x05 | (r << 3), "DCR " + regNames[r]);
        }

        // ── INX / DCX / DAD rp ──────────────────────────────────────────
        for (int rp = 0; rp < 4; rp++) {
            OPCODE_MAP.put(0x03 | (rp << 4), "INX " + rpNames[rp]);
            OPCODE_MAP.put(0x0B | (rp << 4), "DCX " + rpNames[rp]);
            OPCODE_MAP.put(0x09 | (rp << 4), "DAD " + rpNames[rp]);
        }

        // ── Accumulator / flag ops ──────────────────────────────────────
        OPCODE_MAP.put(0x2F, "CMA");
        OPCODE_MAP.put(0x27, "DAA");
        OPCODE_MAP.put(0x37, "STC");
        OPCODE_MAP.put(0x3F, "CMC");

        // ── Rotate ──────────────────────────────────────────────────────
        OPCODE_MAP.put(0x07, "RLC");
        OPCODE_MAP.put(0x0F, "RRC");
        OPCODE_MAP.put(0x17, "RAL");
        OPCODE_MAP.put(0x1F, "RAR");

        // ── Stack: PUSH / POP ───────────────────────────────────────────
        String[] pushPopNames = {"B", "D", "H", "PSW"};
        for (int rp = 0; rp < 4; rp++) {
            OPCODE_MAP.put(0xC5 | (rp << 4), "PUSH " + pushPopNames[rp]);
            OPCODE_MAP.put(0xC1 | (rp << 4), "POP " + pushPopNames[rp]);
        }

        // ── Branch ──────────────────────────────────────────────────────
        OPCODE_MAP.put(0xC3, "JMP");
        OPCODE_MAP.put(0xCD, "CALL");
        OPCODE_MAP.put(0xC9, "RET");

        String[] condNames = {"NZ", "Z", "NC", "C", "PO", "PE", "P", "M"};
        for (int cc = 0; cc < 8; cc++) {
            OPCODE_MAP.put(0xC2 | (cc << 3), "J" + condNames[cc]);   // Jcc
            OPCODE_MAP.put(0xC4 | (cc << 3), "C" + condNames[cc]);   // Ccc
            OPCODE_MAP.put(0xC0 | (cc << 3), "R" + condNames[cc]);   // Rcc
        }

        // ── RST n ───────────────────────────────────────────────────────
        for (int n = 0; n < 8; n++) {
            OPCODE_MAP.put(0xC7 | (n << 3), "RST " + n);
        }

        // ── I/O & Interrupts ────────────────────────────────────────────
        OPCODE_MAP.put(0xDB, "IN");
        OPCODE_MAP.put(0xD3, "OUT");
        OPCODE_MAP.put(0xFB, "EI");
        OPCODE_MAP.put(0xF3, "DI");
        OPCODE_MAP.put(0x20, "RIM");
        OPCODE_MAP.put(0x30, "SIM");
    }

    /** Returns the mnemonic for a given opcode, or "???" if unknown. */
    public static String getMnemonic(int opcode) {
        return OPCODE_MAP.getOrDefault(opcode & 0xFF, "???");
    }

    /** Checks whether a given opcode is in the implemented set. */
    public static boolean isImplemented(int opcode) {
        return OPCODE_MAP.containsKey(opcode & 0xFF);
    }
}
