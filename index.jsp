<%@ page pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html>
<head>
    <title>Promethée Application</title>
    <link rel="stylesheet" href="./css/sheet.css">
    <script src="https://cdn.jsdelivr.net/npm/chart.js"></script>
    <script src="./js/script.js" defer></script>
</head>
<body>

    <div id="mySidebar" class="sidebar">
        <a href="javascript:void(0)" class="closebtn" onclick="toggleSidebar()">&times;</a>
        <h2>Sessions</h2>
        <div class="sidebar-content">
            <label for="sessionSelector" class="sidebar-label">Load Calculation:</label>
            <select id="sessionSelector" class="sidebar-select" onchange="loadSessionFromDB()">
                <option value="">-- Select a saved session --</option>
            </select>
            <button type="button" onclick="createNewSession()" class="btn-action btn-new">New Calculation</button>
        </div>
    </div>

    <div id="main-content">
        <div class="top-bar">
            <button class="openbtn" onclick="toggleSidebar()">&#9776; Sessions Menu</button>
        </div>

        <form id="prometheeForm">
        <table>
            <thead>
                <tr>
                    <th>Alternatives / Criteria</th>
                    <% for (int j = 1; j <= 3; j++) { %>
                    <th>
                        <div class="header-flex">
                            <input type="text" name="critName_<%=j%>" placeholder="Criteria Name <%=j%>">
                            <button type="button" onclick="removeCriterion(this)" class="btn-del <%= (j < 3) ? "hidden" : "" %>">-</button>
                        </div>
                        <input type="number" name="weight_<%=j%>" step="0.01" placeholder="Weight" class="input-weight"><br>
                        
                        <select name="func_<%=j%>" onchange="showParams(this, <%=j%>)">
                            <option value="type1">Type 1: Usual</option>
                            <option value="type2">Type 2: U-Shape</option>
                            <option value="type3">Type 3: V-Shape</option>
                            <option value="type4">Type 4: Level</option>
                            <option value="type5">Type 5: V-Shape Indiff</option>
                            <option value="type6">Type 6: Gaussian</option>
                        </select>
    
                        <div id="q_div_<%=j%>" class="params">q: <input type="number" name="q_<%=j%>" step="0.1" class="input-param"></div>
                        <div id="p_div_<%=j%>" class="params">p: <input type="number" name="p_<%=j%>" step="0.1" class="input-param"></div>
                        <div id="s_div_<%=j%>" class="params">s: <input type="number" name="s_<%=j%>" step="0.1" class="input-param"></div>
    
                        <select name="isMax_<%=j%>" onchange="debouncedSendData()">
                            <option value="true">Higher +</option>
                            <option value="false">Lower -</option>
                        </select>
                    </th>
                    <% } %>
                    <th class="add-col-th">
                        <button type="button" onclick="addCriterion()" class="btn-add-col">+</button>
                    </th>
                </tr>
            </thead>
            <tbody>
                <% for (int i = 1; i <= 3; i++) { %>
                <tr>
                    <td>
                        <button type="button" onclick="removeAlternative(this)" class="btn-del <%= (i < 3) ? "hidden" : "" %>">-</button>
                        <input type="text" name="altName_<%=i%>" placeholder="Alternative <%=i%>">
                    </td>
                    <% for (int j = 1; j <= 3; j++) { %>
                    <td><input type="number" name="val_<%=i%>_<%=j%>" step="0.1" class="input-val"></td>
                    <% } %>
                    <td></td>
                </tr>
                <% } %>
            </tbody>
            <tfoot>
                <tr id="add-alt-row">
                    <td class="add-row-td">
                        <button type="button" onclick="addAlternative()" class="btn-add-row">+</button>
                    </td>
                    <% for (int j = 1; j <= 3; j++) { %>
                    <td></td>
                    <% } %>
                    <td></td>
                </tr>
            </tfoot>
        </table>
    </form>

    <div class="actions-bar">
        <button type="button" onclick="resetData()" class="btn-action btn-reset">Reset</button>
        <div class="actions-right">
            <input type="text" id="sessionNameInput" placeholder="Session Name" class="input-session-name">
            <button type="button" onclick="saveToDatabase()" class="btn-action btn-save">Save to Database</button>
            <button type="button" onclick="exportData()" class="btn-action">Export JSON</button>
            <button type="button" onclick="triggerImport()" class="btn-action">Import JSON</button>
            <input type="file" id="importFile" class="hidden" onchange="importData(event)">
        </div>
    </div>

    <div id="weightWarning" class="warning-msg hidden">
        Warning : The sum of the weights must be equal to 1.0 to perform the calculation.
    </div>

    <div id="comparisonSection" class="section-container">
        <h2>Pairwise Comparison (PROMETHEE I Analysis)</h2>
        <div class="comp-selectors">
            Alternative A: <select id="compA" class="select-comp" onchange="updatePairwiseComparison()"></select>
            Alternative B: <select id="compB" class="select-comp" onchange="updatePairwiseComparison()"></select>
        </div>
        <table id="comparisonTable">
            <thead>
                <tr>
                    <th>Criterion</th>
                    <th id="thValueA">Value A</th>
                    <th id="thValueB">Value B</th>
                    <th id="thPrefAB">P(A, B)</th>
                    <th id="thPrefBA">P(B, A)</th>
                    <th>Status</th>
                </tr>
            </thead>
            <tbody id="comparisonBody">
                <!-- Filled by JS -->
            </tbody>
            <tfoot id="comparisonFooter" class="comparison-footer">
                <!-- Filled by JS -->
            </tfoot>
        </table>
    </div>

    <div id="promethee1MatrixContainer" class="section-container">
        <h2>PROMETHEE I Global Relation Matrix</h2>
        <div class="legend-container">
            <strong>Legend:</strong> 
            <span class="legend-p-plus">P</span> (Row Preferred to Column) | 
            <span class="legend-p-minus">~P</span> (Column Preferred to Row) | 
            <span class="legend-indiff">I</span> (Indifference) | 
            <span class="legend-incomp">R</span> (Incomparability)
        </div>
        <table id="promethee1MatrixTable">
            <thead>
                <tr id="p1MatrixHeader">
                    <th>Alternatives</th>
                </tr>
            </thead>
            <tbody id="p1MatrixBody">
            </tbody>
        </table>
    </div>

    <div id="gaiaSection" class="section-container">
        <h2>GAIA Plane Visualization</h2>
        <p id="gaiaVariance" style="text-align: center; font-style: italic; color: #666;"></p>
        <div style="position: relative; width: 100%; max-width: 800px; margin: 0 auto;">
            <canvas id="gaiaChart" width="800" height="600"></canvas>
        </div>
    </div>

    <div id="resultsContainer">
        <h2>Final Results Matrix (PROMETHEE II)</h2>
        <table id="resultsTable">
            <thead>
                <tr id="resHeader">
                    <th>Alternatives</th>
                    <% for (int j = 1; j <= 3; j++) { %>
                    <th class="res-alt-col">Alt <%=j%></th>
                    <% } %>
                    <th>Φ+</th>
                    <th>Φ (Net)</th>
                    <th>Rank</th>
                </tr>
            </thead>
            <tbody id="resBody">
                <% for (int i = 1; i <= 3; i++) { %>
                <tr>
                    <td class="res-alt-row-name">Alt <%=i%></td>
                    <% for (int j = 1; j <= 3; j++) { %>
                    <td class="res-pair-val"><%= (i == j) ? "\\" : "-" %></td>
                    <% } %>
                    <td class="res-phi-plus">-</td>
                    <td class="res-phi-net">-</td>
                    <td class="res-rank"><strong>-</strong></td>
                </tr>
                <% } %>
            </tbody>
            <tfoot>
                <tr id="resFooter">
                    <th>Φ-</th>
                    <% for (int j = 1; j <= 3; j++) { %>
                    <td class="res-phi-minus">-</td>
                    <% } %>
                    <td colspan="3" class="res-footer-empty"></td>
                </tr>
            </tfoot>
        </table>
    </div>

    </div> <!-- End main-content -->

</body>
</html>
