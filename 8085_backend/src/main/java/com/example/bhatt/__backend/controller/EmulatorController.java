package com.example.bhatt.__backend.controller;

import com.example.bhatt.__backend.model.EmulatorStateDto;
import com.example.bhatt.__backend.model.LoadRequest;
import com.example.bhatt.__backend.service.EmulatorService;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller exposing the 8085 emulator API.
 */
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

    /** Loads hex codes into memory starting at the given address. */
    @PostMapping("/load")
    public EmulatorStateDto load(@RequestBody LoadRequest request) {
        return emulatorService.loadProgram(request.getStartAddress(), request.getHexCodes());
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
