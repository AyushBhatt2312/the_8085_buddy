package com.example.bhatt.__backend.engine;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the complete 8085 instruction set implementation.
 * Each test loads opcodes into memory, steps through, and asserts
 * register/flag values match the expected 8085 behavior.
 */
class CpuCoreInstructionTest {

    private CpuCore cpu;

    @BeforeEach
    void setUp() {
        cpu = new CpuCore();
    }

    private void loadAndRun(int... bytes) {
        for (int i = 0; i < bytes.length; i++) {
            cpu.getMemory().write(i, bytes[i]);
        }
        // Step until HLT or timeout
        for (int i = 0; i < 100 && !cpu.isHalted(); i++) {
            cpu.step();
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  User program 1: MVI A,0AH / INR A / HLT
    //  Expected: A = 0x0B
    // ═══════════════════════════════════════════════════════════════════
    @Test
    void testProgram1_MVI_INR() {
        loadAndRun(0x3E, 0x0A,  // MVI A, 0AH
                   0x3C,        // INR A
                   0x76);       // HLT
        assertEquals(0x0B, cpu.getRegisters().getA());
        assertFalse(cpu.getFlags().isZero());
        assertFalse(cpu.getFlags().isSign());
    }

    // ═══════════════════════════════════════════════════════════════════
    //  User program 2: MVI A,0AH / DCR A / HLT
    //  Expected: A = 0x09
    // ═══════════════════════════════════════════════════════════════════
    @Test
    void testProgram2_MVI_DCR() {
        loadAndRun(0x3E, 0x0A,  // MVI A, 0AH
                   0x3D,        // DCR A
                   0x76);       // HLT
        assertEquals(0x09, cpu.getRegisters().getA());
        assertFalse(cpu.getFlags().isZero());
        assertFalse(cpu.getFlags().isSign());
    }

    // ═══════════════════════════════════════════════════════════════════
    //  User program 3: LDA 2000H / ADI 05H / HLT
    //  Preload memory[0x2000] = 0x10
    //  Expected: A = 0x15
    // ═══════════════════════════════════════════════════════════════════
    @Test
    void testProgram3_LDA_ADI() {
        cpu.getMemory().write(0x2000, 0x10);
        loadAndRun(0x3A, 0x00, 0x20,  // LDA 2000H
                   0xC6, 0x05,         // ADI 05H
                   0x76);              // HLT
        assertEquals(0x15, cpu.getRegisters().getA());
    }

    // ═══════════════════════════════════════════════════════════════════
    //  User program 4: MVI A,2FH / STA 3000H / HLT
    //  Expected: memory[0x3000] = 0x2F
    // ═══════════════════════════════════════════════════════════════════
    @Test
    void testProgram4_MVI_STA() {
        loadAndRun(0x3E, 0x2F,        // MVI A, 2FH
                   0x32, 0x00, 0x30,  // STA 3000H
                   0x76);             // HLT
        assertEquals(0x2F, cpu.getMemory().read(0x3000));
    }

    // ═══════════════════════════════════════════════════════════════════
    //  User program 5: LDA 2000H / STA 3000H / HLT
    //  Preload memory[0x2000] = 0xAB
    //  Expected: memory[0x3000] = 0xAB
    // ═══════════════════════════════════════════════════════════════════
    @Test
    void testProgram5_LDA_STA() {
        cpu.getMemory().write(0x2000, 0xAB);
        loadAndRun(0x3A, 0x00, 0x20,  // LDA 2000H
                   0x32, 0x00, 0x30,  // STA 3000H
                   0x76);             // HLT
        assertEquals(0xAB, cpu.getMemory().read(0x3000));
    }

    // ═══════════════════════════════════════════════════════════════════
    //  User program 6: MVI H,12H / MVI L,34H / XCHG / HLT
    //  Expected: D = 0x12, E = 0x34, H = 0x00, L = 0x00
    // ═══════════════════════════════════════════════════════════════════
    @Test
    void testProgram6_XCHG() {
        loadAndRun(0x26, 0x12,  // MVI H, 12H
                   0x2E, 0x34,  // MVI L, 34H
                   0xEB,        // XCHG
                   0x76);       // HLT
        assertEquals(0x12, cpu.getRegisters().getD());
        assertEquals(0x34, cpu.getRegisters().getE());
        assertEquals(0x00, cpu.getRegisters().getH());
        assertEquals(0x00, cpu.getRegisters().getL());
    }

    // ═══════════════════════════════════════════════════════════════════
    //  User program 7: MVI A,77H / MVI B,55H / ANA B / HLT
    //  A & B = 0x77 & 0x55 = 0x55
    // ═══════════════════════════════════════════════════════════════════
    @Test
    void testProgram7_ANA() {
        loadAndRun(0x3E, 0x77,  // MVI A, 77H
                   0x06, 0x55,  // MVI B, 55H
                   0xA0,        // ANA B
                   0x76);       // HLT
        assertEquals(0x55, cpu.getRegisters().getA());
        assertFalse(cpu.getFlags().isCarry());  // CY cleared by ANA
    }

    // ═══════════════════════════════════════════════════════════════════
    //  User program 8: MVI A,44H / MVI B,22H / ORA B / HLT
    //  A | B = 0x44 | 0x22 = 0x66
    // ═══════════════════════════════════════════════════════════════════
    @Test
    void testProgram8_ORA() {
        loadAndRun(0x3E, 0x44,  // MVI A, 44H
                   0x06, 0x22,  // MVI B, 22H
                   0xB0,        // ORA B
                   0x76);       // HLT
        assertEquals(0x66, cpu.getRegisters().getA());
        assertFalse(cpu.getFlags().isCarry());
    }

    // ═══════════════════════════════════════════════════════════════════
    //  User program 9: MVI A,0FH / MVI B,05H / XRA B / HLT
    //  A ^ B = 0x0F ^ 0x05 = 0x0A
    // ═══════════════════════════════════════════════════════════════════
    @Test
    void testProgram9_XRA() {
        loadAndRun(0x3E, 0x0F,  // MVI A, 0FH
                   0x06, 0x05,  // MVI B, 05H
                   0xA8,        // XRA B
                   0x76);       // HLT
        assertEquals(0x0A, cpu.getRegisters().getA());
        assertFalse(cpu.getFlags().isCarry());
    }

    // ═══════════════════════════════════════════════════════════════════
    //  User program 10: MVI A,55H / CMA / HLT
    //  ~0x55 = 0xAA
    // ═══════════════════════════════════════════════════════════════════
    @Test
    void testProgram10_CMA() {
        loadAndRun(0x3E, 0x55,  // MVI A, 55H
                   0x2F,        // CMA
                   0x76);       // HLT
        assertEquals(0xAA, cpu.getRegisters().getA());
    }

    // ═══════════════════════════════════════════════════════════════════
    //  User program 11: MVI A,81H / RLC / HLT
    //  0x81 = 10000001 → rotate left → 00000011 = 0x03, CY = 1
    // ═══════════════════════════════════════════════════════════════════
    @Test
    void testProgram11_RLC() {
        loadAndRun(0x3E, 0x81,  // MVI A, 81H
                   0x07,        // RLC
                   0x76);       // HLT
        assertEquals(0x03, cpu.getRegisters().getA());
        assertTrue(cpu.getFlags().isCarry());
    }

    // ═══════════════════════════════════════════════════════════════════
    //  User program 12: LXI SP,FFFFH / HLT
    //  Expected: SP = 0xFFFF
    // ═══════════════════════════════════════════════════════════════════
    @Test
    void testProgram12_LXI_SP() {
        loadAndRun(0x31, 0xFF, 0xFF,  // LXI SP, FFFFH
                   0x76);              // HLT
        assertEquals(0xFFFF, cpu.getRegisters().getSp());
    }

    // ═══════════════════════════════════════════════════════════════════
    //  Flag edge cases
    // ═══════════════════════════════════════════════════════════════════

    @Test
    void testZeroFlag_INR() {
        // INR from 0xFF → 0x00 → Zero flag set
        loadAndRun(0x3E, 0xFF,  // MVI A, FFH
                   0x3C,        // INR A
                   0x76);       // HLT
        assertEquals(0x00, cpu.getRegisters().getA());
        assertTrue(cpu.getFlags().isZero());
    }

    @Test
    void testSignFlag_DCR() {
        // DCR from 0x00 → 0xFF → Sign flag set
        loadAndRun(0x3E, 0x00,  // MVI A, 00H
                   0x3D,        // DCR A
                   0x76);       // HLT
        assertEquals(0xFF, cpu.getRegisters().getA());
        assertTrue(cpu.getFlags().isSign());
    }

    @Test
    void testCarryFlag_ADD() {
        // 0xFF + 0x01 = 0x100 → CY set, A = 0x00
        loadAndRun(0x3E, 0xFF,  // MVI A, FFH
                   0x06, 0x01,  // MVI B, 01H
                   0x80,        // ADD B
                   0x76);       // HLT
        assertEquals(0x00, cpu.getRegisters().getA());
        assertTrue(cpu.getFlags().isCarry());
        assertTrue(cpu.getFlags().isZero());
    }

    @Test
    void testParityFlag_ANA() {
        // 0xFF & 0xFF = 0xFF → 8 ones → even parity → P set
        loadAndRun(0x3E, 0xFF,  // MVI A, FFH
                   0x06, 0xFF,  // MVI B, FFH
                   0xA0,        // ANA B
                   0x76);       // HLT
        assertEquals(0xFF, cpu.getRegisters().getA());
        assertTrue(cpu.getFlags().isParity());
    }

    @Test
    void testRRC() {
        // 0x81 = 10000001 → RRC → 11000000 = 0xC0, CY = 1
        loadAndRun(0x3E, 0x81,  // MVI A, 81H
                   0x0F,        // RRC
                   0x76);       // HLT
        assertEquals(0xC0, cpu.getRegisters().getA());
        assertTrue(cpu.getFlags().isCarry());
    }

    @Test
    void testSUB_borrow() {
        // A=0x05, B=0x0A → SUB B → A=0xFB, CY=1 (borrow)
        loadAndRun(0x3E, 0x05,  // MVI A, 05H
                   0x06, 0x0A,  // MVI B, 0AH
                   0x90,        // SUB B
                   0x76);       // HLT
        assertEquals(0xFB, cpu.getRegisters().getA());
        assertTrue(cpu.getFlags().isCarry());
        assertTrue(cpu.getFlags().isSign());
    }

    @Test
    void testLXI_BC() {
        loadAndRun(0x01, 0x34, 0x12,  // LXI B, 1234H
                   0x76);              // HLT
        assertEquals(0x12, cpu.getRegisters().getB());
        assertEquals(0x34, cpu.getRegisters().getC());
    }

    @Test
    void testPUSH_POP() {
        loadAndRun(0x3E, 0xAB,        // MVI A, ABH
                   0x06, 0xCD,        // MVI B, CDH
                   0x31, 0x00, 0x40,  // LXI SP, 4000H
                   0xC5,              // PUSH B  (pushes B=0xCD, C=0x00)
                   0x06, 0x00,        // MVI B, 00H  (clear B)
                   0x0E, 0x00,        // MVI C, 00H  (clear C)
                   0xC1,              // POP B  (restores B & C)
                   0x76);             // HLT
        assertEquals(0xCD, cpu.getRegisters().getB());
        assertEquals(0x00, cpu.getRegisters().getC());
    }

    @Test
    void testDAD() {
        // HL = 1234H, BC = 1111H → DAD B → HL = 2345H
        loadAndRun(0x21, 0x34, 0x12,  // LXI H, 1234H
                   0x01, 0x11, 0x11,  // LXI B, 1111H
                   0x09,              // DAD B
                   0x76);             // HLT
        assertEquals(0x2345, cpu.getRegisters().getHL());
        assertFalse(cpu.getFlags().isCarry());
    }

    @Test
    void testSTC_CMC() {
        loadAndRun(0x37,  // STC → CY = 1
                   0x3F,  // CMC → CY = 0
                   0x76); // HLT
        assertFalse(cpu.getFlags().isCarry());
    }

    @Test
    void testCMP_equal() {
        // A=0x55, B=0x55 → CMP B → Z=1, CY=0
        loadAndRun(0x3E, 0x55,  // MVI A, 55H
                   0x06, 0x55,  // MVI B, 55H
                   0xB8,        // CMP B
                   0x76);       // HLT
        assertEquals(0x55, cpu.getRegisters().getA()); // A unchanged
        assertTrue(cpu.getFlags().isZero());
        assertFalse(cpu.getFlags().isCarry());
    }

    @Test
    void testCMP_less() {
        // A=0x05, B=0x10 → CMP B → Z=0, CY=1
        loadAndRun(0x3E, 0x05,  // MVI A, 05H
                   0x06, 0x10,  // MVI B, 10H
                   0xB8,        // CMP B
                   0x76);       // HLT
        assertEquals(0x05, cpu.getRegisters().getA()); // A unchanged
        assertFalse(cpu.getFlags().isZero());
        assertTrue(cpu.getFlags().isCarry());
    }
}
