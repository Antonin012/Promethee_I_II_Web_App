let colCount = 3; 
let rowCount = 3;
let debounceTimer;

// Event listeners
document.addEventListener('DOMContentLoaded', () => {
    const form = document.getElementById('prometheeForm');
    if (form) {
        form.addEventListener('input', (e) => {
            // Sync Alternative Name to Results Table (Row and Column)
            if (e.target.name && e.target.name.startsWith("altName_")) {
                const id = parseInt(e.target.name.split("_")[1]);
                const newName = e.target.value || `Alt ${id}`;
                
                // Update Row Header
                const resRows = document.querySelectorAll("#resBody tr");
                if (resRows[id - 1]) {
                    resRows[id - 1].cells[0].innerText = newName;
                }
                
                // Update Column Header
                const resHeaderCells = document.querySelectorAll("#resHeader th");
                if (resHeaderCells[id]) {
                    resHeaderCells[id].innerText = newName;
                }
            }
            debouncedSendData();
        });
    }
    updateButtons();
    loadFromLocalStorage();
});

function updateButtons() {
    const altBtns = document.querySelectorAll("tbody .btn-del");
    altBtns.forEach((btn, idx) => {
        if (altBtns.length > 2 && idx === altBtns.length - 1) {
            btn.style.display = "inline-block";
        } else {
            btn.style.display = "none";
        }
    });

    const critBtns = document.querySelectorAll("thead .btn-del");
    critBtns.forEach((btn, idx) => {
        if (critBtns.length > 2 && idx === critBtns.length - 1) {
            btn.style.display = "inline-block";
        } else {
            btn.style.display = "none";
        }
    });
}

function debouncedSendData() {
    clearTimeout(debounceTimer);
    debounceTimer = setTimeout(() => {
        saveToLocalStorage();
        sendData();
    }, 500);
}

// Add column
function addCriterion() {
    colCount++;
    const headerRow = document.querySelector("#prometheeForm thead tr");
    const addColTh = headerRow.querySelector(".add-col-th");
    const newTh = document.createElement("th");
    
    newTh.innerHTML = `
        <div class="header-flex">
            <input type="text" name="critName_${colCount}" placeholder="Nom Critère ${colCount}">
            <button type="button" onclick="removeCriterion(this)" class="btn-del" style="display:none;">-</button>
        </div>
        <input type="number" name="weight_${colCount}" step="0.01" placeholder="Poids" class="input-weight"><br>
        <select name="func_${colCount}" onchange="showParams(this, ${colCount})">
            <option value="type1">Type 1: Usuel</option>
            <option value="type2">Type 2: U-Shape</option>
            <option value="type3">Type 3: V-Shape</option>
            <option value="type4">Type 4: Level</option>
            <option value="type5">Type 5: V-Shape Indiff.</option>
            <option value="type6">Type 6: Gaussien</option>
        </select>
        <div id="q_div_${colCount}" class="params">q: <input type="number" name="q_${colCount}" step="0.1" class="input-param"></div>
        <div id="p_div_${colCount}" class="params">p: <input type="number" name="p_${colCount}" step="0.1" class="input-param"></div>
        <div id="s_div_${colCount}" class="params">s: <input type="number" name="s_${colCount}" step="0.1" class="input-param"></div>
        <select name="isMax_${colCount}">
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
}

// Add row
function addAlternative() {
    rowCount++;
    const tbody = document.querySelector("#prometheeForm tbody");
    const newRow = document.createElement("tr");

    let tds = `<td>
        <button type="button" onclick="removeAlternative(this)" class="btn-del" style="display:none;">-</button>
        <input type="text" name="altName_${rowCount}" placeholder="Alternative ${rowCount}">
        </td>
        `;
    for (let j = 1; j <= colCount; j++) {
        tds += `<td><input type="number" name="val_${rowCount}_${j}" step="0.1" class="input-val"></td>`;
    }
    tds += `<td></td>`;
    
    newRow.innerHTML = tds;
    tbody.appendChild(newRow);

    // Sync Results Table (Square Matrix)
    const resHeader = document.getElementById("resHeader");
    const newResTh = document.createElement("th");
    newResTh.className = "res-alt-col";
    newResTh.innerText = `Alt ${rowCount}`;
    const phiPlusTh = resHeader.cells[resHeader.cells.length - 3];
    resHeader.insertBefore(newResTh, phiPlusTh);

    const resRows = document.querySelectorAll("#resBody tr");
    resRows.forEach((r) => {
        const newTd = document.createElement("td");
        newTd.className = "res-pair-val";
        newTd.innerText = "-";
        const phiPlusTd = r.cells[r.cells.length - 3];
        r.insertBefore(newTd, phiPlusTd);
    });

    const resBody = document.getElementById("resBody");
    const newResRow = document.createElement("tr");
    let resTds = `<td class="res-alt-row-name">Alt ${rowCount}</td>`;
    for (let j = 1; j <= rowCount; j++) {
        resTds += `<td class="res-pair-val">${(j === rowCount) ? "\\" : "-"}</td>`;
    }
    resTds += `<td class="res-phi-plus">-</td>
               <td class="res-phi-net">-</td>
               <td class="res-rank"><strong>-</strong></td>`;
    newResRow.innerHTML = resTds;
    resBody.appendChild(newResRow);

    const resFooter = document.getElementById("resFooter");
    const newFootTd = document.createElement("td");
    newFootTd.className = "res-phi-minus";
    newFootTd.innerText = "-";
    resFooter.insertBefore(newFootTd, resFooter.lastElementChild);

    updateButtons();
}

// Set visibility of each type
function showParams(select, id) {
    const type = select.value;
    const pDiv = document.getElementById('p_div_' + id);
    const qDiv = document.getElementById('q_div_' + id);
    const sDiv = document.getElementById('s_div_' + id);

    if (!pDiv || !qDiv || !sDiv) return;

    pDiv.classList.remove('visible');
    qDiv.classList.remove('visible');
    sDiv.classList.remove('visible');

    if (type === 'type2') qDiv.classList.add('visible');
    if (type === 'type3') pDiv.classList.add('visible');
    if (type === 'type4' || type === 'type5') {
        pDiv.classList.add('visible');
        qDiv.classList.add('visible');
    }
    if (type === 'type6') sDiv.classList.add('visible');
}

// Remove Row (Alternative)
function removeAlternative(btn) {
    if (rowCount <= 2) return;
    const row = btn.closest("tr");
    if (row !== row.parentNode.lastElementChild) return;

    const rowIndex = Array.from(row.parentNode.children).indexOf(row);
    row.remove();
    
    // Sync Results Table (Square Matrix)
    const resHeader = document.getElementById("resHeader");
    const colIndex = rowIndex + 1; 
    if (resHeader.cells[colIndex]) resHeader.cells[colIndex].remove();

    const resRows = document.querySelectorAll("#resBody tr");
    resRows.forEach(r => {
        if (r.cells[colIndex]) r.cells[colIndex].remove();
    });

    if (resRows[rowIndex]) resRows[rowIndex].remove();

    const resFooter = document.getElementById("resFooter");
    if (resFooter.cells[colIndex]) resFooter.cells[colIndex].remove();

    rowCount--;
    updateButtons();
}

// Remove Column (Criterion)
function removeCriterion(btn) {
    if (colCount <= 2) return;
    const th = btn.closest("th");
    const headerRow = th.parentNode;
    const criteriaThs = headerRow.querySelectorAll("th:not(.add-col-th)");
    if (th !== criteriaThs[criteriaThs.length - 1]) return;

    const index = Array.from(headerRow.children).indexOf(th);
    th.remove();

    const rows = document.querySelectorAll("#prometheeForm tbody tr");
    rows.forEach(row => {
        if (row.children[index]) {
            row.children[index].remove();
        }
    });

    const footerRow = document.getElementById("add-alt-row");
    if (footerRow && footerRow.children[index]) {
        footerRow.children[index].remove();
    }

    colCount--;
    updateButtons();
}

async function sendData() {
    const form = document.getElementById('prometheeForm');
    if (!form) return;
    
    // Check weights
    const formData = new FormData(form);
    const data = Object.fromEntries(formData.entries());
    
    let sumWeights = 0;
    for (let j = 1; j <= 100; j++) { // Use a reasonable limit
        const w = data[`weight_${j}`];
        if (w !== undefined) {
            sumWeights += parseFloat(w || 0);
        }
    }
    
    const warningDiv = document.getElementById('weightWarning');
    // Allow a small margin for floating point precision
    if (Math.abs(sumWeights - 1.0) > 0.001) {
        if (warningDiv) warningDiv.classList.remove('hidden');
        return; // Block calculation
    } else {
        if (warningDiv) warningDiv.classList.add('hidden');
    }

    var xhr = new XMLHttpRequest();
    var url = "calculate"; 
    xhr.open("POST", url, true);
    xhr.setRequestHeader("Content-Type", "application/json");
    xhr.onreadystatechange = function () {
        if (xhr.readyState === 4 && xhr.status === 200) {
            var results = JSON.parse(xhr.responseText);
            displayResults(results);
        }
    };
    xhr.send(JSON.stringify(data));
}

function displayResults(results) {
    const resRows = document.querySelectorAll("#resBody tr");
    if (!resRows.length) return;

    if (!results || !results.alternatives || !Array.isArray(results.alternatives) || results.alternatives.length === 0) {
        return;
    }

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
        if (resHeaderCells[index + 1]) {
            resHeaderCells[index + 1].innerText = altName;
        }

        if (matrix && matrix[index]) {
            for (let j = 0; j < alternatives.length; j++) {
                const cell = row.cells[j + 1]; 
                if (cell && cell.classList.contains("res-pair-val")) {
                    if (index === j) {
                        cell.innerText = "\\";
                    } else {
                        cell.innerText = Number(matrix[index][j]).toFixed(4);
                    }
                }
            }
        }

        const phiPlusCell = row.cells[row.cells.length - 3];
        const phiNetCell = row.cells[row.cells.length - 2];
        const rankCell = row.cells[row.cells.length - 1];

        phiPlusCell.innerText = Number(item.phiPlus).toFixed(4);
        phiNetCell.innerText = Number(item.phiNet).toFixed(4);
        
        const rank = sorted.findIndex(s => s.phiNet === item.phiNet) + 1;
        rankCell.innerHTML = `<strong>${rank}</strong>`;

        if (resFooterCells[index]) {
            resFooterCells[index].innerText = Number(item.phiMinus).toFixed(4);
        }
    });
}

function saveToLocalStorage() {
    const form = document.getElementById('prometheeForm');
    if (!form) return;
    const formData = new FormData(form);
    const data = Object.fromEntries(formData.entries());
    const state = {
        colCount: colCount,
        rowCount: rowCount,
        data: data
    };
    localStorage.setItem('promethee_state', JSON.stringify(state));
}

function loadFromLocalStorage() {
    const saved = localStorage.getItem('promethee_state');
    if (saved) {
        try {
            const state = JSON.parse(saved);
            applyData(state);
        } catch (e) {
            console.error("Error loading from localStorage", e);
        }
    }
}

function applyData(imported) {
    if (!imported || !imported.data) return;

    while (colCount > 2) {
        const btns = document.querySelectorAll("thead .btn-del");
        if (btns.length > 0) removeCriterion(btns[btns.length-1]);
        else break;
    }
    while (rowCount > 2) {
        const btns = document.querySelectorAll("tbody .btn-del");
        if (btns.length > 0) removeAlternative(btns[btns.length-1]);
        else break;
    }

    while (colCount < imported.colCount) addCriterion();
    while (rowCount < imported.rowCount) addAlternative();
    
    const form = document.getElementById('prometheeForm');
    if (!form) return;
    for (const [key, value] of Object.entries(imported.data)) {
        const input = form.elements[key];
        if (input) {
            input.value = value;
            if (input.tagName === 'SELECT' && key.startsWith('func_')) {
                const id = key.split('_')[1];
                showParams(input, id);
            }
        }
    }
    sendData();
}

function resetData() {
    if (confirm("Do you really want to reset all the data ?")) {
        localStorage.removeItem('promethee_state');
        location.reload();
    }
}

function exportData() {
    const form = document.getElementById('prometheeForm');
    if (!form) return;
    const formData = new FormData(form);
    const data = Object.fromEntries(formData.entries());
    const exportObj = {
        colCount: colCount,
        rowCount: rowCount,
        data: data
    };
    
    const blob = new Blob([JSON.stringify(exportObj, null, 2)], { type: "application/json" });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `promethee_data_${new Date().toISOString().slice(0, 10)}.json`;
    a.click();
    URL.revokeObjectURL(url);
}

function triggerImport() {
    const input = document.getElementById('importFile');
    if (input) input.click();
}

function importData(event) {
    const file = event.target.files[0];
    if (!file) return;

    const reader = new FileReader();
    reader.onload = function(e) {
        try {
            const imported = JSON.parse(e.target.result);
            applyData(imported);
            alert("Imported data succed !");
        } catch (err) {
            alert("Error importation : " + err.message);
        }
    };
    reader.readAsText(file);
    event.target.value = ''; 
}
