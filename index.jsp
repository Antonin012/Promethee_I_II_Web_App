<%@ page pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html>
<head>
    <title>Promethée Application</title>
    <link rel="stylesheet" href="./css/sheet.css">
    <script src="./js/script.js" defer></script>
</head>
<body>

    <form id="prometheeForm">
        <table>
            <thead>
                <tr>
                    <th>Alternatives / Criteria</th>
                    <% for (int j = 1; j <= 3; j++) { %>
                    <th>
                        <div class="header-flex">
                            <input type="text" name="critName_<%=j%>" placeholder="Criteria Name <%=j%>">
                            <button type="button" onclick="removeCriterion(this)" class="btn-del" style="<%= (j < 3) ? "display:none;" : "" %>">-</button>
                        </div>
                        <input type="number" name="weight_<%=j%>" step="0.01" placeholder="Weight" class="input-weight"><br>
                        
                        <select name="func_<%=j%>" onchange="showParams(this, <%=j%>)">
                            <option value="type1">Type 1: Usuel</option>
                            <option value="type2">Type 2: U-Shape</option>
                            <option value="type3">Type 3: V-Shape</option>
                            <option value="type4">Type 4: Level</option>
                            <option value="type5">Type 5: V-Shape Indiff</option>
                            <option value="type6">Type 6: Gaussien</option>
                        </select>
    
                        <div id="q_div_<%=j%>" class="params">q: <input type="number" name="q_<%=j%>" step="0.1" class="input-param"></div>
                        <div id="p_div_<%=j%>" class="params">p: <input type="number" name="p_<%=j%>" step="0.1" class="input-param"></div>
                        <div id="s_div_<%=j%>" class="params">s: <input type="number" name="s_<%=j%>" step="0.1" class="input-param"></div>
    
                        <select name="isMax_<%=j%>">
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
                        <button type="button" onclick="removeAlternative(this)" class="btn-del" style="<%= (i < 3) ? "display:none;" : "" %>">-</button>
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

    <div id="resultsContainer"></div>

</body>
</html>