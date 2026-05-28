package com.example.bhatt.__backend.model;

import java.util.Map;

public class EmulatorStateDto {

    private Map<String, Integer> registers;
    private Map<String, Boolean> flags;
    private Map<String, Integer> memory;  // only non-zero / recently-touched addresses
    private boolean halted;
    private String lastInstruction;

    public EmulatorStateDto() {}

    public EmulatorStateDto(Map<String, Integer> registers,
                            Map<String, Boolean> flags,
                            Map<String, Integer> memory,
                            boolean halted,
                            String lastInstruction) {
        this.registers = registers;
        this.flags = flags;
        this.memory = memory;
        this.halted = halted;
        this.lastInstruction = lastInstruction;
    }

    // ── Getters / Setters ────────────────────────────────────────────────

    public Map<String, Integer> getRegisters() { return registers; }
    public void setRegisters(Map<String, Integer> registers) { this.registers = registers; }

    public Map<String, Boolean> getFlags() { return flags; }
    public void setFlags(Map<String, Boolean> flags) { this.flags = flags; }

    public Map<String, Integer> getMemory() { return memory; }
    public void setMemory(Map<String, Integer> memory) { this.memory = memory; }

    public boolean isHalted() { return halted; }
    public void setHalted(boolean halted) { this.halted = halted; }

    public String getLastInstruction() { return lastInstruction; }
    public void setLastInstruction(String lastInstruction) { this.lastInstruction = lastInstruction; }
}
