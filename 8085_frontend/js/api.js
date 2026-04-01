/* ═══════════════════════════════════════════════════════════════════════
   API.JS — Fetch wrappers for the 8085 Emulator REST API
   ═══════════════════════════════════════════════════════════════════════ */

const API_BASE = 'http://localhost:8080/api';

const EmulatorAPI = {

    /**
     * Resets all registers, flags, and memory.
     * @returns {Promise<Object>} EmulatorStateDto
     */
    async reset() {
        const res = await fetch(`${API_BASE}/reset`, { method: 'POST' });
        if (!res.ok) throw new Error(`Reset failed: ${res.status}`);
        return res.json();
    },

    /**
     * Loads hex codes into memory starting at the given address.
     * @param {number} startAddress - 16-bit start address
     * @param {number[]} hexCodes   - array of byte values (0–255)
     * @returns {Promise<Object>} EmulatorStateDto
     */
    async load(startAddress, hexCodes) {
        const res = await fetch(`${API_BASE}/load`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ startAddress, hexCodes })
        });
        if (!res.ok) throw new Error(`Load failed: ${res.status}`);
        return res.json();
    },

    /**
     * Executes one instruction at the current PC.
     * @returns {Promise<Object>} EmulatorStateDto
     */
    async step() {
        const res = await fetch(`${API_BASE}/step`, { method: 'POST' });
        if (!res.ok) throw new Error(`Step failed: ${res.status}`);
        return res.json();
    },

    /**
     * Returns the current emulator state without modifying it.
     * @returns {Promise<Object>} EmulatorStateDto
     */
    async getState() {
        const res = await fetch(`${API_BASE}/state`);
        if (!res.ok) throw new Error(`State fetch failed: ${res.status}`);
        return res.json();
    }
};
