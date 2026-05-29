let colCount = 3; 
let rowCount = 3;
let debounceTimer;

// Event listeners
document.addEventListener('DOMContentLoaded', () => {
    const form = document.getElementById('prometheeForm');
    if (form) {
        form.addEventListener('input', () => {
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

    row.remove();
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
    
    const rows = document.querySelectorAll("tbody tr");
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
        }
    };
    xhr.send(JSON.stringify(data));

}

