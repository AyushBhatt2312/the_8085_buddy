package com.example.bhatt.__backend.engine;

/**
 * Represents the 8085 flag register.
 *
 * Flag bits (bit position in the PSW):
 *   S(7)  Z(6)  -(5)  AC(4)  -(3)  P(2)  -(1)  CY(0)
 *
 * All flags are stored as booleans for clarity.
 */
public class Flags {

    private boolean sign;           // S  — set if result bit 7 is 1
    private boolean zero;           // Z  — set if result is 0
    private boolean auxiliaryCarry; // AC — set if carry from bit 3 to bit 4
    private boolean parity;         // P  — set if result has even number of 1-bits
    private boolean carry;          // CY — set if result overflows 8 bits

    public Flags() {
        reset();
    }

    public void reset() {
        sign = false;
        zero = false;
        auxiliaryCarry = false;
        parity = false;
        carry = false;
    }

    // ── Flag calculation helpers ─────────────────────────────────────────

    /**
     * Updates all five flags based on a full (possibly >8-bit) ALU result.
     *
     * @param result    the raw ALU result (may be 9+ bits for carry detection)
     * @param operand1  first operand (8-bit)
     * @param operand2  second operand (8-bit)
     * @param isSubtraction true if the operation was a subtraction (affects AC calc)
     */
    public void updateFlags(int result, int operand1, int operand2, boolean isSubtraction) {
        int masked = result & 0xFF; // 8-bit result

        // Sign flag: bit 7 of the 8-bit result
        sign = (masked & 0x80) != 0;

        // Zero flag: result is zero
        zero = (masked == 0);

        // Carry flag: result exceeded 8 bits
        carry = (result & 0x100) != 0;

        // Parity flag: even number of 1-bits in the 8-bit result
        parity = (Integer.bitCount(masked) % 2 == 0);

        // Auxiliary carry flag: carry from bit 3 to bit 4
        if (isSubtraction) {
            // For subtraction: borrow from bit 4 into lower nibble
            auxiliaryCarry = ((operand1 & 0x0F) - (operand2 & 0x0F)) < 0;
        } else {
            // For addition: carry from lower nibble
            auxiliaryCarry = ((operand1 & 0x0F) + (operand2 & 0x0F)) > 0x0F;
        }
    }


    public void updateFlagsAdd(int result, int operand1, int operand2) {
        updateFlags(result, operand1, operand2, false);
    }


    public void updateFlagsSub(int result, int operand1, int operand2) {
        updateFlags(result, operand1, operand2, true);
    }

    public void updateFlagsLogical(int result, boolean isAnd) {
        int masked = result & 0xFF;
        sign = (masked & 0x80) != 0;
        zero = (masked == 0);
        carry = false;                        // CY always cleared
        parity = (Integer.bitCount(masked) % 2 == 0);
        auxiliaryCarry = isAnd;               // AC set for ANA/ANI, cleared otherwise
    }

    public void updateFlagsInr(int result, int operand) {
        int masked = result & 0xFF;
        sign = (masked & 0x80) != 0;
        zero = (masked == 0);
        parity = (Integer.bitCount(masked) % 2 == 0);
        // AC: carry from bit 3 → bit 4 during increment
        auxiliaryCarry = ((operand & 0x0F) + 1) > 0x0F;
        // CY not affected
    }

    /**
     * Updates flags for DCR (decrement). CY is NOT affected.
     */
    public void updateFlagsDcr(int result, int operand) {
        int masked = result & 0xFF;
        sign = (masked & 0x80) != 0;
        zero = (masked == 0);
        parity = (Integer.bitCount(masked) % 2 == 0);
        // AC: borrow from bit 4 into lower nibble during decrement
        auxiliaryCarry = !((operand & 0x0F) == 0);  // no borrow if lower nibble != 0
        // CY not affected
    }

    // ── Getters / Setters ────────────────────────────────────────────────

    public boolean isSign()           { return sign; }
    public boolean isZero()           { return zero; }
    public boolean isAuxiliaryCarry() { return auxiliaryCarry; }
    public boolean isParity()         { return parity; }
    public boolean isCarry()          { return carry; }

    public void setSign(boolean v)           { sign = v; }
    public void setZero(boolean v)           { zero = v; }
    public void setAuxiliaryCarry(boolean v) { auxiliaryCarry = v; }
    public void setParity(boolean v)         { parity = v; }
    public void setCarry(boolean v)          { carry = v; }
}
