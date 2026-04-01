/* ═══════════════════════════════════════════════════════════════════════
   EMULATOR.JS — Ties UI buttons to API calls, keypad logic, assembler
   ═══════════════════════════════════════════════════════════════════════ */

const Emulator = (() => {

    // ── Keypad state ─────────────────────────────────────────────────────
    // Mode: 'address' → entering a 4-digit address
    //        'data'    → entering 2-digit data at the current address
    let mode = 'address';
    let addressBuffer = '';  // up to 4 hex chars
    let dataBuffer = '';     // up to 2 hex chars
    let currentAddress = 0x0000;

    // ── Simple assembler: mnemonic → hex codes ───────────────────────────
    const REG_CODES = { B: 0, C: 1, D: 2, E: 3, H: 4, L: 5, M: 6, A: 7 };
    const RP_CODES  = { B: 0, D: 1, H: 2, SP: 3 };
    const PUSH_POP_CODES = { B: 0, D: 1, H: 2, PSW: 3 };
    const COND_CODES = { NZ: 0, Z: 1, NC: 2, C: 3, PO: 4, PE: 5, P: 6, M: 7 };

    function assemble(source) {
        const lines = source.split('\n');
        const hexCodes = [];
        let errors = [];

        for (let i = 0; i < lines.length; i++) {
            let line = lines[i].trim();
            // remove comments
            const commentIdx = line.indexOf(';');
            if (commentIdx >= 0) line = line.substring(0, commentIdx).trim();
            if (line.length === 0) continue;

            const parts = line.toUpperCase().replace(/,/g, ' ').split(/\s+/);
            const mnemonic = parts[0];

            try {
                switch (mnemonic) {
                    case 'NOP':
                        hexCodes.push(0x00);
                        break;

                    case 'HLT':
                        hexCodes.push(0x76);
                        break;

                    // ── Data Transfer ────────────────────────────────────
                    case 'MVI': {
                        const reg = parts[1];
                        const value = parseImmediate(parts[2]);
                        if (REG_CODES[reg] === undefined) throw new Error(`Invalid register: ${reg}`);
                        hexCodes.push(0x06 | (REG_CODES[reg] << 3), value & 0xFF);
                        break;
                    }

                    case 'MOV': {
                        const dst = parts[1];
                        const src = parts[2];
                        if (REG_CODES[dst] === undefined) throw new Error(`Invalid register: ${dst}`);
                        if (REG_CODES[src] === undefined) throw new Error(`Invalid register: ${src}`);
                        hexCodes.push(0x40 | (REG_CODES[dst] << 3) | REG_CODES[src]);
                        break;
                    }

                    case 'LXI': {
                        const rp = parts[1];
                        const value = parseAddress(parts[2]);
                        if (RP_CODES[rp] === undefined) throw new Error(`Invalid register pair: ${rp}`);
                        hexCodes.push(0x01 | (RP_CODES[rp] << 4), value & 0xFF, (value >> 8) & 0xFF);
                        break;
                    }

                    case 'LDA': {
                        const addr = parseAddress(parts[1]);
                        hexCodes.push(0x3A, addr & 0xFF, (addr >> 8) & 0xFF);
                        break;
                    }

                    case 'STA': {
                        const addr = parseAddress(parts[1]);
                        hexCodes.push(0x32, addr & 0xFF, (addr >> 8) & 0xFF);
                        break;
                    }

                    case 'LHLD': {
                        const addr = parseAddress(parts[1]);
                        hexCodes.push(0x2A, addr & 0xFF, (addr >> 8) & 0xFF);
                        break;
                    }

                    case 'SHLD': {
                        const addr = parseAddress(parts[1]);
                        hexCodes.push(0x22, addr & 0xFF, (addr >> 8) & 0xFF);
                        break;
                    }

                    case 'LDAX': {
                        const rp = parts[1];
                        if (rp === 'B') hexCodes.push(0x0A);
                        else if (rp === 'D') hexCodes.push(0x1A);
                        else throw new Error(`LDAX only supports B or D, got: ${rp}`);
                        break;
                    }

                    case 'STAX': {
                        const rp = parts[1];
                        if (rp === 'B') hexCodes.push(0x02);
                        else if (rp === 'D') hexCodes.push(0x12);
                        else throw new Error(`STAX only supports B or D, got: ${rp}`);
                        break;
                    }

                    case 'XCHG': hexCodes.push(0xEB); break;
                    case 'XTHL': hexCodes.push(0xE3); break;
                    case 'SPHL': hexCodes.push(0xF9); break;
                    case 'PCHL': hexCodes.push(0xE9); break;

                    // ── Arithmetic (register) ───────────────────────────
                    case 'ADD': {
                        const reg = parts[1];
                        if (REG_CODES[reg] === undefined) throw new Error(`Invalid register: ${reg}`);
                        hexCodes.push(0x80 | REG_CODES[reg]);
                        break;
                    }

                    case 'ADC': {
                        const reg = parts[1];
                        if (REG_CODES[reg] === undefined) throw new Error(`Invalid register: ${reg}`);
                        hexCodes.push(0x88 | REG_CODES[reg]);
                        break;
                    }

                    case 'SUB': {
                        const reg = parts[1];
                        if (REG_CODES[reg] === undefined) throw new Error(`Invalid register: ${reg}`);
                        hexCodes.push(0x90 | REG_CODES[reg]);
                        break;
                    }

                    case 'SBB': {
                        const reg = parts[1];
                        if (REG_CODES[reg] === undefined) throw new Error(`Invalid register: ${reg}`);
                        hexCodes.push(0x98 | REG_CODES[reg]);
                        break;
                    }

                    case 'INR': {
                        const reg = parts[1];
                        if (REG_CODES[reg] === undefined) throw new Error(`Invalid register: ${reg}`);
                        hexCodes.push(0x04 | (REG_CODES[reg] << 3));
                        break;
                    }

                    case 'DCR': {
                        const reg = parts[1];
                        if (REG_CODES[reg] === undefined) throw new Error(`Invalid register: ${reg}`);
                        hexCodes.push(0x05 | (REG_CODES[reg] << 3));
                        break;
                    }

                    case 'INX': {
                        const rp = parts[1];
                        if (RP_CODES[rp] === undefined) throw new Error(`Invalid register pair: ${rp}`);
                        hexCodes.push(0x03 | (RP_CODES[rp] << 4));
                        break;
                    }

                    case 'DCX': {
                        const rp = parts[1];
                        if (RP_CODES[rp] === undefined) throw new Error(`Invalid register pair: ${rp}`);
                        hexCodes.push(0x0B | (RP_CODES[rp] << 4));
                        break;
                    }

                    case 'DAD': {
                        const rp = parts[1];
                        if (RP_CODES[rp] === undefined) throw new Error(`Invalid register pair: ${rp}`);
                        hexCodes.push(0x09 | (RP_CODES[rp] << 4));
                        break;
                    }

                    // ── Arithmetic (immediate) ──────────────────────────
                    case 'ADI': hexCodes.push(0xC6, parseImmediate(parts[1]) & 0xFF); break;
                    case 'ACI': hexCodes.push(0xCE, parseImmediate(parts[1]) & 0xFF); break;
                    case 'SUI': hexCodes.push(0xD6, parseImmediate(parts[1]) & 0xFF); break;
                    case 'SBI': hexCodes.push(0xDE, parseImmediate(parts[1]) & 0xFF); break;

                    // ── Logical (register) ──────────────────────────────
                    case 'ANA': {
                        const reg = parts[1];
                        if (REG_CODES[reg] === undefined) throw new Error(`Invalid register: ${reg}`);
                        hexCodes.push(0xA0 | REG_CODES[reg]);
                        break;
                    }

                    case 'XRA': {
                        const reg = parts[1];
                        if (REG_CODES[reg] === undefined) throw new Error(`Invalid register: ${reg}`);
                        hexCodes.push(0xA8 | REG_CODES[reg]);
                        break;
                    }

                    case 'ORA': {
                        const reg = parts[1];
                        if (REG_CODES[reg] === undefined) throw new Error(`Invalid register: ${reg}`);
                        hexCodes.push(0xB0 | REG_CODES[reg]);
                        break;
                    }

                    case 'CMP': {
                        const reg = parts[1];
                        if (REG_CODES[reg] === undefined) throw new Error(`Invalid register: ${reg}`);
                        hexCodes.push(0xB8 | REG_CODES[reg]);
                        break;
                    }

                    // ── Logical (immediate) ─────────────────────────────
                    case 'ANI': hexCodes.push(0xE6, parseImmediate(parts[1]) & 0xFF); break;
                    case 'XRI': hexCodes.push(0xEE, parseImmediate(parts[1]) & 0xFF); break;
                    case 'ORI': hexCodes.push(0xF6, parseImmediate(parts[1]) & 0xFF); break;
                    case 'CPI': hexCodes.push(0xFE, parseImmediate(parts[1]) & 0xFF); break;

                    // ── Accumulator / Flag ops ──────────────────────────
                    case 'CMA': hexCodes.push(0x2F); break;
                    case 'DAA': hexCodes.push(0x27); break;
                    case 'STC': hexCodes.push(0x37); break;
                    case 'CMC': hexCodes.push(0x3F); break;

                    // ── Rotate ──────────────────────────────────────────
                    case 'RLC': hexCodes.push(0x07); break;
                    case 'RRC': hexCodes.push(0x0F); break;
                    case 'RAL': hexCodes.push(0x17); break;
                    case 'RAR': hexCodes.push(0x1F); break;

                    // ── Stack ───────────────────────────────────────────
                    case 'PUSH': {
                        const rp = parts[1];
                        if (PUSH_POP_CODES[rp] === undefined) throw new Error(`Invalid operand: ${rp}`);
                        hexCodes.push(0xC5 | (PUSH_POP_CODES[rp] << 4));
                        break;
                    }

                    case 'POP': {
                        const rp = parts[1];
                        if (PUSH_POP_CODES[rp] === undefined) throw new Error(`Invalid operand: ${rp}`);
                        hexCodes.push(0xC1 | (PUSH_POP_CODES[rp] << 4));
                        break;
                    }

                    // ── Branch ──────────────────────────────────────────
                    case 'JMP': {
                        const addr = parseAddress(parts[1]);
                        hexCodes.push(0xC3, addr & 0xFF, (addr >> 8) & 0xFF);
                        break;
                    }

                    case 'JNZ': case 'JZ': case 'JNC': case 'JC':
                    case 'JPO': case 'JPE': case 'JP': case 'JM': {
                        const cc = COND_CODES[mnemonic.substring(1)];
                        const addr = parseAddress(parts[1]);
                        hexCodes.push(0xC2 | (cc << 3), addr & 0xFF, (addr >> 8) & 0xFF);
                        break;
                    }

                    case 'CALL': {
                        const addr = parseAddress(parts[1]);
                        hexCodes.push(0xCD, addr & 0xFF, (addr >> 8) & 0xFF);
                        break;
                    }

                    case 'CNZ': case 'CZ': case 'CNC': case 'CC':
                    case 'CPO': case 'CPE': case 'CP': case 'CM': {
                        const cc = COND_CODES[mnemonic.substring(1)];
                        const addr = parseAddress(parts[1]);
                        hexCodes.push(0xC4 | (cc << 3), addr & 0xFF, (addr >> 8) & 0xFF);
                        break;
                    }

                    case 'RET': hexCodes.push(0xC9); break;

                    case 'RNZ': case 'RZ': case 'RNC': case 'RC':
                    case 'RPO': case 'RPE': case 'RP': case 'RM': {
                        const cc = COND_CODES[mnemonic.substring(1)];
                        hexCodes.push(0xC0 | (cc << 3));
                        break;
                    }

                    case 'RST': {
                        const n = parseInt(parts[1]);
                        if (n < 0 || n > 7) throw new Error(`RST number must be 0-7, got: ${n}`);
                        hexCodes.push(0xC7 | (n << 3));
                        break;
                    }

                    // ── I/O & Interrupts ────────────────────────────────
                    case 'IN':  hexCodes.push(0xDB, parseImmediate(parts[1]) & 0xFF); break;
                    case 'OUT': hexCodes.push(0xD3, parseImmediate(parts[1]) & 0xFF); break;
                    case 'EI':  hexCodes.push(0xFB); break;
                    case 'DI':  hexCodes.push(0xF3); break;
                    case 'RIM': hexCodes.push(0x20); break;
                    case 'SIM': hexCodes.push(0x30); break;

                    default:
                        errors.push(`Line ${i + 1}: Unknown mnemonic '${mnemonic}'`);
                }
            } catch (e) {
                errors.push(`Line ${i + 1}: ${e.message}`);
            }
        }

        return { hexCodes, errors };
    }

    function parseImmediate(str) {
        if (!str) throw new Error('Missing immediate value');
        str = str.trim();
        if (str.startsWith('0X')) return parseInt(str, 16);
        if (str.endsWith('H'))    return parseInt(str.slice(0, -1), 16);
        return parseInt(str, 10);
    }

    function parseAddress(str) {
        if (!str) throw new Error('Missing address value');
        str = str.trim();
        if (str.startsWith('0X')) return parseInt(str, 16);
        if (str.endsWith('H'))    return parseInt(str.slice(0, -1), 16);
        return parseInt(str, 10);
    }

    // ── Initialization ───────────────────────────────────────────────────
    function init() {
        // Hex keypad buttons
        document.querySelectorAll('.key.hex').forEach(btn => {
            btn.addEventListener('click', () => handleHexKey(btn.dataset.key));
        });

        // Function keys
        document.getElementById('btnExMem').addEventListener('click', handleExMem);
        document.getElementById('btnNext').addEventListener('click', handleNext);
        document.getElementById('btnExec').addEventListener('click', handleExec);
        document.getElementById('btnReset').addEventListener('click', handleReset);

        // Dashboard step button
        document.getElementById('btnStepDash').addEventListener('click', handleStep);

        // Assemble button
        document.getElementById('btnAssemble').addEventListener('click', handleAssemble);

        // Console clear
        document.getElementById('btnClearConsole').addEventListener('click', () => UI.clearConsole());

        // Initial display
        UI.setDisplay('0000', '00');
        UI.log('System initialized. Enter address on the keypad or write assembly.', 'info');
    }

    // ── Keypad Handlers ──────────────────────────────────────────────────

    function handleHexKey(key) {
        if (mode === 'address') {
            if (addressBuffer.length < 4) {
                addressBuffer += key;
            } else {
                // If already 4, shift left
                addressBuffer = addressBuffer.substring(1) + key;
            }
            UI.setDisplay(addressBuffer.padStart(4, '0'), '00');
        } else if (mode === 'data') {
            if (dataBuffer.length < 2) {
                dataBuffer += key;
            } else {
                dataBuffer = dataBuffer.substring(1) + key;
            }
            UI.setDisplay(
                currentAddress.toString(16).toUpperCase().padStart(4, '0'),
                dataBuffer.padStart(2, '0')
            );
        }
    }

    function handleExMem() {
        // "Examine Memory": switch to data-entry mode for the entered address
        if (mode === 'address' && addressBuffer.length > 0) {
            currentAddress = parseInt(addressBuffer, 16);
            mode = 'data';
            dataBuffer = '';
            UI.setDisplay(
                currentAddress.toString(16).toUpperCase().padStart(4, '0'),
                '00'
            );
            UI.log(`Examining address 0x${currentAddress.toString(16).toUpperCase().padStart(4, '0')}`, 'info');
        } else if (mode === 'address') {
            UI.log('Enter an address first.', 'warn');
        }
    }

    async function handleNext() {
        if (mode === 'data') {
            // Store current data at current address, advance to next
            if (dataBuffer.length > 0) {
                const value = parseInt(dataBuffer, 16);
                // Load single byte
                try {
                    const state = await EmulatorAPI.load(currentAddress, [value]);
                    UI.updateDashboard(state);
                } catch (e) {
                    UI.log(`Error loading: ${e.message}`, 'error');
                    return;
                }
                UI.log(`[${currentAddress.toString(16).toUpperCase().padStart(4, '0')}] = ${dataBuffer.toUpperCase().padStart(2, '0')}`, 'success');
            }
            currentAddress = (currentAddress + 1) & 0xFFFF;
            dataBuffer = '';
            UI.setDisplay(
                currentAddress.toString(16).toUpperCase().padStart(4, '0'),
                '00'
            );
        } else {
            UI.log('Press EX MEM first to enter data mode.', 'warn');
        }
    }

    async function handleExec() {
        // Set PC to current address (if in address mode) and start stepping
        if (mode === 'address' && addressBuffer.length > 0) {
            currentAddress = parseInt(addressBuffer, 16);
        }

        // If there's pending data, store it first
        if (mode === 'data' && dataBuffer.length > 0) {
            const value = parseInt(dataBuffer, 16);
            try {
                await EmulatorAPI.load(currentAddress, [value]);
            } catch (e) {
                UI.log(`Error loading: ${e.message}`, 'error');
                return;
            }
        }

        // Step
        await handleStep();

        // Reset to address mode
        mode = 'address';
        addressBuffer = '';
        dataBuffer = '';
    }

    async function handleReset() {
        try {
            const state = await EmulatorAPI.reset();
            UI.updateDashboard(state);
            mode = 'address';
            addressBuffer = '';
            dataBuffer = '';
            UI.setDisplay('0000', '00');
            UI.log('System reset.', 'info');
        } catch (e) {
            UI.log(`Reset error: ${e.message}`, 'error');
        }
    }

    async function handleStep() {
        try {
            const state = await EmulatorAPI.step();
            UI.updateDashboard(state);
            if (state.halted) {
                UI.log('CPU HALTED.', 'warn');
            }
        } catch (e) {
            UI.log(`Step error: ${e.message}`, 'error');
        }
    }

    async function handleAssemble() {
        const source = document.getElementById('codeEditor').value;
        if (!source.trim()) {
            UI.log('Editor is empty.', 'warn');
            return;
        }

        const { hexCodes, errors } = assemble(source);

        if (errors.length > 0) {
            errors.forEach(err => UI.log(err, 'error'));
            return;
        }

        if (hexCodes.length === 0) {
            UI.log('No instructions found.', 'warn');
            return;
        }

        // Log hex dump
        const hexStr = hexCodes.map(b => b.toString(16).toUpperCase().padStart(2, '0')).join(' ');
        UI.log(`Assembled ${hexCodes.length} bytes: ${hexStr}`, 'info');

        // Default load address: 0x0000
        const loadAddr = 0x0000;
        try {
            // Reset first
            await EmulatorAPI.reset();
            const state = await EmulatorAPI.load(loadAddr, hexCodes);
            UI.updateDashboard(state);
            UI.log(`Loaded at 0x${loadAddr.toString(16).toUpperCase().padStart(4, '0')}. Press Step to execute.`, 'success');
        } catch (e) {
            UI.log(`Load error: ${e.message}`, 'error');
        }
    }

    // ── Boot ─────────────────────────────────────────────────────────────
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }

    return { handleStep, handleReset };

})();
