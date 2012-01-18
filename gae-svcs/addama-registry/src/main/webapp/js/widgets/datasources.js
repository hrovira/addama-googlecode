Ext.ns("org.systemsbiology.addama.js");

org.systemsbiology.addama.js.DatasourcesView = Ext.extend(Object, {
    constructor: function(config) {
        Ext.apply(this, config);

        org.systemsbiology.addama.js.DatasourcesView.superclass.constructor.call(this);

        this.loadTree();
        this.loadDatabases();
    },

    loadTree: function() {
        this.treePanel = new Ext.tree.TreePanel({
            title: "Datasources",
            region:"west",
            split: true,
            minSize: 150,
            autoScroll: true,
            border: true,
            margins: "5 0 5 5",
            width: 275,
            maxSize: 500,
            // tree-specific configs:
            rootVisible: false,
            lines: false,
            singleExpand: true,
            useArrows: true,
            loader: new Ext.tree.TreeLoader(),
            root: new Ext.tree.AsyncTreeNode()
        });
        this.treePanel.on("expandnode", this.expandNode, this);
        this.treePanel.on("click", this.selectTable, this);

        var dataPanel = {
            margins: "5 5 5 0",
            layout: "border",
            region: "center",
            border: true,
            split: true,
            items:[
                {
                    title: "Query", region: "north", contentEl: "container_sql", width:600,
                    bbar: [
                        { text: "Show Results", handler: this.queryHtml },
                        { text: "Export to CSV", handler: this.queryCsv },
                        { text: "Export to TSV", handler: this.queryTsv }

                    ]
                },
                { title: "Results", region: "south", contentEl: "container_preview", width:810, height: 400, colspan:2, autoScroll: true }
            ]
        };

        this.mainPanel = new Ext.Panel({
            title: "Main",
            contentEl: this.contentEl,
            margins: "5 5 5 0",
            layout: "border",
            border: true,
            split: true,
            items:[
                this.treePanel,
                dataPanel
            ]
        });
    },

    loadDatabases: function() {
        Ext.Ajax.request({
            url: "/addama/datasources",
            method: "GET",
            success: function(o) {
                var json = Ext.util.JSON.decode(o.responseText);
                if (json && json.items) {
                    Ext.each(json.items, function(database) {
                        // TODO : Add data to tree
                    });
                }
            },
            failure: this.displayFailure,
            scope: this
        });
    },

    loadTables: function(databaseUri) {
        Ext.Ajax.request({
            url: databaseUri,
            method: "GET",
            success: function(o) {
                var json = Ext.util.JSON.decode(o.responseText);
                if (json && json.items) {
                    Ext.each(json.items, function(table) {
                        // TODO : Add table to tree
                    });
                }
            },
            failure: this.displayFailure,
            scope: this
        });
    },

    loadColumns: function(tableUri) {
        Ext.Ajax.request({
            url: tableUri,
            method: "GET",
            success: function(o) {
                var json = Ext.util.JSON.decode(o.responseText);
                if (json && json.items) {
                    Ext.each(json.items, function(table) {
                        // TODO : Add columns to tree:  column.name, column.datatype
                    });
                }
            },
            failure: this.displayFailure,
            scope: this
        });
    },

    displayFailure: function(o) {
        org.systemsbiology.addama.js.Message.error("Datasources", "Error Loading: " + o.statusText);
    },

    selectTable: function(node) {
        if (node.attributes.isTable) {
            this.selectedTable = node;
        }
    },

    queryHtml: function() {
        if (!this.isReadyToQuery()) {
            return;
        }

        var tableUri = this.selectedTable.id;
        var columns = this.selectedTable.columns;
        var querySql = Ext.getDom("textarea_sql").value;

        Ext.Ajax.request({
            url: tableUri + "/query",
            method: "GET",
            params: {
                tq: querySql,
                tqx: "out:json_array"
            },
            success: function(o) {
                var json = Ext.util.JSON.decode(o.responseText);
                if (json) {
                    org.systemsbiology.addama.js.Message.show("Datasources", "Retrieved " + data.length + " Records");

                    var fields = [];
                    Ext.each(columns, function(column) {
                        fields.push(column.name);
                        column.id = column.name;
                        column.header = column.name;
                        column.dataIndex = column.name;
                        column.sortable = true;
                    });

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
                                width: 200, sortable: true
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
                }
            },
            failure: this.displayFailure,
            scope: this
        });
    },

    queryCsv: function() {
        if (!this.isReadyToQuery()) {
            return;
        }

        var tableUri = this.selectedTable.id;
        var querySql = Ext.getDom("textarea_sql").value;
        document.location = tableUri + "/query?tq=" + querySql + "&tqx=reqId:123;out:tsv-excel;outFileName:results.tsv";
    },

    queryTsv: function() {
        if (!this.isReadyToQuery()) {
            return;
        }

        var tableUri = this.selectedTable.id;
        var querySql = Ext.getDom("textarea_sql").value;
        document.location = tableUri + "/query?tq=" + querySql + "&tqx=reqId:123;out:csv;outFileName:results";
    },

    isReadyToQuery: function() {
        if (!this.selectedTable) {
            org.systemsbiology.addama.js.Message.error("Datasources", "No table selected for query");
            return false;
        }

        if (!Ext.getDom("textarea_sql").value) {
            org.systemsbiology.addama.js.Message.error("Datasources", "SQL statement not entered. Defaulting to SELECT *");
            return true;
        }

        return true;
    }
});