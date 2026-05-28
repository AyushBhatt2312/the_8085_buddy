/* ═══════════════════════════════════════════════════════════════════════
   UI.JS — Dashboard rendering & state-change highlighting
   ═══════════════════════════════════════════════════════════════════════ */

const UI = (() => {

    // Previous state for diffing (to highlight changes)
    let prevRegisters = {};
    let prevFlags = {};
    let prevMemory = {};

    // ── DOM references ───────────────────────────────────────────────────
    const dom = {
        consoleOutput: () => document.getElementById('consoleOutput'),
        statusBadge: () => document.getElementById('statusBadge'),
        memoryTableBody: () => document.getElementById('memoryTableBody'),
        // 7-seg display
        addr3: () => document.getElementById('addr3'),
        addr2: () => document.getElementById('addr2'),
        addr1: () => document.getElementById('addr1'),
        addr0: () => document.getElementById('addr0'),
        data1: () => document.getElementById('data1'),
        data0: () => document.getElementById('data0'),
    };

    // ── Console logging ──────────────────────────────────────────────────
    function log(message, type = 'info') {
        const el = dom.consoleOutput();
        const line = document.createElement('span');
        line.className = `console-line ${type}`;
        line.textContent = `▸ ${message}`;
        el.appendChild(line);
        el.scrollTop = el.scrollHeight;
    }

    function clearConsole() {
        dom.consoleOutput().innerHTML = '';
        log('Console cleared.', 'info');
    }

    // ── 7-Segment Display ────────────────────────────────────────────────
    function setDisplay(addressHex, dataHex) {
        const addr = addressHex.padStart(4, '0').toUpperCase();
        const data = dataHex.padStart(2, '0').toUpperCase();
        dom.addr3().textContent = addr[0];
        dom.addr2().textContent = addr[1];
        dom.addr1().textContent = addr[2];
        dom.addr0().textContent = addr[3];
        dom.data1().textContent = data[0];
        dom.data0().textContent = data[1];
    }

    // ── Highlight helper ─────────────────────────────────────────────────
    function flashHighlight(element) {
        element.classList.remove('highlight');
        // Force reflow so the animation re-triggers
        void element.offsetWidth;
        element.classList.add('highlight');
        setTimeout(() => element.classList.remove('highlight'), 900);
    }

    // ── Update state dashboard ───────────────────────────────────────────
    function updateDashboard(state) {
        if (!state) return;

        // -- Registers --
        const regMap = state.registers || {};
        for (const [name, value] of Object.entries(regMap)) {
            const row = document.getElementById(`reg${name}`);
            if (!row) continue;

            const is16bit = (name === 'PC' || name === 'SP');
            const hexStr = is16bit
                ? value.toString(16).toUpperCase().padStart(4, '0')
                : value.toString(16).toUpperCase().padStart(2, '0');

            const hexCell = row.querySelector('.hex-val');
            const decCell = row.querySelector('.dec-val');

            if (hexCell) hexCell.textContent = hexStr;
            if (decCell) decCell.textContent = value;

            // Highlight if changed
            if (prevRegisters[name] !== undefined && prevRegisters[name] !== value) {
                if (hexCell) flashHighlight(hexCell);
                if (decCell) flashHighlight(decCell);
            }
        }
        prevRegisters = { ...regMap };

        // -- Flags --
        const flagMap = state.flags || {};
        for (const [name, value] of Object.entries(flagMap)) {
            const cell = document.getElementById(`flag${name}`);
            if (!cell) continue;

            const numVal = value ? 1 : 0;
            cell.textContent = numVal;

            if (prevFlags[name] !== undefined && prevFlags[name] !== value) {
                flashHighlight(cell);
            }
        }
        prevFlags = { ...flagMap };

        // -- Memory --
        const memMap = state.memory || {};
        const tbody = dom.memoryTableBody();

        if (Object.keys(memMap).length === 0) {
            tbody.innerHTML = '<tr><td colspan="2" class="empty-state">No data loaded</td></tr>';
        } else {
            tbody.innerHTML = '';
            for (const [addr, val] of Object.entries(memMap)) {
                const tr = document.createElement('tr');
                tr.id = `mem${addr}`;

                const tdAddr = document.createElement('td');
                tdAddr.textContent = addr;
                const tdVal = document.createElement('td');
                tdVal.className = 'val';
                tdVal.textContent = val.toString(16).toUpperCase().padStart(2, '0');

                tr.appendChild(tdAddr);
                tr.appendChild(tdVal);
                tbody.appendChild(tr);

                // Highlight changed memory
                if (prevMemory[addr] !== undefined && prevMemory[addr] !== val) {
                    flashHighlight(tdVal);
                }
            }
        }
        prevMemory = { ...memMap };

        // -- Status badge --
        const badge = dom.statusBadge();
        if (state.halted) {
            badge.textContent = 'HALTED';
            badge.classList.add('halted');
        } else {
            badge.textContent = 'READY';
            badge.classList.remove('halted');
        }

        // -- Update 7-seg with PC and data at PC --
        const pc = regMap['PC'] || 0;
        const pcHex = pc.toString(16).toUpperCase().padStart(4, '0');
        const dataAtPc = memMap[pcHex];
        const dataHex = dataAtPc !== undefined
            ? dataAtPc.toString(16).toUpperCase().padStart(2, '0')
            : '00';
        setDisplay(pcHex, dataHex);

        // -- Log last instruction --
        if (state.lastInstruction && state.lastInstruction.length > 0) {
            log(`Executed: ${state.lastInstruction}`, 'success');
        }
    }

    // ── Public API ───────────────────────────────────────────────────────
    return {
        log,
        clearConsole,
        setDisplay,
        updateDashboard,
    };

})();
