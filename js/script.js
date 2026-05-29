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
});

function updateButtons() {
    // For alternatives (rows)
    const altBtns = document.querySelectorAll("tbody .btn-del");
    altBtns.forEach((btn, idx) => {
        if (altBtns.length > 2 && idx === altBtns.length - 1) {
            btn.style.display = "inline-block";
        } else {
            btn.style.display = "none";
        }
    });

    // For criteria (columns)
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
        sendData();
    }, 500);
}

// Add column
function addCriterion() {
    colCount++;
    const headerRow = document.querySelector("thead tr");
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

    const rows = document.querySelectorAll("tbody tr");
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
    const tbody = document.querySelector("tbody");
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
    // Add Column to Header
    const resHeader = document.getElementById("resHeader");
    const newResTh = document.createElement("th");
    newResTh.className = "res-alt-col";
    newResTh.innerText = `Alt ${rowCount}`;
    const phiPlusTh = resHeader.cells[resHeader.cells.length - 3];
    resHeader.insertBefore(newResTh, phiPlusTh);

    // Add Cell to existing rows in resBody
    const resRows = document.querySelectorAll("#resBody tr");
    resRows.forEach((r, idx) => {
        const newTd = document.createElement("td");
        newTd.className = "res-pair-val";
        newTd.innerText = "-";
        const phiPlusTd = r.cells[r.cells.length - 3];
        r.insertBefore(newTd, phiPlusTd);
    });

    // Add new Row to resBody
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

    // Add Cell to resFooter
    const resFooter = document.getElementById("resFooter");
    const newFootTd = document.createElement("td");
    newFootTd.className = "res-phi-minus";
    newFootTd.innerText = "-";
    // Insert before the colspan=3 cell
    resFooter.insertBefore(newFootTd, resFooter.lastElementChild);

    updateButtons();
}

// Set visibility of each type
function showParams(select, id) {
    const type = select.value;
    const pDiv = document.getElementById('p_div_' + id);
    const qDiv = document.getElementById('q_div_' + id);
    const sDiv = document.getElementById('s_div_' + id);

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
    // Remove column from header
    const resHeader = document.getElementById("resHeader");
    const colIndex = rowIndex + 1; // +1 because cells[0] is title
    if (resHeader.cells[colIndex]) resHeader.cells[colIndex].remove();

    // Remove column from each row in body
    const resRows = document.querySelectorAll("#resBody tr");
    resRows.forEach(r => {
        if (r.cells[colIndex]) r.cells[colIndex].remove();
    });

    // Remove the row itself
    if (resRows[rowIndex]) resRows[rowIndex].remove();

    // Remove cell from footer
    const resFooter = document.getElementById("resFooter");
    if (resFooter.cells[colIndex]) resFooter.cells[colIndex].remove();

    rowCount--;
    updateButtons();
    sendData();
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

    // Sync Results Table Header
    const resHeaderRow = document.querySelector("#resultsTable thead tr");
    if (resHeaderRow.cells[index]) {
        resHeaderRow.cells[index].remove();
    }
    
    const rows = document.querySelectorAll("tbody tr");
    const resRows = document.querySelectorAll("#resultsTable tbody tr");

    rows.forEach((row, rowIndex) => {
        if (row.children[index]) {
            row.children[index].remove();
        }
        // Sync Results Table Body
        if (resRows[rowIndex] && resRows[rowIndex].cells[index]) {
            resRows[rowIndex].cells[index].remove();
        }
    });

    const footerRow = document.getElementById("add-alt-row");
    if (footerRow && footerRow.children[index]) {
        footerRow.children[index].remove();
    }

    colCount--;
    updateButtons();
    sendData(); 
}

async function sendData() {
    const formData = new FormData(document.getElementById('prometheeForm'));
    const data = Object.fromEntries(formData.entries());
    console.log("Sended data :", data);

    // Sending and receiving data in JSON format using POST method
    var xhr = new XMLHttpRequest();
    var url = "calculate"; // URL relative au contexte actuel
    xhr.open("POST", url, true);
    xhr.setRequestHeader("Content-Type", "application/json");
    xhr.onreadystatechange = function () {
        if (xhr.readyState === 4 && xhr.status === 200) {
            var results = JSON.parse(xhr.responseText);
            console.log("Results:", results);
            displayResults(results);
        }
    };
    xhr.send(JSON.stringify(data));
}

function displayResults(results) {
    const resRows = document.querySelectorAll("#resBody tr");
    if (!resRows.length) return;

    if (!results || !Array.isArray(results) || results.length === 0) {
        return;
    }

    const sorted = [...results].sort((a, b) => b.phiNet - a.phiNet);

    const resHeaderCells = document.querySelectorAll("#resHeader th");
    const resFooterCells = document.querySelectorAll("#resFooter td.res-phi-minus");

    results.forEach((item, index) => {
        const row = resRows[index];
        if (!row) return;

        // Sync Alternative Name from Input (Row and Column Header)
        const altInput = document.querySelector(`input[name="altName_${index + 1}"]`);
        const altName = altInput ? altInput.value || `Alt ${index + 1}` : item.name;
        
        row.cells[0].innerText = altName;
        if (resHeaderCells[index + 1]) {
            resHeaderCells[index + 1].innerText = altName;
        }

        // Update Flows (at the end of the row)
        const phiPlusCell = row.cells[row.cells.length - 3];
        const phiNetCell = row.cells[row.cells.length - 2];
        const rankCell = row.cells[row.cells.length - 1];

        phiPlusCell.innerText = Number(item.phiPlus).toFixed(4);
        phiNetCell.innerText = Number(item.phiNet).toFixed(4);
        
        const rank = sorted.findIndex(s => s.phiNet === item.phiNet) + 1;
        rankCell.innerHTML = `<strong>${rank}</strong>`;

        // Update Phi Minus in the Footer
        if (resFooterCells[index]) {
            resFooterCells[index].innerText = Number(item.phiMinus).toFixed(4);
        }
    });
}

