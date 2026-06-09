// Initial counts for criteria (columns) and alternatives (rows)
let colCount = 3; 
let rowCount = 3;

// Timer for debouncing API requests
let debounceTimer;

// Stores the most recent calculation results from the server
let lastResults = null;

// Tracks the state of the sidebar menu
let sidebarOpen = false;

/**
 * Toggles the visibility of the sidebar menu and adjusts the main content margin.
 */
function toggleSidebar() {
    sidebarOpen = !sidebarOpen;
    const sidebar = document.getElementById("mySidebar");
    const mainContent = document.getElementById("main-content");
    if (sidebarOpen) {
        sidebar.style.width = "250px";
        mainContent.style.marginLeft = "250px";
    } else {
        sidebar.style.width = "0";
        mainContent.style.marginLeft = "0";
    }
}

// Event listeners to set up the page once the DOM is fully loaded
document.addEventListener('DOMContentLoaded', () => {
    const form = document.getElementById('prometheeForm');
    if (form) {
        // Listen for input changes to dynamically update tables and trigger calculation
        form.addEventListener('input', (e) => {
            // Update alternative names dynamically in the results table
            if (e.target.name && e.target.name.startsWith("altName_")) {
                const id = parseInt(e.target.name.split("_")[1]);
                const newName = e.target.value || `Alt ${id}`;
                
                // Update row headers
                const resRows = document.querySelectorAll("#resBody tr");
                if (resRows[id - 1]) resRows[id - 1].cells[0].innerText = newName;
                
                // Update column headers
                const resHeaderCells = document.querySelectorAll("#resHeader th");
                if (resHeaderCells[id]) resHeaderCells[id].innerText = newName;
                
                // Update comparison dropdowns
                updateDropdowns();
            }
            // Trigger calculation after a short delay
            debouncedSendData();
        });
    }
    
    // Initialize the UI elements and load saved data
    updateButtons();
    loadFromLocalStorage();
    updateDropdowns();
    fetchSessionsList(); // Load available sessions from the database
});

/**
 * Fetches the list of saved sessions from the API and populates the session selector dropdown.
 */
function fetchSessionsList() {
    var xhr = new XMLHttpRequest();
    xhr.open("GET", "api/sessions", true);
    xhr.onreadystatechange = function () {
        if (xhr.readyState === 4 && xhr.status === 200) {
            try {
                var sessions = JSON.parse(xhr.responseText);
                var selector = document.getElementById("sessionSelector");
                if (selector) {
                    selector.innerHTML = '<option value="">-- Select a saved session --</option>';
                    sessions.forEach(s => {
                        var opt = document.createElement('option');
                        opt.value = s.id;
                        var date = new Date(s.createdAt).toLocaleString();
                        opt.text = s.name + " (" + date + ")";
                        selector.appendChild(opt);
                    });
                }
            } catch (e) {
                console.error("Error parsing sessions list:", e);
            }
        }
    };
    xhr.send();
}

/**
 * Loads a specific session from the database based on the selected ID in the dropdown.
 * Fetches the data, applies it to the UI, and alerts the user.
 */
function loadSessionFromDB() {
    var selector = document.getElementById("sessionSelector");
    var id = selector.value;
    if (!id) return;
    
    // Close sidebar to give a better view of the loaded data
    if (sidebarOpen) toggleSidebar();
    
    var xhr = new XMLHttpRequest();
    xhr.open("GET", "api/sessions?id=" + id, true);
    xhr.onreadystatechange = function () {
        if (xhr.readyState === 4) {
            if (xhr.status === 200) {
                try {
                    var sessionData = JSON.parse(xhr.responseText);
                    applyData(sessionData); // Map data back to form
                    // Set the session name input to match the loaded session (removing the date part)
                    document.getElementById("sessionNameInput").value = selector.options[selector.selectedIndex].text.split(" (")[0];
                    alert("Session loaded successfully!");
                } catch (e) {
                    alert("Error parsing session data.");
                }
            } else {
                alert("Failed to load session.");
            }
        }
    };
    xhr.send();
}

/**
 * Gathers the current form data and sends it to the API to save the session in the database.
 * Validates the session name and checks that the sum of criteria weights equals 1.0.
 */
function saveToDatabase() {
    var sessionName = document.getElementById("sessionNameInput").value.trim();
    if (!sessionName) {
        alert("Please enter a Session Name before saving.");
        return;
    }
    
    const form = document.getElementById('prometheeForm');
    if (!form) return;
    const formData = new FormData(form);
    const data = Object.fromEntries(formData.entries());
    
    // Validate that the sum of weights is approximately 1.0
    let sumWeights = 0;
    for (let j = 1; j <= 100; j++) {
        let w = data[`weight_${j}`];
        if (w !== undefined) sumWeights += parseFloat(String(w).replace(',', '.') || 0);
    }
    if (isNaN(sumWeights) || Math.abs(sumWeights - 1.0) > 0.001) {
        alert("Cannot save: The sum of the weights must be equal to 1.0.");
        return;
    }

    // Prepare payload
    const exportObj = {
        sessionName: sessionName,
        data: data
    };
    
    var xhr = new XMLHttpRequest();
    xhr.open("POST", "api/sessions", true);
    xhr.setRequestHeader("Content-Type", "application/json");
    xhr.onreadystatechange = function () {
        if (xhr.readyState === 4) {
            if (xhr.status === 200) {
                alert("Session saved successfully!");
                fetchSessionsList(); // Refresh the list after successful save
            } else {
                alert("Failed to save session. Server returned: " + xhr.responseText);
            }
        }
    };
    xhr.send(JSON.stringify(exportObj));
}

/**
 * Clears local storage and reloads the page to start a completely new session.
 */
function createNewSession() {
    if (confirm("Create a new calculation? Unsaved changes will be lost.")) {
        localStorage.removeItem('promethee_state');
        window.location.href = window.location.pathname + "?reset=" + new Date().getTime();
    }
}

/**
 * Updates the visibility of delete buttons (minus signs) for rows and columns.
 * Ensures that at least 2 alternatives and 2 criteria remain.
 */
function updateButtons() {
    const altBtns = document.querySelectorAll("tbody .btn-del");
    altBtns.forEach((btn, idx) => {
        // Show delete button only for the last alternative if there are more than 2
        btn.classList.toggle('hidden', !(altBtns.length > 2 && idx === altBtns.length - 1));
    });

    const critBtns = document.querySelectorAll("thead .btn-del");
    critBtns.forEach((btn, idx) => {
        // Show delete button only for the last criterion if there are more than 2
        btn.classList.toggle('hidden', !(critBtns.length > 2 && idx === critBtns.length - 1));
    });
}

/**
 * Debounces the save and send actions to prevent sending too many requests 
 * during rapid typing in input fields.
 */
function debouncedSendData() {
    clearTimeout(debounceTimer);
    debounceTimer = setTimeout(() => {
        saveToLocalStorage();
        sendData();
    }, 500);
}

/**
 * Adds a new criterion (column) to the table.
 * Generates the necessary input fields for name, weight, preference function, and parameters.
 */
function addCriterion() {
    colCount++;
    const headerRow = document.querySelector("#prometheeForm thead tr");
    const addColTh = headerRow.querySelector(".add-col-th");
    const newTh = document.createElement("th");
    
    // Inject HTML for the new criterion header
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

    // Add corresponding evaluation cells to all existing alternative rows
    const rows = document.querySelectorAll("#prometheeForm tbody tr");
    rows.forEach((row, index) => {
        const newTd = document.createElement("td");
        newTd.innerHTML = `<input type="number" name="val_${index + 1}_${colCount}" step="0.1" class="input-val">`;
        row.insertBefore(newTd, row.lastElementChild);
    });

    // Add empty cell to the footer row (where the 'Add Alternative' button is)
    const footerRow = document.getElementById("add-alt-row");
    if (footerRow) {
        const emptyTd = document.createElement("td");
        footerRow.insertBefore(emptyTd, footerRow.lastElementChild);
    }
    
    updateButtons();
    updatePairwiseComparison();
}

/**
 * Adds a new alternative (row) to the main data table and updates the results table to match.
 */
function addAlternative() {
    rowCount++;
    const tbody = document.querySelector("#prometheeForm tbody");
    const newRow = document.createElement("tr");

    // Build the row HTML starting with the name field
    let tds = `<td>
        <button type="button" onclick="removeAlternative(this)" class="btn-del hidden">-</button>
        <input type="text" name="altName_${rowCount}" placeholder="Alternative ${rowCount}">
        </td>
        `;
    // Add evaluation inputs for each criterion
    for (let j = 1; j <= colCount; j++) {
        tds += `<td><input type="number" name="val_${rowCount}_${j}" step="0.1" class="input-val"></td>`;
    }
    tds += `<td></td>`; // Empty trailing cell for layout
    
    newRow.innerHTML = tds;
    tbody.appendChild(newRow);

    // Synchronize the Results Table by adding a new column
    const resHeader = document.getElementById("resHeader");
    const newResTh = document.createElement("th");
    newResTh.className = "res-alt-col";
    newResTh.innerText = `Alt ${rowCount}`;
    resHeader.insertBefore(newResTh, resHeader.cells[resHeader.cells.length - 3]);

    // Insert new cells into existing results rows
    const resRows = document.querySelectorAll("#resBody tr");
    resRows.forEach((r) => {
        const newTd = document.createElement("td");
        newTd.className = "res-pair-val";
        newTd.innerText = "-";
        r.insertBefore(newTd, r.cells[r.cells.length - 3]);
    });

    // Add a new row to the Results Table for the new alternative
    const resBody = document.getElementById("resBody");
    const newResRow = document.createElement("tr");
    let resTds = `<td class="res-alt-row-name">Alt ${rowCount}</td>`;
    for (let j = 1; j <= rowCount; j++) {
        resTds += `<td class="res-pair-val">${(j === rowCount) ? "\\" : "-"}</td>`;
    }
    resTds += `<td class="res-phi-plus">-</td><td class="res-phi-net">-</td><td class="res-rank"><strong>-</strong></td>`;
    newResRow.innerHTML = resTds;
    resBody.appendChild(newResRow);

    // Update the footer row of the results table
    const resFooter = document.getElementById("resFooter");
    const newFootTd = document.createElement("td");
    newFootTd.className = "res-phi-minus";
    newFootTd.innerText = "-";
    resFooter.insertBefore(newFootTd, resFooter.lastElementChild);

    updateButtons();
    updateDropdowns();
}

/**
 * Toggles the visibility of threshold parameter inputs (p, q, s) 
 * depending on the selected preference function type.
 * 
 * @param {HTMLElement} select The select element triggering the change
 * @param {number} id The numerical ID of the criterion column
 */
function showParams(select, id) {
    const type = select.value;
    const pDiv = document.getElementById('p_div_' + id);
    const qDiv = document.getElementById('q_div_' + id);
    const sDiv = document.getElementById('s_div_' + id);
    if (!pDiv || !qDiv || !sDiv) return;

    // Show/hide based on logic required by specific preference function types
    pDiv.classList.toggle('visible', ['type3', 'type4', 'type5'].includes(type));
    qDiv.classList.toggle('visible', ['type2', 'type4', 'type5'].includes(type));
    sDiv.classList.toggle('visible', type === 'type6');
    
    // Automatically trigger calculation after changing function type
    debouncedSendData();
}

/**
 * Removes an alternative (row) from both the main table and the results table.
 * 
 * @param {HTMLElement} btn The delete button that was clicked
 */
function removeAlternative(btn) {
    if (rowCount <= 2) return; // Enforce minimum of 2 alternatives
    const row = btn.closest("tr");
    if (row !== row.parentNode.lastElementChild) return; // Only allow removing the last row

    const rowIndex = Array.from(row.parentNode.children).indexOf(row);
    row.remove();
    
    // Remove the corresponding column and row from the Results Table
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
    sendData(); // Recalculate without the deleted alternative
}

/**
 * Removes a criterion (column) from the main table.
 * 
 * @param {HTMLElement} btn The delete button that was clicked
 */
function removeCriterion(btn) {
    if (colCount <= 2) return; // Enforce minimum of 2 criteria
    const th = btn.closest("th");
    const headerRow = th.parentNode;
    const criteriaThs = headerRow.querySelectorAll("th:not(.add-col-th)");
    if (th !== criteriaThs[criteriaThs.length - 1]) return; // Only allow removing the last column

    const index = Array.from(headerRow.children).indexOf(th);
    th.remove();
    
    // Remove the corresponding cells from every data row
    const rows = document.querySelectorAll("#prometheeForm tbody tr");
    rows.forEach(row => { if (row.children[index]) row.children[index].remove(); });
    
    // Remove from the footer row
    const footerRow = document.getElementById("add-alt-row");
    if (footerRow && footerRow.children[index]) footerRow.children[index].remove();

    colCount--;
    updateButtons();
    saveToLocalStorage();
    sendData(); // Recalculate without the deleted criterion
}

/**
 * Serializes form data and sends it to the calculation backend endpoint.
 * Also verifies that the sum of criteria weights is equal to 1.0 before proceeding.
 */
async function sendData() {
    const form = document.getElementById('prometheeForm');
    if (!form) return;
    const formData = new FormData(form);
    const data = Object.fromEntries(formData.entries());
    
    // Validate weight sum
    let sumWeights = 0;
    for (let j = 1; j <= 100; j++) {
        let w = data[`weight_${j}`];
        if (w !== undefined) sumWeights += parseFloat(String(w).replace(',', '.') || 0);
    }
    
    const warningDiv = document.getElementById('weightWarning');
    if (isNaN(sumWeights) || Math.abs(sumWeights - 1.0) > 0.001) {
        if (warningDiv) warningDiv.classList.remove('hidden');
        console.log("Calculation blocked: Weight sum is " + sumWeights + " (must be 1.0)");
        return; // Abort calculation if weights are invalid
    } else {
        if (warningDiv) warningDiv.classList.add('hidden');
        console.log("Weight sum valid (1.0). Sending request...");
    }

    var xhr = new XMLHttpRequest();
    xhr.open("POST", "calculate", true);
    xhr.setRequestHeader("Content-Type", "application/json");
    xhr.onreadystatechange = function () {
        if (xhr.readyState === 4 && xhr.status === 200) {
            // Process and display calculation results from backend
            lastResults = JSON.parse(xhr.responseText);
            console.log("Incoming JSON :", lastResults);
            displayResults(lastResults);
            updatePairwiseComparison(); // Refresh the manual comparison view
        }
    };
    
    const payload = JSON.stringify(data);
    console.log("Outgoing JSON :", data);
    xhr.send(payload);
}

/**
 * Injects the calculation results (matrix, flows, and ranking) into the Results Table.
 * 
 * @param {Object} results The JSON object containing alternatives and global matrix data
 */
function displayResults(results) {
    const resRows = document.querySelectorAll("#resBody tr");
    if (!resRows.length || !results || !results.alternatives) return;

    const alternatives = results.alternatives;
    const matrix = results.matrix;
    
    // Sort alternatives by net flow (PhiNet) descending to determine rank
    const sorted = [...alternatives].sort((a, b) => b.phiNet - a.phiNet);
    
    const resHeaderCells = document.querySelectorAll("#resHeader th");
    const resFooterCells = document.querySelectorAll("#resFooter td.res-phi-minus");

    // Loop through alternatives and inject data into corresponding rows and columns
    alternatives.forEach((item, index) => {
        const row = resRows[index];
        if (!row) return;

        const altInput = document.querySelector(`input[name="altName_${index + 1}"]`);
        const altName = altInput ? altInput.value || `Alt ${index + 1}` : item.name;
        
        row.cells[0].innerText = altName;
        if (resHeaderCells[index + 1]) resHeaderCells[index + 1].innerText = altName;

        // Populate pairwise matrix values
        if (matrix && matrix[index]) {
            for (let j = 0; j < alternatives.length; j++) {
                const cell = row.cells[j + 1]; 
                if (cell && cell.classList.contains("res-pair-val")) {
                    cell.innerText = (index === j) ? "\\" : Number(matrix[index][j]).toFixed(4);
                }
            }
        }

        // Display positive and net flows, and calculated rank
        row.cells[row.cells.length - 3].innerText = Number(item.phiPlus).toFixed(4);
        row.cells[row.cells.length - 2].innerText = Number(item.phiNet).toFixed(4);
        row.cells[row.cells.length - 1].innerHTML = `<strong>${sorted.findIndex(s => s.phiNet === item.phiNet) + 1}</strong>`;

        // Display negative flows in the table footer
        if (resFooterCells[index]) resFooterCells[index].innerText = Number(item.phiMinus).toFixed(4);
    });
}

/**
 * Updates the dropdown menus used for manual pairwise comparison of alternatives.
 */
function updateDropdowns() {
    const selA = document.getElementById('compA');
    const selB = document.getElementById('compB');
    if (!selA || !selB) return;

    const valA = selA.value;
    const valB = selB.value;

    selA.innerHTML = '';
    selB.innerHTML = '';

    // Populate options with current alternative names
    for (let i = 1; i <= rowCount; i++) {
        const nameInput = document.querySelector(`input[name="altName_${i}"]`);
        const name = nameInput ? nameInput.value || `Alt ${i}` : `Alt ${i}`;
        const optA = new Option(name, i);
        const optB = new Option(name, i);
        selA.add(optA);
        selB.add(optB);
    }

    // Retain previous selections if they are still valid
    if (valA && valA <= rowCount) selA.value = valA; else selA.selectedIndex = 0;
    if (valB && valB <= rowCount) selB.value = valB; else selB.selectedIndex = Math.min(1, rowCount - 1);
    
    updatePairwiseComparison();
}

/**
 * Replicates the preference function calculation on the frontend 
 * to provide live feedback for pairwise comparison details.
 * 
 * @param {number} d The numerical difference between two evaluations
 * @param {string} type The preference function type identifier
 * @param {number} p Strict preference threshold
 * @param {number} q Indifference threshold
 * @param {number} s Gaussian standard deviation threshold
 * @return {number} The computed preference degree [0, 1]
 */
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

/**
 * Updates the pairwise comparison section showing detailed preference degrees
 * criterion by criterion for the two currently selected alternatives.
 */
function updatePairwiseComparison() {
    const idA = document.getElementById('compA').value;
    const idB = document.getElementById('compB').value;
    const tbody = document.getElementById('comparisonBody');
    const relationDiv = document.getElementById('prometheeIRelation');
    if (!tbody || !idA || !idB) return;

    tbody.innerHTML = '';
    const form = document.getElementById('prometheeForm');
    const data = Object.fromEntries(new FormData(form).entries());

    // Iterate through all criteria to calculate frontend preference details
    for (let j = 1; j <= colCount; j++) {
        const name = data[`critName_${j}`] || `Criterion ${j}`;
        const valA = parseFloat(data[`val_${idA}_${j}`] || 0);
        const valB = parseFloat(data[`val_${idB}_${j}`] || 0);
        const isMax = data[`isMax_${j}`] === "true";
        const type = data[`func_${j}`];
        const p = parseFloat(data[`p_${j}`] || 0);
        const q = parseFloat(data[`q_${j}`] || 0);
        const s = parseFloat(data[`s_${j}`] || 0);

        // Adjust difference calculation based on optimization direction
        const diffAB = isMax ? (valA - valB) : (valB - valA);
        const diffBA = isMax ? (valB - valA) : (valA - valB);

        const prefAB = calculatePref(diffAB, type, p, q, s);
        const prefBA = calculatePref(diffBA, type, p, q, s);

        // Determine winner for UI styling
        let statusClass = 'status-equal';
        let statusText = 'Indifference';
        if (prefAB > prefBA) { statusClass = 'status-better'; statusText = 'A is Better'; }
        else if (prefBA > prefAB) { statusClass = 'status-worse'; statusText = 'B is Better'; }

        // Create table row for criteria detail
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

    // Determine and display Global PROMETHEE I outranking relation
    if (lastResults && lastResults.alternatives) {
        const altA = lastResults.alternatives[idA - 1];
        const altB = lastResults.alternatives[idB - 1];
        if (altA && altB) {
            const pPlus = altA.phiPlus > altB.phiPlus;
            const pMinus = altA.phiMinus < altB.phiMinus;
            const ePlus = Math.abs(altA.phiPlus - altB.phiPlus) < 0.0001;
            const eMinus = Math.abs(altA.phiMinus - altB.phiMinus) < 0.0001;

            let relation = "";
            // Rules determining Indifference, Strict Preference, or Incomparability
            if (ePlus && eMinus) relation = "A I B (Indifference)";
            else if ((pPlus && (pMinus || eMinus)) || (ePlus && pMinus)) relation = "A P B (A Preferred to B)";
            else if (((altB.phiPlus > altA.phiPlus) && (altB.phiMinus < altA.phiMinus || eMinus)) || (ePlus && altB.phiMinus < altA.phiMinus)) relation = "B P A (B Preferred to A)";
            else relation = "A R B (Incomparability)";
            
            relationDiv.innerText = "Global Relation: " + relation;
        }
    }
}

/**
 * Serializes and saves the current form state to local browser storage 
 * so it survives page reloads.
 */
function saveToLocalStorage() {
    const form = document.getElementById('prometheeForm');
    if (!form) return;
    const data = Object.fromEntries(new FormData(form).entries());
    localStorage.setItem('promethee_state', JSON.stringify({ colCount, rowCount, data }));
}

/**
 * Loads and applies data from local browser storage on initialization.
 */
function loadFromLocalStorage() {
    const saved = localStorage.getItem('promethee_state');
    if (saved) {
        try {
            const state = JSON.parse(saved);
            applyData(state);
        } catch (e) { console.error("Error loading", e); }
    } else {
        // If no state exists (e.g. after a reset), make sure the form is completely empty.
        const form = document.getElementById('prometheeForm');
        if (form) form.reset();
    }
}

/**
 * Populates the UI data table and triggers a recalculation using a provided JSON state object.
 * Adjusts column and row counts to match the incoming data.
 * 
 * @param {Object} imported The parsed state object containing colCount, rowCount, and form data
 */
function applyData(imported) {
    if (!imported || !imported.data) return;
    
    // Reset columns and rows to the minimum (2x2) before expanding to match imported data
    while (colCount > 2) { const b = document.querySelectorAll("thead .btn-del"); if (b.length > 0) removeCriterion(b[b.length-1]); else break; }
    while (rowCount > 2) { const b = document.querySelectorAll("tbody .btn-del"); if (b.length > 0) removeAlternative(b[b.length-1]); else break; }
    
    // Reconstruct columns and rows
    while (colCount < imported.colCount) addCriterion();
    while (rowCount < imported.rowCount) addAlternative();
    
    const form = document.getElementById('prometheeForm');
    // Populate form inputs
    for (const [key, value] of Object.entries(imported.data)) {
        const input = form.elements[key];
        if (input) {
            input.value = value;
            // Handle conditional parameter visibility if function type changes
            if (input.tagName === 'SELECT' && key.startsWith('func_')) showParams(input, key.split('_')[1]);
        }
    }
    updateDropdowns();
    sendData(); // Recalculate based on newly loaded data
}

/**
 * Prompts user to confirm resetting all current data. 
 * If confirmed, clears local storage and reloads the page.
 */
function resetData() {
    if (confirm("Do you really want to reset all data?")) {
        localStorage.removeItem('promethee_state');
        window.location.href = window.location.pathname + "?reset=" + new Date().getTime();
    }
}

/**
 * Packages the current state into a JSON Blob and triggers a file download for the user.
 */
function exportData() {
    const form = document.getElementById('prometheeForm');
    const data = Object.fromEntries(new FormData(form).entries());
    const blob = new Blob([JSON.stringify({ colCount, rowCount, data }, null, 2)], { type: "application/json" });
    const a = document.createElement('a');
    a.href = URL.createObjectURL(blob);
    a.download = `promethee_${new Date().toISOString().slice(0, 10)}.json`;
    a.click();
}

/**
 * Clicks the hidden file input element to open the file selection dialog.
 */
function triggerImport() { document.getElementById('importFile').click(); }

/**
 * Handles the file input change event to load and parse a JSON file, 
 * applying its content to the UI if successful.
 * 
 * @param {Event} event The file selection event
 */
function importData(event) {
    const file = event.target.files[0];
    if (!file) return;
    const reader = new FileReader();
    reader.onload = (e) => {
        try { 
            applyData(JSON.parse(e.target.result)); 
            alert("Import successful!"); 
        } catch (err) { 
            alert("Import error: " + err.message); 
        }
    };
    reader.readAsText(file);
    event.target.value = ''; // Reset input so the same file can be selected again if needed
}
