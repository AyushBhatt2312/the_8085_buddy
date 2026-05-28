package com.example.bhatt.__backend.controller;

import com.example.bhatt.__backend.model.EmulatorStateDto;
import com.example.bhatt.__backend.model.LoadRequest;
import com.example.bhatt.__backend.service.EmulatorService;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class EmulatorController {

    private final EmulatorService emulatorService;

    public EmulatorController(EmulatorService emulatorService) {
        this.emulatorService = emulatorService;
    }

    /** Resets all registers, flags, and memory. */
    @PostMapping("/reset")
    public EmulatorStateDto reset() {
        return emulatorService.reset();
    }

    /** Resets registers and flags but keeps memory intact. */
    @PostMapping("/reset-cpu")
    public EmulatorStateDto resetCpu() {
        return emulatorService.resetCpu();
    }

    /** Loads hex codes into memory starting at the given address AND sets PC. */
    @PostMapping("/load")
    public EmulatorStateDto load(@RequestBody LoadRequest request) {
        return emulatorService.loadProgram(request.getStartAddress(), request.getHexCodes());
    }

    /** Writes hex codes into memory WITHOUT changing PC (for data entry). */
    @PostMapping("/write-memory")
    public EmulatorStateDto writeMemory(@RequestBody LoadRequest request) {
        return emulatorService.writeMemory(request.getStartAddress(), request.getHexCodes());
    }

    /** Executes one instruction and returns the updated state. */
    @PostMapping("/step")
    public EmulatorStateDto step() {
        return emulatorService.step();
    }

    /** Returns the current emulator state without modifying it. */
    @GetMapping("/state")
    public EmulatorStateDto state() {
        return emulatorService.getState();
    }
}
