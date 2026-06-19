/**
 * PROMETHEE Web Application - Core Logic
 * Handles dynamic UI, local persistence, and API communication.
 */

let colCount = 3, rowCount = 3, debounceTimer, lastResults = null, sidebarOpen = false;

// Selection helpers
const $ = id => document.getElementById(id);
const qsa = s => document.querySelectorAll(s);

/**
 * Sidebar toggle logic
 */
function toggleSidebar() {
    sidebarOpen = !sidebarOpen;
    $("mySidebar").style.width = sidebarOpen ? "250px" : "0";
    $("main-content").style.marginLeft = sidebarOpen ? "250px" : "0";
}

// Initialization and Event Wiring
document.addEventListener('DOMContentLoaded', () => {
    const form = $("prometheeForm");
    if (form) {
        form.addEventListener('input', (e) => {
            // Live sync alternative names to result tables
            if (e.target.name?.startsWith("altName_")) {
                const id = e.target.name.split("_")[1], name = e.target.value || `Alt ${id}`;
                const row = qsa("#resBody tr")[id - 1];
                if (row) row.cells[0].innerText = name;
                const head = qsa("#resHeader th")[id];
                if (head) head.innerText = name;
                updateDropdowns();
                if (lastResults?.alternatives) updatePromethee1Matrix(lastResults.alternatives);
            }
            updatePairwiseComparison();
            debouncedSendData();
        });
    }
    updateButtons(); loadFromLocalStorage(); updateDropdowns(); fetchSessionsList();
});

/**
 * API: Fetch all saved sessions for the sidebar menu
 */
const fetchSessionsList = () => {
    fetch("api/sessions").then(r => r.json()).then(sessions => {
        const sel = $("sessionSelector");
        if (!sel) return;
        sel.innerHTML = '<option value="">-- Select a saved session --</option>';
        sessions.forEach(s => {
            const opt = new Option(`${s.name} (${new Date(s.createdAt).toLocaleString()})`, s.id);
            sel.add(opt);
        });
    }).catch(e => console.error("List error", e));
};

/**
 * API: Load a session's full data from the DB
 */
function loadSessionFromDB() {
    const id = $("sessionSelector").value;
    if (!id) return;
    if (sidebarOpen) toggleSidebar();
    fetch(`api/sessions?id=${id}`).then(r => r.json()).then(data => {
        applyData(data);
        $("sessionNameInput").value = $("sessionSelector").options[$("sessionSelector").selectedIndex].text.split(" (")[0];
        alert("Session loaded!");
    }).catch(() => alert("Load failed"));
}

/**
 * API: Save current matrix and parameters to DB
 */
function saveToDatabase() {
    const name = $("sessionNameInput").value.trim();
    if (!name) return alert("Enter session name");
    const data = Object.fromEntries(new FormData($("prometheeForm")).entries());
    
    // Weight validation before saving
    let sum = 0;
    Object.keys(data).filter(k => k.startsWith("weight_")).forEach(k => sum += parseFloat(String(data[k]).replace(',', '.') || 0));
    if (Math.abs(sum - 1.0) > 0.001) return alert("Weight sum must be 1.0");

    fetch("api/sessions", {
        method: "POST", headers: {"Content-Type": "application/json"},
        body: JSON.stringify({ sessionName: name, data })
    }).then(r => r.json()).then(() => {
        alert("Saved!"); fetchSessionsList();
    }).catch(e => alert("Save failed"));
}

/**
 * Reset board to 3x3 empty state
 */
const createNewSession = () => confirm("New calculation? Unsaved data lost.") && (localStorage.removeItem('promethee_state'), window.location.href = window.location.pathname + "?reset=" + Date.now());

/**
 * Toggle visibility of delete buttons (min 2 alts/criteria)
 */
function updateButtons() {
    const update = (sel, min) => {
        const btns = qsa(sel);
        btns.forEach((b, i) => b.classList.toggle('hidden', !(btns.length > min && i === btns.length - 1)));
    };
    update("tbody .btn-del", 2); update("thead .btn-del", 2);
}

/**
 * Prevents flooding server with requests during typing
 */
const debouncedSendData = () => { clearTimeout(debounceTimer); debounceTimer = setTimeout(() => { saveToLocalStorage(); sendData(); }, 500); };

/**
 * UI: Add a new criterion column
 */
function addCriterion() {
    colCount++;
    const head = qsa("#prometheeForm thead tr")[0];
    const th = document.createElement("th");
    th.innerHTML = `<div class="header-flex"><input type="text" name="critName_${colCount}" placeholder="Criteria ${colCount}"><button type="button" onclick="removeCriterion(this)" class="btn-del hidden">-</button></div>
        <input type="number" name="weight_${colCount}" step="0.01" placeholder="Weight" class="input-weight"><br>
        <select name="func_${colCount}" onchange="showParams(this, ${colCount})">
            <option value="type1">Type 1: Usual</option><option value="type2">Type 2: U-Shape</option><option value="type3">Type 3: V-Shape</option>
            <option value="type4">Type 4: Level</option><option value="type5">Type 5: V-Shape Indiff</option><option value="type6">Type 6: Gaussian</option>
        </select>
        <div id="q_div_${colCount}" class="params">q: <input type="number" name="q_${colCount}" step="0.1" class="input-param"></div>
        <div id="p_div_${colCount}" class="params">p: <input type="number" name="p_${colCount}" step="0.1" class="input-param"></div>
        <div id="s_div_${colCount}" class="params">s: <input type="number" name="s_${colCount}" step="0.1" class="input-param"></div>
        <select name="isMax_${colCount}" onchange="debouncedSendData()"><option value="true">Higher +</option><option value="false">Lower -</option></select>`;
    head.insertBefore(th, head.querySelector(".add-col-th"));
    
    // Add value cells to rows
    qsa("#prometheeForm tbody tr").forEach((r, i) => {
        const td = document.createElement("td");
        td.innerHTML = `<input type="number" name="val_${i+1}_${colCount}" step="0.1" class="input-val">`;
        r.insertBefore(td, r.lastElementChild);
    });
    const foot = $("add-alt-row");
    if (foot) foot.insertBefore(document.createElement("td"), foot.lastElementChild);
    updateButtons(); updatePairwiseComparison();
}

/**
 * UI: Add a new alternative row and result matrix cells
 */
function addAlternative() {
    rowCount++;
    const row = document.createElement("tr");
    let html = `<td><button type="button" onclick="removeAlternative(this)" class="btn-del hidden">-</button><input type="text" name="altName_${rowCount}" placeholder="Alt ${rowCount}"></td>`;
    for (let j = 1; j <= colCount; j++) html += `<td><input type="number" name="val_${rowCount}_${j}" step="0.1" class="input-val"></td>`;
    row.innerHTML = html + `<td></td>`;
    qsa("#prometheeForm tbody")[0].appendChild(row);

    // Sync Results Table headers and body
    const h = $("resHeader");
    const th = document.createElement("th"); th.className = "res-alt-col"; th.innerText = `Alt ${rowCount}`;
    h.insertBefore(th, h.cells[h.cells.length - 3]);

    qsa("#resBody tr").forEach(r => {
        const td = document.createElement("td"); td.className = "res-pair-val"; td.innerText = "-";
        r.insertBefore(td, r.cells[r.cells.length - 3]);
    });

    const nr = document.createElement("tr");
    let rHtml = `<td class="res-alt-row-name">Alt ${rowCount}</td>`;
    for (let j = 1; j <= rowCount; j++) rHtml += `<td class="res-pair-val">${j === rowCount ? "\\" : "-"}</td>`;
    nr.innerHTML = rHtml + `<td class="res-phi-plus">-</td><td class="res-phi-net">-</td><td class="res-rank"><strong>-</strong></td>`;
    $("resBody").appendChild(nr);

    const f = $("resFooter");
    const ftd = document.createElement("td"); ftd.className = "res-phi-minus"; ftd.innerText = "-";
    f.insertBefore(ftd, f.lastElementChild);

    updateButtons(); updateDropdowns();
}

/**
 * UI: Show/Hide p, q, s fields based on function type
 */
function showParams(sel, id) {
    const v = sel.value;
    const t = (d, l) => $(d + id).classList.toggle('visible', l.includes(v));
    t('p_div_', ['type3', 'type4', 'type5']); t('q_div_', ['type2', 'type4', 'type5']); t('s_div_', ['type6']);
    updatePairwiseComparison(); debouncedSendData();
}

/**
 * UI: Remove alternative
 */
function removeAlternative(btn) {
    if (rowCount <= 2) return;
    const row = btn.closest("tr");
    const idx = Array.from(row.parentNode.children).indexOf(row);
    row.remove();
    const h = $("resHeader"), ci = idx + 1;
    if (h.cells[ci]) h.cells[ci].remove();
    qsa("#resBody tr").forEach(r => r.cells[ci]?.remove());
    const rRows = qsa("#resBody tr"); if (rRows[idx]) rRows[idx].remove();
    const f = $("resFooter"); if (f.cells[ci]) f.cells[ci].remove();
    rowCount--; updateButtons(); updateDropdowns(); saveToLocalStorage(); sendData();
}

/**
 * UI: Remove criterion
 */
function removeCriterion(btn) {
    if (colCount <= 2) return;
    const th = btn.closest("th"), h = th.parentNode;
    const idx = Array.from(h.children).indexOf(th);
    th.remove();
    qsa("#prometheeForm tbody tr").forEach(r => r.children[idx]?.remove());
    if ($("add-alt-row")?.children[idx]) $("add-alt-row").children[idx].remove();
    colCount--; updateButtons(); saveToLocalStorage(); sendData();
}

/**
 * API: Trigger full calculation on backend
 */
async function sendData() {
    const form = $("prometheeForm"); if (!form) return;
    const data = Object.fromEntries(new FormData(form).entries());
    let sum = 0;
    Object.keys(data).filter(k => k.startsWith("weight_")).forEach(k => sum += parseFloat(String(data[k]).replace(',', '.') || 0));
    
    const warn = $("weightWarning");
    if (isNaN(sum) || Math.abs(sum - 1.0) > 0.001) { warn?.classList.remove('hidden'); return; }
    warn?.classList.add('hidden');

    fetch("calculate", { method: "POST", headers: {"Content-Type": "application/json"}, body: JSON.stringify(data) })
        .then(r => r.json()).then(res => { lastResults = res; displayResults(res); updatePairwiseComparison(); })
        .catch(e => console.error("Calc error", e));
}

/**
 * UI: Inject calculation results into tables
 */
function displayResults(res) {
    const rows = qsa("#resBody tr"); if (!rows.length || !res?.alternatives) return;
    const alts = res.alternatives, mat = res.matrix;
    const sorted = [...alts].sort((a, b) => b.phiNet - a.phiNet);
    const hCells = qsa("#resHeader th"), fCells = qsa("#resFooter td.res-phi-minus");

    alts.forEach((item, i) => {
        const r = rows[i]; if (!r) return;
        const nameInput = document.querySelector(`input[name="altName_${i+1}"]`);
        const name = (nameInput ? nameInput.value : "") || item.name;
        r.cells[0].innerText = name;
        if (hCells[i+1]) hCells[i+1].innerText = name;
        if (mat?.[i]) {
            for (let j = 0; j < alts.length; j++) {
                const c = r.cells[j+1];
                if (c?.classList.contains("res-pair-val")) c.innerText = i === j ? "\\" : Number(mat[i][j]).toFixed(4);
            }
        }
        r.cells[r.cells.length-3].innerText = Number(item.phiPlus).toFixed(4);
        r.cells[r.cells.length-2].innerText = Number(item.phiNet).toFixed(4);
        r.cells[r.cells.length-1].innerHTML = `<strong>${sorted.findIndex(s => s.phiNet === item.phiNet) + 1}</strong>`;
        if (fCells[i]) fCells[i].innerText = Number(item.phiMinus).toFixed(4);
    });

    updatePromethee1Matrix(alts);
}

/**
 * UI: Refresh alternative selection for pairwise comparison
 */
function updateDropdowns() {
    const a = $("compA"), b = $("compB"); if (!a || !b) return;
    const vA = a.value, vB = b.value;
    a.innerHTML = ''; b.innerHTML = '';
    for (let i = 1; i <= rowCount; i++) {
        const nameInput = document.querySelector(`input[name="altName_${i}"]`);
        const name = (nameInput ? nameInput.value : "") || `Alt ${i}`;
        a.add(new Option(name, i)); b.add(new Option(name, i));
    }
    a.value = (vA && vA <= rowCount) ? vA : 1;
    b.value = (vB && vB <= rowCount) ? vB : Math.min(2, rowCount);
    updatePairwiseComparison();
}

/**
 * MATH: Pure JS implementation of preference functions for live feedback
 */
function calculatePref(d, type, p, q, s) {
    if (d <= 0) return 0;
    p = parseFloat(p) || 0; q = parseFloat(q) || 0; s = parseFloat(s) || 0;
    switch (type) {
        case 'type1': return 1;
        case 'type2': return d > q ? 1 : 0;
        case 'type3': return p > 0 ? (d > p ? 1 : d / p) : (d > 0 ? 1 : 0);
        case 'type4': return d > p ? 1 : (d > q ? 0.5 : 0);
        case 'type5': return p <= q ? (d > p ? 1 : 0) : (d > p ? 1 : (d > q ? (d-q)/(p-q) : 0));
        case 'type6': return s <= 0 ? (d > 0 ? 1 : 0) : 1 - Math.exp(-Math.pow(d, 2) / (2 * Math.pow(s, 2)));
        default: return 0;
    }
}

/**
 * UI: Render PROMETHEE I pairwise table and global relation
 */
function updatePairwiseComparison() {
    const a = $("compA")?.value, b = $("compB")?.value, body = $("comparisonBody");
    if (!body || !a || !b) return;
    body.innerHTML = '';
    const data = Object.fromEntries(new FormData($("prometheeForm")).entries());
    for (let j = 1; j <= colCount; j++) {
        const nameInput = document.querySelector(`input[name="critName_${j}"]`);
        const name = (nameInput ? nameInput.value : "") || `Criterion ${j}`;
        const vA = parseFloat(String(data[`val_${a}_${j}`] || "0").replace(',', '.')) || 0;
        const vB = parseFloat(String(data[`val_${b}_${j}`] || "0").replace(',', '.')) || 0;
        const isMax = data[`isMax_${j}`] === "true", type = data[`func_${j}`];
        const p = parseFloat(String(data[`p_${j}`] || "0").replace(',', '.')) || 0;
        const q = parseFloat(String(data[`q_${j}`] || "0").replace(',', '.')) || 0;
        const s = parseFloat(String(data[`s_${j}`] || "0").replace(',', '.')) || 0;
        const dAB = isMax ? (vA - vB) : (vB - vA), dBA = isMax ? (vB - vA) : (vA - vB);
        const pAB = calculatePref(dAB, type, p, q, s), pBA = calculatePref(dBA, type, p, q, s);
        let c = 'status-equal', t = 'Indifference';
        if (pAB > pBA) { c = 'status-better'; t = 'A Better'; } else if (pBA > pAB) { c = 'status-worse'; t = 'B Better'; }
        const tr = document.createElement('tr');
        tr.innerHTML = `<td>${name}</td><td>${vA}</td><td>${vB}</td><td>${pAB.toFixed(4)}</td><td>${pBA.toFixed(4)}</td><td class="${c}">${t}</td>`;
        body.appendChild(tr);
    }
    if (lastResults?.alternatives) {
        const altA = lastResults.alternatives[a - 1], altB = lastResults.alternatives[b - 1];
        if (altA && altB) {
            const pP = altA.phiPlus > altB.phiPlus, pM = altA.phiMinus < altB.phiMinus;
            const eP = Math.abs(altA.phiPlus - altB.phiPlus) < 0.0001, eM = Math.abs(altA.phiMinus - altB.phiMinus) < 0.0001;

            let relation = "";
            let relColor = "";

            if (eP && eM) { 
                relation = "A I B (Indifference)";
            } else if ((pP && (pM || eM)) || (eP && pM)) { 
                relation = "A P B (A Preferred to B)"; 
            } else if (((altB.phiPlus > altA.phiPlus) && (altB.phiMinus < altA.phiMinus || eM)) || (eP && altB.phiMinus < altA.phiMinus)) { 
                relation = "B P A (B Preferred to A)";
            } else { 
                relation = "A R B (Incomparability)";
            }

            const foot = $("comparisonFooter");
            if (foot) {
                foot.innerHTML = `
                    <tr style="border-top: 2px solid #ccc;">
                        <td colspan="3" style="text-align: right; background: #f8f9fa;">Outgoing Flow (&#934;+) :</td>
                        <td style="color: ${pP ? '#155724' : (eP ? 'black' : '#721c24')}">&#934;+(A) = ${altA.phiPlus.toFixed(4)}</td>
                        <td style="color: ${!pP && !eP ? '#155724' : (eP ? 'black' : '#721c24')}">&#934;+(B) = ${altB.phiPlus.toFixed(4)}</td>
                        <td style="background: #f8f9fa; text-align: center;">${pP ? '&#934;+(A) > &#934;+(B)' : (eP ? '&#934;+(A) = &#934;+(B)' : '&#934;+(B) > &#934;+(A)')}</td>
                    </tr>
                    <tr>
                        <td colspan="3" style="text-align: right; background: #f8f9fa;">Incoming Flow (&#934;-) :</td>
                        <td style="color: ${pM ? '#155724' : (eM ? 'black' : '#721c24')}">&#934;-(A) = ${altA.phiMinus.toFixed(4)}</td>
                        <td style="color: ${!pM && !eM ? '#155724' : (eM ? 'black' : '#721c24')}">&#934;-(B) = ${altB.phiMinus.toFixed(4)}</td>
                        <td style="background: #f8f9fa; text-align: center;">${pM ? '&#934;-(A) < &#934;-(B)' : (eM ? '&#934;-(A) = &#934;-(B)' : '&#934;-(B) < &#934;-(A)')}</td>
                    </tr>
                    <tr style="font-weight: bold;">
                        <td colspan="3" style="text-align: right;">PROMETHEE I CONCLUSION :</td>
                        <td colspan="3" style="text-align: center; font-size: 1.1em;">${relation}</td>
                    </tr>
                `;
            }
        }
    }
}

/**
 * UI: Render PROMETHEE I Global Matrix
 */
function updatePromethee1Matrix(alts) {
    const h = $("p1MatrixHeader"), body = $("p1MatrixBody");
    if (!h || !body || !alts) return;

    h.innerHTML = '<th>Alternatives</th>';
    alts.forEach((alt, i) => {
        const th = document.createElement('th');
        const nameInput = document.querySelector(`input[name="altName_${i+1}"]`);
        th.innerText = (nameInput ? nameInput.value : "") || alt.name;
        h.appendChild(th);
    });

    body.innerHTML = '';
    alts.forEach((altA, i) => {
        const tr = document.createElement('tr');
        const tdName = document.createElement('td');
        const nameInput = document.querySelector(`input[name="altName_${i+1}"]`);
        tdName.innerText = (nameInput ? nameInput.value : "") || altA.name;
        tdName.style.fontWeight = "bold";
        tr.appendChild(tdName);

        alts.forEach((altB, j) => {
            const td = document.createElement('td');
            td.style.textAlign = "center";
            td.style.fontWeight = "bold";
            if (i === j) {
                td.innerText = '\\';
            } else {
                const pP = altA.phiPlus > altB.phiPlus;
                const pM = altA.phiMinus < altB.phiMinus;
                const eP = Math.abs(altA.phiPlus - altB.phiPlus) < 0.0001;
                const eM = Math.abs(altA.phiMinus - altB.phiMinus) < 0.0001;

                let relation = "";
                if (eP && eM) {
                    relation = "I";
                    td.style.color = "#000000"; // Black
                } else if ((pP && (pM || eM)) || (eP && pM)) {
                    relation = "P+";
                    td.style.color = "#155724"; // Green
                } else if (((altB.phiPlus > altA.phiPlus) && (altB.phiMinus < altA.phiMinus || eM)) || (eP && altB.phiMinus < altA.phiMinus)) {
                    relation = "P-";
                    td.style.color = "#721c24"; // Red
                } else {
                    relation = "R";
                    td.style.color = "#856404"; // Yellow/Orange
                }
                td.innerText = relation;
            }
            tr.appendChild(td);
        });
        body.appendChild(tr);
    });
}

/**
 * PERSISTENCE: Save state to localStorage
 */
const saveToLocalStorage = () => {
    const f = $("prometheeForm"); if (!f) return;
    localStorage.setItem('promethee_state', JSON.stringify({ colCount, rowCount, data: Object.fromEntries(new FormData(f).entries()) }));
};

/**
 * PERSISTENCE: Load state from localStorage
 */
function loadFromLocalStorage() {
    const s = localStorage.getItem('promethee_state');
    if (s) { try { applyData(JSON.parse(s)); } catch (e) { console.error(e); } } else if ($("prometheeForm")) $("prometheeForm").reset();
}

/**
 * PERSISTENCE: Reconstruct table from state object
 */
function applyData(imp) {
    if (!imp?.data) return;
    
    let safetyCounter = 0;
    while (colCount > 2 && safetyCounter < 50) { 
        const b = qsa("thead .btn-del"); 
        if (b.length) removeCriterion(b[b.length-1]); 
        else break; 
        safetyCounter++;
    }
    
    safetyCounter = 0;
    while (rowCount > 2 && safetyCounter < 50) { 
        const b = qsa("tbody .btn-del"); 
        if (b.length) removeAlternative(b[b.length-1]); 
        else break; 
        safetyCounter++;
    }

    while (colCount < imp.colCount) addCriterion();
    while (rowCount < imp.rowCount) addAlternative();
    
    const f = $("prometheeForm");
    Object.entries(imp.data).forEach(([k, v]) => {
        if (f.elements[k]) { 
            f.elements[k].value = v; 
            if (f.elements[k].tagName === 'SELECT' && k.startsWith('func_')) showParams(f.elements[k], k.split('_')[1]); 
        }
    });
    updateDropdowns(); sendData();
}

/**
 * ACTIONS: Clear all and reload
 */
const resetData = () => confirm("Reset all data?") && (localStorage.removeItem('promethee_state'), window.location.href = window.location.pathname + "?reset=" + Date.now());

/**
 * ACTIONS: Download state as JSON file
 */
function exportData() {
    const data = Object.fromEntries(new FormData($("prometheeForm")).entries());
    const a = document.createElement('a');
    a.href = URL.createObjectURL(new Blob([JSON.stringify({ colCount, rowCount, data }, null, 2)], { type: "application/json" }));
    a.download = `promethee_${new Date().toISOString().slice(0, 10)}.json`; a.click();
}

/**
 * ACTIONS: Upload state from JSON file
 */
const triggerImport = () => $("importFile").click();
function importData(e) {
    const f = e.target.files[0]; if (!f) return;
    const r = new FileReader(); r.onload = (ev) => { try { applyData(JSON.parse(ev.target.result)); alert("Imported!"); } catch (err) { alert("Error: " + err.message); } };
    r.readAsText(f); e.target.value = '';
}
