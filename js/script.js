let colCount = 3; 
let rowCount = 3;
let debounceTimer;
let lastResults = null;

// Event listeners
document.addEventListener('DOMContentLoaded', () => {
    const form = document.getElementById('prometheeForm');
    if (form) {
        form.addEventListener('input', (e) => {
            // Sync Alternative Name
            if (e.target.name && e.target.name.startsWith("altName_")) {
                const id = parseInt(e.target.name.split("_")[1]);
                const newName = e.target.value || `Alt ${id}`;
                
                // Update Results Table Headers
                const resRows = document.querySelectorAll("#resBody tr");
                if (resRows[id - 1]) resRows[id - 1].cells[0].innerText = newName;
                const resHeaderCells = document.querySelectorAll("#resHeader th");
                if (resHeaderCells[id]) resHeaderCells[id].innerText = newName;

                updateDropdowns();
            }
            debouncedSendData();
        });
    }
    updateButtons();
    loadFromLocalStorage();
    updateDropdowns();
});

function updateButtons() {
    const altBtns = document.querySelectorAll("tbody .btn-del");
    altBtns.forEach((btn, idx) => {
        btn.classList.toggle('hidden', !(altBtns.length > 2 && idx === altBtns.length - 1));
    });

    const critBtns = document.querySelectorAll("thead .btn-del");
    critBtns.forEach((btn, idx) => {
        btn.classList.toggle('hidden', !(critBtns.length > 2 && idx === critBtns.length - 1));
    });
}

function debouncedSendData() {
    clearTimeout(debounceTimer);
    debounceTimer = setTimeout(() => {
        saveToLocalStorage();
        sendData();
    }, 500);
}

function addCriterion() {
    colCount++;
    const headerRow = document.querySelector("#prometheeForm thead tr");
    const addColTh = headerRow.querySelector(".add-col-th");
    const newTh = document.createElement("th");
    
    newTh.innerHTML = `
        <div class="header-flex">
            <input type="text" name="critName_${colCount}" placeholder="Criteria Name ${colCount}">
            <button type="button" onclick="removeCriterion(this)" class="btn-del hidden">-</button>
        </div>
        <input type="number" name="weight_${colCount}" step="0.01" placeholder="Weight" class="input-weight"><br>
        <select name="func_${colCount}" onchange="showParams(this, ${colCount})">
            <option value="type1">Type 1: Usual</option>
            <option value="type2">Type 2: U-Shape</option>
            <option value="type3">Type 3: V-Shape</option>
            <option value="type4">Type 4: Level</option>
            <option value="type5">Type 5: V-Shape Indiff</option>
            <option value="type6">Type 6: Gaussian</option>
        </select>
        <div id="q_div_${colCount}" class="params">q: <input type="number" name="q_${colCount}" step="0.1" class="input-param"></div>
        <div id="p_div_${colCount}" class="params">p: <input type="number" name="p_${colCount}" step="0.1" class="input-param"></div>
        <div id="s_div_${colCount}" class="params">s: <input type="number" name="s_${colCount}" step="0.1" class="input-param"></div>
        <select name="isMax_${colCount}" onchange="debouncedSendData()">
            <option value="true">Higher +</option>
            <option value="false">Lower -</option>
        </select>
    `;
    headerRow.insertBefore(newTh, addColTh);

    const rows = document.querySelectorAll("#prometheeForm tbody tr");
    rows.forEach((row, index) => {
        const newTd = document.createElement("td");
        newTd.innerHTML = `<input type="number" name="val_${index + 1}_${colCount}" step="0.1" class="input-val">`;
        row.insertBefore(newTd, row.lastElementChild);
    });

    const footerRow = document.getElementById("add-alt-row");
    if (footerRow) {
        const emptyTd = document.createElement("td");
        footerRow.insertBefore(emptyTd, footerRow.lastElementChild);
    }
    
    updateButtons();
    updatePairwiseComparison();
}

function addAlternative() {
    rowCount++;
    const tbody = document.querySelector("#prometheeForm tbody");
    const newRow = document.createElement("tr");

    let tds = `<td>
        <button type="button" onclick="removeAlternative(this)" class="btn-del hidden">-</button>
        <input type="text" name="altName_${rowCount}" placeholder="Alternative ${rowCount}">
        </td>
        `;
    for (let j = 1; j <= colCount; j++) {
        tds += `<td><input type="number" name="val_${rowCount}_${j}" step="0.1" class="input-val"></td>`;
    }
    tds += `<td></td>`;
    
    newRow.innerHTML = tds;
    tbody.appendChild(newRow);

    // Sync Results Table
    const resHeader = document.getElementById("resHeader");
    const newResTh = document.createElement("th");
    newResTh.className = "res-alt-col";
    newResTh.innerText = `Alt ${rowCount}`;
    resHeader.insertBefore(newResTh, resHeader.cells[resHeader.cells.length - 3]);

    const resRows = document.querySelectorAll("#resBody tr");
    resRows.forEach((r) => {
        const newTd = document.createElement("td");
        newTd.className = "res-pair-val";
        newTd.innerText = "-";
        r.insertBefore(newTd, r.cells[r.cells.length - 3]);
    });

    const resBody = document.getElementById("resBody");
    const newResRow = document.createElement("tr");
    let resTds = `<td class="res-alt-row-name">Alt ${rowCount}</td>`;
    for (let j = 1; j <= rowCount; j++) {
        resTds += `<td class="res-pair-val">${(j === rowCount) ? "\\" : "-"}</td>`;
    }
    resTds += `<td class="res-phi-plus">-</td><td class="res-phi-net">-</td><td class="res-rank"><strong>-</strong></td>`;
    newResRow.innerHTML = resTds;
    resBody.appendChild(newResRow);

    const resFooter = document.getElementById("resFooter");
    const newFootTd = document.createElement("td");
    newFootTd.className = "res-phi-minus";
    newFootTd.innerText = "-";
    resFooter.insertBefore(newFootTd, resFooter.lastElementChild);

    updateButtons();
    updateDropdowns();
}

function showParams(select, id) {
    const type = select.value;
    const pDiv = document.getElementById('p_div_' + id);
    const qDiv = document.getElementById('q_div_' + id);
    const sDiv = document.getElementById('s_div_' + id);
    if (!pDiv || !qDiv || !sDiv) return;

    pDiv.classList.toggle('visible', ['type3', 'type4', 'type5'].includes(type));
    qDiv.classList.toggle('visible', ['type2', 'type4', 'type5'].includes(type));
    sDiv.classList.toggle('visible', type === 'type6');
    
    debouncedSendData();
}

function removeAlternative(btn) {
    if (rowCount <= 2) return;
    const row = btn.closest("tr");
    if (row !== row.parentNode.lastElementChild) return;

    const rowIndex = Array.from(row.parentNode.children).indexOf(row);
    row.remove();
    
    const resHeader = document.getElementById("resHeader");
    const colIndex = rowIndex + 1; 
    if (resHeader.cells[colIndex]) resHeader.cells[colIndex].remove();
    const resRows = document.querySelectorAll("#resBody tr");
    resRows.forEach(r => { if (r.cells[colIndex]) r.cells[colIndex].remove(); });
    if (resRows[rowIndex]) resRows[rowIndex].remove();
    const resFooter = document.getElementById("resFooter");
    if (resFooter.cells[colIndex]) resFooter.cells[colIndex].remove();

    rowCount--;
    updateButtons();
    updateDropdowns();
    saveToLocalStorage();
    sendData();
}

function removeCriterion(btn) {
    if (colCount <= 2) return;
    const th = btn.closest("th");
    const headerRow = th.parentNode;
    const criteriaThs = headerRow.querySelectorAll("th:not(.add-col-th)");
    if (th !== criteriaThs[criteriaThs.length - 1]) return;

    const index = Array.from(headerRow.children).indexOf(th);
    th.remove();
    const rows = document.querySelectorAll("#prometheeForm tbody tr");
    rows.forEach(row => { if (row.children[index]) row.children[index].remove(); });
    const footerRow = document.getElementById("add-alt-row");
    if (footerRow && footerRow.children[index]) footerRow.children[index].remove();

    colCount--;
    updateButtons();
    saveToLocalStorage();
    sendData(); 
}

async function sendData() {
    const form = document.getElementById('prometheeForm');
    if (!form) return;
    const formData = new FormData(form);
    const data = Object.fromEntries(formData.entries());
    
    let sumWeights = 0;
    for (let j = 1; j <= 100; j++) {
        let w = data[`weight_${j}`];
        if (w !== undefined) sumWeights += parseFloat(String(w).replace(',', '.') || 0);
    }
    
    const warningDiv = document.getElementById('weightWarning');
    if (isNaN(sumWeights) || Math.abs(sumWeights - 1.0) > 0.001) {
        if (warningDiv) warningDiv.classList.remove('hidden');
        console.log("Calculation blocked: Weight sum is " + sumWeights + " (must be 1.0)");
        return;
    } else {
        if (warningDiv) warningDiv.classList.add('hidden');
        console.log("Weight sum valid (1.0). Sending request...");
    }

    var xhr = new XMLHttpRequest();
    xhr.open("POST", "calculate", true);
    xhr.setRequestHeader("Content-Type", "application/json");
    xhr.onreadystatechange = function () {
        if (xhr.readyState === 4 && xhr.status === 200) {
            lastResults = JSON.parse(xhr.responseText);
            console.log("Incoming JSON :", lastResults);
            displayResults(lastResults);
            updatePairwiseComparison();
        }
    };
    const payload = JSON.stringify(data);
    console.log("Outgoing JSON :", data);
    xhr.send(payload);
}

function displayResults(results) {
    const resRows = document.querySelectorAll("#resBody tr");
    if (!resRows.length || !results || !results.alternatives) return;

    const alternatives = results.alternatives;
    const matrix = results.matrix;
    const sorted = [...alternatives].sort((a, b) => b.phiNet - a.phiNet);
    const resHeaderCells = document.querySelectorAll("#resHeader th");
    const resFooterCells = document.querySelectorAll("#resFooter td.res-phi-minus");

    alternatives.forEach((item, index) => {
        const row = resRows[index];
        if (!row) return;

        const altInput = document.querySelector(`input[name="altName_${index + 1}"]`);
        const altName = altInput ? altInput.value || `Alt ${index + 1}` : item.name;
        
        row.cells[0].innerText = altName;
        if (resHeaderCells[index + 1]) resHeaderCells[index + 1].innerText = altName;

        if (matrix && matrix[index]) {
            for (let j = 0; j < alternatives.length; j++) {
                const cell = row.cells[j + 1]; 
                if (cell && cell.classList.contains("res-pair-val")) {
                    cell.innerText = (index === j) ? "\\" : Number(matrix[index][j]).toFixed(4);
                }
            }
        }

        row.cells[row.cells.length - 3].innerText = Number(item.phiPlus).toFixed(4);
        row.cells[row.cells.length - 2].innerText = Number(item.phiNet).toFixed(4);
        row.cells[row.cells.length - 1].innerHTML = `<strong>${sorted.findIndex(s => s.phiNet === item.phiNet) + 1}</strong>`;

        if (resFooterCells[index]) resFooterCells[index].innerText = Number(item.phiMinus).toFixed(4);
    });
}

// PROMETHEE I Logic
function updateDropdowns() {
    const selA = document.getElementById('compA');
    const selB = document.getElementById('compB');
    if (!selA || !selB) return;

    const valA = selA.value;
    const valB = selB.value;

    selA.innerHTML = '';
    selB.innerHTML = '';

    for (let i = 1; i <= rowCount; i++) {
        const nameInput = document.querySelector(`input[name="altName_${i}"]`);
        const name = nameInput ? nameInput.value || `Alt ${i}` : `Alt ${i}`;
        const optA = new Option(name, i);
        const optB = new Option(name, i);
        selA.add(optA);
        selB.add(optB);
    }

    if (valA && valA <= rowCount) selA.value = valA; else selA.selectedIndex = 0;
    if (valB && valB <= rowCount) selB.value = valB; else selB.selectedIndex = Math.min(1, rowCount - 1);
    
    updatePairwiseComparison();
}

function calculatePref(d, type, p, q, s) {
    if (d <= 0) return 0;
    switch (type) {
        case 'type1': return 1;
        case 'type2': return d > q ? 1 : 0;
        case 'type3': return d > p ? 1 : d / p;
        case 'type4': return d > p ? 1 : (d > q ? 0.5 : 0);
        case 'type5': return d > p ? 1 : (d > q ? (d - q) / (p - q) : 0);
        case 'type6': return 1 - Math.exp(-Math.pow(d, 2) / (2 * Math.pow(s, 2)));
        default: return 0;
    }
}

function updatePairwiseComparison() {
    const idA = document.getElementById('compA').value;
    const idB = document.getElementById('compB').value;
    const tbody = document.getElementById('comparisonBody');
    const relationDiv = document.getElementById('prometheeIRelation');
    if (!tbody || !idA || !idB) return;

    tbody.innerHTML = '';
    const form = document.getElementById('prometheeForm');
    const data = Object.fromEntries(new FormData(form).entries());

    for (let j = 1; j <= colCount; j++) {
        const name = data[`critName_${j}`] || `Criterion ${j}`;
        const valA = parseFloat(data[`val_${idA}_${j}`] || 0);
        const valB = parseFloat(data[`val_${idB}_${j}`] || 0);
        const isMax = data[`isMax_${j}`] === "true";
        const type = data[`func_${j}`];
        const p = parseFloat(data[`p_${j}`] || 0);
        const q = parseFloat(data[`q_${j}`] || 0);
        const s = parseFloat(data[`s_${j}`] || 0);

        const diffAB = isMax ? (valA - valB) : (valB - valA);
        const diffBA = isMax ? (valB - valA) : (valA - valB);

        const prefAB = calculatePref(diffAB, type, p, q, s);
        const prefBA = calculatePref(diffBA, type, p, q, s);

        let statusClass = 'status-equal';
        let statusText = 'Indifference';
        if (prefAB > prefBA) { statusClass = 'status-better'; statusText = 'A is Better'; }
        else if (prefBA > prefAB) { statusClass = 'status-worse'; statusText = 'B is Better'; }

        const tr = document.createElement('tr');
        tr.innerHTML = `
            <td>${name}</td>
            <td>${valA}</td>
            <td>${valB}</td>
            <td>${prefAB.toFixed(4)}</td>
            <td>${prefBA.toFixed(4)}</td>
            <td class="${statusClass}">${statusText}</td>
        `;
        tbody.appendChild(tr);
    }

    // Global PROMETHEE I Relation
    if (lastResults && lastResults.alternatives) {
        const altA = lastResults.alternatives[idA - 1];
        const altB = lastResults.alternatives[idB - 1];
        if (altA && altB) {
            const pPlus = altA.phiPlus > altB.phiPlus;
            const pMinus = altA.phiMinus < altB.phiMinus;
            const ePlus = Math.abs(altA.phiPlus - altB.phiPlus) < 0.0001;
            const eMinus = Math.abs(altA.phiMinus - altB.phiMinus) < 0.0001;

            let relation = "";
            if (ePlus && eMinus) relation = "A I B (Indifference)";
            else if ((pPlus && (pMinus || eMinus)) || (ePlus && pMinus)) relation = "A P B (A Preferred to B)";
            else if (((altB.phiPlus > altA.phiPlus) && (altB.phiMinus < altA.phiMinus || eMinus)) || (ePlus && altB.phiMinus < altA.phiMinus)) relation = "B P A (B Preferred to A)";
            else relation = "A R B (Incomparability)";
            
            relationDiv.innerText = "Global Relation: " + relation;
        }
    }
}

function saveToLocalStorage() {
    const form = document.getElementById('prometheeForm');
    if (!form) return;
    const data = Object.fromEntries(new FormData(form).entries());
    localStorage.setItem('promethee_state', JSON.stringify({ colCount, rowCount, data }));
}

function loadFromLocalStorage() {
    const saved = localStorage.getItem('promethee_state');
    if (saved) {
        try {
            const state = JSON.parse(saved);
            applyData(state);
        } catch (e) { console.error("Error loading", e); }
    }
}

function applyData(imported) {
    if (!imported || !imported.data) return;
    while (colCount > 2) { const b = document.querySelectorAll("thead .btn-del"); if (b.length > 0) removeCriterion(b[b.length-1]); else break; }
    while (rowCount > 2) { const b = document.querySelectorAll("tbody .btn-del"); if (b.length > 0) removeAlternative(b[b.length-1]); else break; }
    while (colCount < imported.colCount) addCriterion();
    while (rowCount < imported.rowCount) addAlternative();
    const form = document.getElementById('prometheeForm');
    for (const [key, value] of Object.entries(imported.data)) {
        const input = form.elements[key];
        if (input) {
            input.value = value;
            if (input.tagName === 'SELECT' && key.startsWith('func_')) showParams(input, key.split('_')[1]);
        }
    }
    updateDropdowns();
    sendData();
}

function resetData() {
    if (confirm("Do you really want to reset all data?")) {
        localStorage.removeItem('promethee_state');
        location.reload();
    }
}

function exportData() {
    const form = document.getElementById('prometheeForm');
    const data = Object.fromEntries(new FormData(form).entries());
    const blob = new Blob([JSON.stringify({ colCount, rowCount, data }, null, 2)], { type: "application/json" });
    const a = document.createElement('a');
    a.href = URL.createObjectURL(blob);
    a.download = `promethee_${new Date().toISOString().slice(0, 10)}.json`;
    a.click();
}

function triggerImport() { document.getElementById('importFile').click(); }

function importData(event) {
    const file = event.target.files[0];
    if (!file) return;
    const reader = new FileReader();
    reader.onload = (e) => {
        try { applyData(JSON.parse(e.target.result)); alert("Import successful!"); }
        catch (err) { alert("Import error: " + err.message); }
    };
    reader.readAsText(file);
    event.target.value = ''; 
}
