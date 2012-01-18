function load_dbs() {
    Ext.Ajax.request({
        url: "/addama/datasources",
        method: "GET",
        success: function(o) {
            var json = Ext.util.JSON.decode(o.responseText);
            if (json && json.numberOfItems) {
                var html = "";
                for (var i = 0; i < json.items.length; i++) {
                    var database = json.items[i];
                    html += "<li>";
                    html += "<a onclick='load_tables(\"" + database.uri + "\"); return false;' href='#'>" + database.label + "</a>";
                    html += "</li>";
                }
                Ext.getDom("container_datasources").innerHTML = "<ul>" + html + "</ul>";
            }

            Ext.getDom("container_errors").innerHTML += "GET /addama/datasources<br/>";
        },
        failure: displayFailure
    });
}

function load_tables(databaseUri) {
    Ext.getDom("container_errors").innerHTML = "GET " + databaseUri;
    Ext.Ajax.request({
        url: databaseUri,
        method: "GET",
        success: function(o) {
            var json = Ext.util.JSON.decode(o.responseText);
            if (json && json.numberOfItems) {
                var html = "";
                for (var i = 0; i < json.items.length; i++) {
                    var table = json.items[i];
                    html += "<li>";
                    html += "<a onclick='select_table(\"" + table.uri + "\"); return false;' href='#'>" + table.name + "</a>";
                    html += "</li>";
                }

                Ext.getDom("container_tables").innerHTML = "<ul>" + html + "</ul>";
            }

            Ext.getDom("container_errors").innerHTML += "GET /addama/datasources<br/>";
        },
        failure: displayFailure
    });
}

function select_table(tableUri) {
    Ext.getDom("container_selected_datasource").innerHTML = tableUri;
    Ext.getDom("container_columns").innerHTML = "";
    Ext.getDom("container_preview").innerHTML = "";
    Ext.getDom("container_errors").innerHTML = "";
    Ext.getDom("container_numberOfResults").innerHTML = "";
    Ext.getDom("container_errors").innerHTML = "GET " + tableUri;

    loadColumns(tableUri, function(columns) {
        var html = "";
        for (var i = 0; i < columns.length; i++) {
            var column = columns[i];
            html += "<li>";
            html += "<strong>" + column.name + "</strong>: " + column.datatype;
            html += "</li>";
        }

        Ext.getDom("container_columns").innerHTML = "<ul>" + html + "</ul>";
    });
}

function ready_to_query() {
    Ext.getDom("container_preview").innerHTML = "";
    Ext.getDom("container_errors").innerHTML = "";
    Ext.getDom("container_numberOfResults").innerHTML = "";

    if (!Ext.getDom("container_selected_datasource").innerHTML) {
        Ext.getDom("container_errors").innerHTML = "no table selected";
        return false;
    }

    if (!Ext.getDom("textarea_sql").value) {
        Ext.getDom("container_errors").innerHTML = "no SQL entered. Defaulting to SELECT *";
        return true;
    }

    return true;
}

function export_csv() {
    if (!ready_to_query()) {
        return;
    }

    var tableUri = Ext.getDom("container_selected_datasource").innerHTML;
    var querySql = Ext.getDom("textarea_sql").value;
    document.location = tableUri + "/query?tq=" + querySql + "&tqx=reqId:123;out:csv;outFileName:results";
}

function export_tsv() {
    if (!ready_to_query()) {
        return;
    }

    var tableUri = Ext.getDom("container_selected_datasource").innerHTML;
    var querySql = Ext.getDom("textarea_sql").value;
    document.location = tableUri + "/query?tq=" + querySql + "&tqx=reqId:123;out:tsv-excel;outFileName:results.tsv";
}

function execute_html() {
    if (!ready_to_query()) {
        return;
    }

    var tableUri = Ext.getDom("container_selected_datasource").innerHTML;
    var querySql = Ext.getDom("textarea_sql").value;

    Ext.getDom("container_errors").innerHTML = "GET " + tableUri + "/query<br/>Query=" + querySql;

    queryTableUri(tableUri, querySql, function(columns, data) {
        Ext.getDom("container_numberOfResults").innerHTML = data.length;

        var fields = [];
        for (var i = 0; i < columns.length; i++) {
            var column = columns[i];
            fields[fields.length] = column.name;
            column.id = column.name;
            column.header = column.name;
            column.dataIndex = column.name;
            column.sortable = true;
        }

        var store = new Ext.data.JsonStore({
            storeId : 'arrayStore',
            autoDestroy: true,
            idProperty: fields[0],
            root : 'results',
            fields: fields
        });

        store.loadData({ results: data });

        var grid = new Ext.grid.GridPanel({
            store: store,
            colModel: new Ext.grid.ColumnModel({
                defaults: {
                    width: 200,
                    sortable: true
                },
                columns: columns
            }),
            viewConfig: {
                forceFit: true
            },
            stripeRows: true,
            width: 600,
            height: 350,
            frame: true,
            title: 'Results',
            iconCls: 'icon-grid'
        });
        grid.render("container_preview");
    });
}

function queryTableUri(tableUri, sql, callback) {
    loadColumns(tableUri, function(columns) {
        Ext.Ajax.request({
            url: tableUri + "/query",
            method: "GET",
            params: {
                tq: sql,
                tqx: "out:json_array"
            },
            success: function(o) {
                var json = Ext.util.JSON.decode(o.responseText);
                if (json) {
                    callback(columns, json);
                }
            },
            failure: displayFailure
        });
    });
}

function loadColumns(tableUri, callback) {
    Ext.Ajax.request({
        url: tableUri,
        method: "GET",
        success: function(o) {
            var json = Ext.util.JSON.decode(o.responseText);
            if (json && json.items) {
                callback(json.items);
            }
        },
        failure: displayFailure
    });
}

function displayFailure(o) {
    Ext.getDom("container_errors").innerHTML = o.statusText + " (" + o.status + ")";
}
