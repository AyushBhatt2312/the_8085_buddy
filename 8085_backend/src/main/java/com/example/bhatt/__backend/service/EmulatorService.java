package com.example.bhatt.__backend.service;

import com.example.bhatt.__backend.engine.CpuCore;
import com.example.bhatt.__backend.engine.Flags;
import com.example.bhatt.__backend.engine.Registers;
import com.example.bhatt.__backend.model.EmulatorStateDto;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * Manages the lifecycle of the 8085 CpuCore.
 * Holds a single emulator instance and exposes reset / load / step / state operations.
 */
@Service
public class EmulatorService {

    private final CpuCore cpu = new CpuCore();

    /**
     * Tracks which memory addresses have been written to, so the state DTO
     * only returns "interesting" memory rather than all 64 KB.
     */
    private final java.util.Set<Integer> touchedAddresses = new java.util.TreeSet<>();

    // ── Operations ───────────────────────────────────────────────────────

    public EmulatorStateDto reset() {
        cpu.reset();
        touchedAddresses.clear();
        return buildState("RESET");
    }

    public EmulatorStateDto loadProgram(int startAddress, java.util.List<Integer> hexCodes) {
        int addr = startAddress & 0xFFFF;
        for (int code : hexCodes) {
            cpu.getMemory().write(addr, code);
            touchedAddresses.add(addr);
            addr = (addr + 1) & 0xFFFF;
        }
        // Set PC to the start address so execution begins there
        cpu.getRegisters().setPc(startAddress);
        return buildState("LOADED " + hexCodes.size() + " bytes at 0x"
                + String.format("%04X", startAddress & 0xFFFF));
    }

    public EmulatorStateDto step() {
        String mnemonic = cpu.step();
        // Track any address the PC has touched (for display purposes)
        touchedAddresses.add(cpu.getRegisters().getPc());
        return buildState(mnemonic);
    }

    public EmulatorStateDto getState() {
        return buildState("");
    }

    // ── State snapshot builder ────────────────────────────────────────────

    private EmulatorStateDto buildState(String lastInstruction) {
        Registers r = cpu.getRegisters();
        Flags f = cpu.getFlags();

        // Registers (ordered)
        Map<String, Integer> regs = new LinkedHashMap<>();
        regs.put("A", r.getA());
        regs.put("B", r.getB());
        regs.put("C", r.getC());
        regs.put("D", r.getD());
        regs.put("E", r.getE());
        regs.put("H", r.getH());
        regs.put("L", r.getL());
        regs.put("PC", r.getPc());
        regs.put("SP", r.getSp());

        // Flags
        Map<String, Boolean> flags = new LinkedHashMap<>();
        flags.put("S", f.isSign());
        flags.put("Z", f.isZero());
        flags.put("AC", f.isAuxiliaryCarry());
        flags.put("P", f.isParity());
        flags.put("CY", f.isCarry());

        // Memory: only touched addresses (sorted by address)
        Map<String, Integer> mem = new TreeMap<>();
        for (int addr : touchedAddresses) {
            String key = String.format("%04X", addr);
            mem.put(key, cpu.getMemory().read(addr));
        }

        return new EmulatorStateDto(regs, flags, mem, cpu.isHalted(), lastInstruction);
    }
}
