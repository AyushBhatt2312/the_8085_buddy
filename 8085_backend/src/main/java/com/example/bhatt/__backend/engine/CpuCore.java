package com.example.bhatt.__backend.engine;

/**
 * Simulates the 8085 CPU core.
 *
 * Supports a fetch→decode→execute pipeline via the {@link #step()} method.
 * Implements the complete 8085 instruction set.
 */
public class CpuCore {

    private final Registers registers;
    private final Flags flags;
    private final Memory memory;
    private boolean halted;
    private boolean interruptsEnabled;

    public CpuCore() {
        registers = new Registers();
        flags = new Flags();
        memory = new Memory();
        halted = false;
        interruptsEnabled = false;
    }

    // ── Public API ───────────────────────────────────────────────────────

    /** Resets CPU and memory to initial state. */
    public void reset() {
        registers.reset();
        flags.reset();
        memory.reset();
        halted = false;
        interruptsEnabled = false;
    }

    /**
     * Executes a single instruction at the current PC.
     * @return the mnemonic string of the executed instruction (for logging).
     */
    public String step() {
        if (halted) {
            return "HALTED";
        }

        int opcode = fetchByte();
        String mnemonic = InstructionSet.getMnemonic(opcode);

        decode(opcode);

        return mnemonic;
    }

    // ── Fetch helpers ────────────────────────────────────────────────────

    /** Fetches the byte at PC and increments PC. */
    private int fetchByte() {
        int value = memory.read(registers.getPc());
        registers.setPc(registers.getPc() + 1);
        return value;
    }

    /** Fetches a 16-bit word (little-endian) at PC, advancing PC by 2. */
    private int fetchWord() {
        int low = fetchByte();
        int high = fetchByte();
        return (high << 8) | low;
    }

    // ── Stack helpers ────────────────────────────────────────────────────

    /** Pushes a 16-bit value onto the stack (high byte first, then low). */
    private void pushWord(int value) {
        int sp = registers.getSp();
        sp = (sp - 1) & 0xFFFF;
        memory.write(sp, (value >> 8) & 0xFF); // high byte
        sp = (sp - 1) & 0xFFFF;
        memory.write(sp, value & 0xFF);          // low byte
        registers.setSp(sp);
    }

    /** Pops a 16-bit value from the stack. */
    private int popWord() {
        int sp = registers.getSp();
        int low = memory.read(sp);
        sp = (sp + 1) & 0xFFFF;
        int high = memory.read(sp);
        sp = (sp + 1) & 0xFFFF;
        registers.setSp(sp);
        return (high << 8) | low;
    }

    // ── Register-pair helpers ────────────────────────────────────────────

    /** Returns a register value; code 6 = memory at HL. */
    private int getRegOrMem(int code) {
        if (code == 6) return memory.read(registers.getHL());
        return registers.getByCode(code);
    }

    /** Sets a register value; code 6 = memory at HL. */
    private void setRegOrMem(int code, int value) {
        if (code == 6) {
            memory.write(registers.getHL(), value & 0xFF);
        } else {
            registers.setByCode(code, value & 0xFF);
        }
    }

    /** Returns the 16-bit register pair value by pair code: 0=BC, 1=DE, 2=HL, 3=SP. */
    private int getRegPair(int pairCode) {
        return switch (pairCode) {
            case 0 -> registers.getBC();
            case 1 -> registers.getDE();
            case 2 -> registers.getHL();
            case 3 -> registers.getSp();
            default -> 0;
        };
    }

    /** Sets the 16-bit register pair by pair code. */
    private void setRegPair(int pairCode, int value) {
        switch (pairCode) {
            case 0 -> registers.setBC(value & 0xFFFF);
            case 1 -> registers.setDE(value & 0xFFFF);
            case 2 -> registers.setHL(value & 0xFFFF);
            case 3 -> registers.setSp(value & 0xFFFF);
        }
    }

    /** Builds the PSW (A in high byte, flags in low byte). */
    private int getPSW() {
        int f = 0;
        if (flags.isSign())           f |= 0x80;
        if (flags.isZero())           f |= 0x40;
        if (flags.isAuxiliaryCarry()) f |= 0x10;
        if (flags.isParity())         f |= 0x04;
        if (flags.isCarry())          f |= 0x01;
        f |= 0x02; // bit 1 is always 1 in 8085
        return (registers.getA() << 8) | f;
    }

    /** Restores A and flags from a PSW value. */
    private void setPSW(int psw) {
        registers.setA((psw >> 8) & 0xFF);
        int f = psw & 0xFF;
        flags.setSign((f & 0x80) != 0);
        flags.setZero((f & 0x40) != 0);
        flags.setAuxiliaryCarry((f & 0x10) != 0);
        flags.setParity((f & 0x04) != 0);
        flags.setCarry((f & 0x01) != 0);
    }

    /** Check condition based on 3-bit condition code (bits 5-3 of opcode). */
    private boolean checkCondition(int cc) {
        return switch (cc) {
            case 0 -> !flags.isZero();        // NZ
            case 1 -> flags.isZero();          // Z
            case 2 -> !flags.isCarry();        // NC
            case 3 -> flags.isCarry();         // C
            case 4 -> !flags.isParity();       // PO (parity odd)
            case 5 -> flags.isParity();        // PE (parity even)
            case 6 -> !flags.isSign();         // P  (positive)
            case 7 -> flags.isSign();          // M  (minus)
            default -> false;
        };
    }

    // ── Decoder / Executor ───────────────────────────────────────────────

    private void decode(int opcode) {

        // ── NOP (0x00) ───────────────────────────────────────────────────
        if (opcode == 0x00) {
            return;
        }

        // ── HLT (0x76) ──────────────────────────────────────────────────
        if (opcode == 0x76) {
            halted = true;
            return;
        }

        // ══════════════════════════════════════════════════════════════════
        //  DATA TRANSFER GROUP
        // ══════════════════════════════════════════════════════════════════

        // ── MVI r, data  (00 DDD 110) ───────────────────────────────────
        if ((opcode & 0xC7) == 0x06) {
            int dst = (opcode >> 3) & 0x07;
            int data = fetchByte();
            setRegOrMem(dst, data);
            return;
        }

        // ── MOV dst, src  (01 DDD SSS) ──────────────────────────────────
        if ((opcode & 0xC0) == 0x40) {
            int dst = (opcode >> 3) & 0x07;
            int src = opcode & 0x07;
            int value = getRegOrMem(src);
            setRegOrMem(dst, value);
            return;
        }

        // ── LXI rp, data16  (00 RP0 001) ────────────────────────────────
        if ((opcode & 0xCF) == 0x01) {
            int rp = (opcode >> 4) & 0x03;
            int data16 = fetchWord();
            setRegPair(rp, data16);
            return;
        }

        // ── LDA addr (0x3A) ─────────────────────────────────────────────
        if (opcode == 0x3A) {
            int addr = fetchWord();
            registers.setA(memory.read(addr));
            return;
        }

        // ── STA addr (0x32) ─────────────────────────────────────────────
        if (opcode == 0x32) {
            int addr = fetchWord();
            memory.write(addr, registers.getA());
            return;
        }

        // ── LHLD addr (0x2A) ────────────────────────────────────────────
        if (opcode == 0x2A) {
            int addr = fetchWord();
            registers.setL(memory.read(addr));
            registers.setH(memory.read((addr + 1) & 0xFFFF));
            return;
        }

        // ── SHLD addr (0x22) ────────────────────────────────────────────
        if (opcode == 0x22) {
            int addr = fetchWord();
            memory.write(addr, registers.getL());
            memory.write((addr + 1) & 0xFFFF, registers.getH());
            return;
        }

        // ── LDAX B (0x0A) / LDAX D (0x1A) ──────────────────────────────
        if (opcode == 0x0A) {
            registers.setA(memory.read(registers.getBC()));
            return;
        }
        if (opcode == 0x1A) {
            registers.setA(memory.read(registers.getDE()));
            return;
        }

        // ── STAX B (0x02) / STAX D (0x12) ──────────────────────────────
        if (opcode == 0x02) {
            memory.write(registers.getBC(), registers.getA());
            return;
        }
        if (opcode == 0x12) {
            memory.write(registers.getDE(), registers.getA());
            return;
        }

        // ── XCHG (0xEB) — exchange DE ↔ HL ─────────────────────────────
        if (opcode == 0xEB) {
            int de = registers.getDE();
            int hl = registers.getHL();
            registers.setDE(hl);
            registers.setHL(de);
            return;
        }

        // ── XTHL (0xE3) — exchange HL with top of stack ─────────────────
        if (opcode == 0xE3) {
            int sp = registers.getSp();
            int low = memory.read(sp);
            int high = memory.read((sp + 1) & 0xFFFF);
            memory.write(sp, registers.getL());
            memory.write((sp + 1) & 0xFFFF, registers.getH());
            registers.setL(low);
            registers.setH(high);
            return;
        }

        // ── SPHL (0xF9) — SP ← HL ──────────────────────────────────────
        if (opcode == 0xF9) {
            registers.setSp(registers.getHL());
            return;
        }

        // ── PCHL (0xE9) — PC ← HL ──────────────────────────────────────
        if (opcode == 0xE9) {
            registers.setPc(registers.getHL());
            return;
        }

        // ══════════════════════════════════════════════════════════════════
        //  ARITHMETIC GROUP
        // ══════════════════════════════════════════════════════════════════

        // ── ADD r  (10 000 SSS) ─────────────────────────────────────────
        if ((opcode & 0xF8) == 0x80) {
            int src = opcode & 0x07;
            int operand = getRegOrMem(src);
            int a = registers.getA();
            int result = a + operand;
            flags.updateFlagsAdd(result, a, operand);
            registers.setA(result & 0xFF);
            return;
        }

        // ── ADC r  (10 001 SSS) — add with carry ───────────────────────
        if ((opcode & 0xF8) == 0x88) {
            int src = opcode & 0x07;
            int operand = getRegOrMem(src);
            int cy = flags.isCarry() ? 1 : 0;
            int a = registers.getA();
            int result = a + operand + cy;
            flags.updateFlagsAdd(result, a, operand + cy);
            registers.setA(result & 0xFF);
            return;
        }

        // ── SUB r  (10 010 SSS) ─────────────────────────────────────────
        if ((opcode & 0xF8) == 0x90) {
            int src = opcode & 0x07;
            int operand = getRegOrMem(src);
            int a = registers.getA();
            int result = a - operand;
            flags.updateFlagsSub(result, a, operand);
            flags.setCarry(a < operand);
            registers.setA(result & 0xFF);
            return;
        }

        // ── SBB r  (10 011 SSS) — subtract with borrow ─────────────────
        if ((opcode & 0xF8) == 0x98) {
            int src = opcode & 0x07;
            int operand = getRegOrMem(src);
            int cy = flags.isCarry() ? 1 : 0;
            int a = registers.getA();
            int result = a - operand - cy;
            flags.updateFlagsSub(result, a, operand + cy);
            flags.setCarry((a & 0xFF) < ((operand + cy) & 0xFF));
            registers.setA(result & 0xFF);
            return;
        }

        // ── INR r  (00 DDD 100) ─────────────────────────────────────────
        if ((opcode & 0xC7) == 0x04) {
            int reg = (opcode >> 3) & 0x07;
            int value = getRegOrMem(reg);
            int result = (value + 1) & 0xFF;
            flags.updateFlagsInr(result, value);
            setRegOrMem(reg, result);
            return;
        }

        // ── DCR r  (00 DDD 101) ─────────────────────────────────────────
        if ((opcode & 0xC7) == 0x05) {
            int reg = (opcode >> 3) & 0x07;
            int value = getRegOrMem(reg);
            int result = (value - 1) & 0xFF;
            flags.updateFlagsDcr(result, value);
            setRegOrMem(reg, result);
            return;
        }

        // ── INX rp (00 RP0 011) ─────────────────────────────────────────
        if ((opcode & 0xCF) == 0x03) {
            int rp = (opcode >> 4) & 0x03;
            setRegPair(rp, (getRegPair(rp) + 1) & 0xFFFF);
            return;
        }

        // ── DCX rp (00 RP1 011) ─────────────────────────────────────────
        if ((opcode & 0xCF) == 0x0B) {
            int rp = (opcode >> 4) & 0x03;
            setRegPair(rp, (getRegPair(rp) - 1) & 0xFFFF);
            return;
        }

        // ── DAD rp (00 RP1 001) — double add to HL ─────────────────────
        if ((opcode & 0xCF) == 0x09) {
            int rp = (opcode >> 4) & 0x03;
            int hl = registers.getHL();
            int rpVal = getRegPair(rp);
            int result = hl + rpVal;
            flags.setCarry(result > 0xFFFF);
            registers.setHL(result & 0xFFFF);
            return;
        }

        // ── ADI data (0xC6) ─────────────────────────────────────────────
        if (opcode == 0xC6) {
            int data = fetchByte();
            int a = registers.getA();
            int result = a + data;
            flags.updateFlagsAdd(result, a, data);
            registers.setA(result & 0xFF);
            return;
        }

        // ── ACI data (0xCE) ─────────────────────────────────────────────
        if (opcode == 0xCE) {
            int data = fetchByte();
            int cy = flags.isCarry() ? 1 : 0;
            int a = registers.getA();
            int result = a + data + cy;
            flags.updateFlagsAdd(result, a, data + cy);
            registers.setA(result & 0xFF);
            return;
        }

        // ── SUI data (0xD6) ─────────────────────────────────────────────
        if (opcode == 0xD6) {
            int data = fetchByte();
            int a = registers.getA();
            int result = a - data;
            flags.updateFlagsSub(result, a, data);
            flags.setCarry(a < data);
            registers.setA(result & 0xFF);
            return;
        }

        // ── SBI data (0xDE) ─────────────────────────────────────────────
        if (opcode == 0xDE) {
            int data = fetchByte();
            int cy = flags.isCarry() ? 1 : 0;
            int a = registers.getA();
            int result = a - data - cy;
            flags.updateFlagsSub(result, a, data + cy);
            flags.setCarry((a & 0xFF) < ((data + cy) & 0xFF));
            registers.setA(result & 0xFF);
            return;
        }

        // ── DAA (0x27) — Decimal Adjust Accumulator ─────────────────────
        if (opcode == 0x27) {
            int a = registers.getA();
            boolean newCarry = flags.isCarry();
            if ((a & 0x0F) > 9 || flags.isAuxiliaryCarry()) {
                flags.setAuxiliaryCarry((a & 0x0F) > 9);
                a += 6;
            }
            if (((a >> 4) & 0x0F) > 9 || newCarry || a > 0xFF) {
                a += 0x60;
                newCarry = true;
            }
            int result = a & 0xFF;
            flags.setSign((result & 0x80) != 0);
            flags.setZero(result == 0);
            flags.setParity(Integer.bitCount(result) % 2 == 0);
            flags.setCarry(newCarry);
            registers.setA(result);
            return;
        }

        // ══════════════════════════════════════════════════════════════════
        //  LOGICAL GROUP
        // ══════════════════════════════════════════════════════════════════

        // ── ANA r  (10 100 SSS) ─────────────────────────────────────────
        if ((opcode & 0xF8) == 0xA0) {
            int src = opcode & 0x07;
            int result = registers.getA() & getRegOrMem(src);
            flags.updateFlagsLogical(result, true);
            registers.setA(result & 0xFF);
            return;
        }

        // ── XRA r  (10 101 SSS) ─────────────────────────────────────────
        if ((opcode & 0xF8) == 0xA8) {
            int src = opcode & 0x07;
            int result = registers.getA() ^ getRegOrMem(src);
            flags.updateFlagsLogical(result, false);
            registers.setA(result & 0xFF);
            return;
        }

        // ── ORA r  (10 110 SSS) ─────────────────────────────────────────
        if ((opcode & 0xF8) == 0xB0) {
            int src = opcode & 0x07;
            int result = registers.getA() | getRegOrMem(src);
            flags.updateFlagsLogical(result, false);
            registers.setA(result & 0xFF);
            return;
        }

        // ── CMP r  (10 111 SSS) ─────────────────────────────────────────
        if ((opcode & 0xF8) == 0xB8) {
            int src = opcode & 0x07;
            int operand = getRegOrMem(src);
            int a = registers.getA();
            int result = a - operand;
            flags.updateFlagsSub(result, a, operand);
            flags.setCarry(a < operand);
            // A is NOT modified
            return;
        }

        // ── ANI data (0xE6) ─────────────────────────────────────────────
        if (opcode == 0xE6) {
            int data = fetchByte();
            int result = registers.getA() & data;
            flags.updateFlagsLogical(result, true);
            registers.setA(result & 0xFF);
            return;
        }

        // ── XRI data (0xEE) ─────────────────────────────────────────────
        if (opcode == 0xEE) {
            int data = fetchByte();
            int result = registers.getA() ^ data;
            flags.updateFlagsLogical(result, false);
            registers.setA(result & 0xFF);
            return;
        }

        // ── ORI data (0xF6) ─────────────────────────────────────────────
        if (opcode == 0xF6) {
            int data = fetchByte();
            int result = registers.getA() | data;
            flags.updateFlagsLogical(result, false);
            registers.setA(result & 0xFF);
            return;
        }

        // ── CPI data (0xFE) ─────────────────────────────────────────────
        if (opcode == 0xFE) {
            int data = fetchByte();
            int a = registers.getA();
            int result = a - data;
            flags.updateFlagsSub(result, a, data);
            flags.setCarry(a < data);
            return;
        }

        // ── CMA (0x2F) — complement accumulator ────────────────────────
        if (opcode == 0x2F) {
            registers.setA(~registers.getA() & 0xFF);
            return;
        }

        // ── STC (0x37) — set carry ──────────────────────────────────────
        if (opcode == 0x37) {
            flags.setCarry(true);
            return;
        }

        // ── CMC (0x3F) — complement carry ───────────────────────────────
        if (opcode == 0x3F) {
            flags.setCarry(!flags.isCarry());
            return;
        }

        // ══════════════════════════════════════════════════════════════════
        //  ROTATE GROUP
        // ══════════════════════════════════════════════════════════════════

        // ── RLC (0x07) — rotate A left, bit 7 → CY and bit 0 ───────────
        if (opcode == 0x07) {
            int a = registers.getA();
            int bit7 = (a >> 7) & 1;
            a = ((a << 1) | bit7) & 0xFF;
            flags.setCarry(bit7 == 1);
            registers.setA(a);
            return;
        }

        // ── RRC (0x0F) — rotate A right, bit 0 → CY and bit 7 ─────────
        if (opcode == 0x0F) {
            int a = registers.getA();
            int bit0 = a & 1;
            a = ((a >> 1) | (bit0 << 7)) & 0xFF;
            flags.setCarry(bit0 == 1);
            registers.setA(a);
            return;
        }

        // ── RAL (0x17) — rotate A left through carry ───────────────────
        if (opcode == 0x17) {
            int a = registers.getA();
            int oldCarry = flags.isCarry() ? 1 : 0;
            flags.setCarry((a & 0x80) != 0);
            a = ((a << 1) | oldCarry) & 0xFF;
            registers.setA(a);
            return;
        }

        // ── RAR (0x1F) — rotate A right through carry ──────────────────
        if (opcode == 0x1F) {
            int a = registers.getA();
            int oldCarry = flags.isCarry() ? 1 : 0;
            flags.setCarry((a & 0x01) != 0);
            a = ((a >> 1) | (oldCarry << 7)) & 0xFF;
            registers.setA(a);
            return;
        }

        // ══════════════════════════════════════════════════════════════════
        //  STACK GROUP
        // ══════════════════════════════════════════════════════════════════

        // ── PUSH rp (11 RP0 101) ────────────────────────────────────────
        if ((opcode & 0xCF) == 0xC5) {
            int rp = (opcode >> 4) & 0x03;
            if (rp == 3) {
                // PUSH PSW
                pushWord(getPSW());
            } else {
                pushWord(getRegPair(rp));
            }
            return;
        }

        // ── POP rp (11 RP0 001) ─────────────────────────────────────────
        if ((opcode & 0xCF) == 0xC1) {
            int rp = (opcode >> 4) & 0x03;
            int value = popWord();
            if (rp == 3) {
                // POP PSW
                setPSW(value);
            } else {
                setRegPair(rp, value);
            }
            return;
        }

        // ══════════════════════════════════════════════════════════════════
        //  BRANCH GROUP
        // ══════════════════════════════════════════════════════════════════

        // ── JMP addr (0xC3) ─────────────────────────────────────────────
        if (opcode == 0xC3) {
            int addr = fetchWord();
            registers.setPc(addr);
            return;
        }

        // ── Jcc addr (11 CCC 010) — conditional jump ───────────────────
        if ((opcode & 0xC7) == 0xC2) {
            int cc = (opcode >> 3) & 0x07;
            int addr = fetchWord();
            if (checkCondition(cc)) {
                registers.setPc(addr);
            }
            return;
        }

        // ── CALL addr (0xCD) ────────────────────────────────────────────
        if (opcode == 0xCD) {
            int addr = fetchWord();
            pushWord(registers.getPc());
            registers.setPc(addr);
            return;
        }

        // ── Ccc addr (11 CCC 100) — conditional call ───────────────────
        if ((opcode & 0xC7) == 0xC4) {
            int cc = (opcode >> 3) & 0x07;
            int addr = fetchWord();
            if (checkCondition(cc)) {
                pushWord(registers.getPc());
                registers.setPc(addr);
            }
            return;
        }

        // ── RET (0xC9) ─────────────────────────────────────────────────
        if (opcode == 0xC9) {
            registers.setPc(popWord());
            return;
        }

        // ── Rcc (11 CCC 000) — conditional return ──────────────────────
        if ((opcode & 0xC7) == 0xC0) {
            int cc = (opcode >> 3) & 0x07;
            if (checkCondition(cc)) {
                registers.setPc(popWord());
            }
            return;
        }

        // ── RST n (11 NNN 111) — restart ───────────────────────────────
        if ((opcode & 0xC7) == 0xC7) {
            int n = (opcode >> 3) & 0x07;
            pushWord(registers.getPc());
            registers.setPc(n * 8);
            return;
        }

        // ══════════════════════════════════════════════════════════════════
        //  I/O & INTERRUPT GROUP
        // ══════════════════════════════════════════════════════════════════

        // ── IN port (0xDB) ──────────────────────────────────────────────
        if (opcode == 0xDB) {
            fetchByte(); // read and discard port number (no I/O simulation)
            return;
        }

        // ── OUT port (0xD3) ─────────────────────────────────────────────
        if (opcode == 0xD3) {
            fetchByte(); // read and discard port number
            return;
        }

        // ── EI (0xFB) ──────────────────────────────────────────────────
        if (opcode == 0xFB) {
            interruptsEnabled = true;
            return;
        }

        // ── DI (0xF3) ──────────────────────────────────────────────────
        if (opcode == 0xF3) {
            interruptsEnabled = false;
            return;
        }

        // ── RIM (0x20) / SIM (0x30) — emulated as NOP ──────────────────
        if (opcode == 0x20 || opcode == 0x30) {
            return;
        }

        // ── Unimplemented opcode ─────────────────────────────────────────
        System.err.println("WARNING: Unimplemented opcode 0x"
                + String.format("%02X", opcode) + " at PC=0x"
                + String.format("%04X", registers.getPc() - 1));
    }

    // ── Accessors ────────────────────────────────────────────────────────

    public Registers getRegisters() { return registers; }
    public Flags     getFlags()     { return flags; }
    public Memory    getMemory()    { return memory; }
    public boolean   isHalted()     { return halted; }
}
