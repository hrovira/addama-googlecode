Ext.ns("org.systemsbiology.addama.js");

org.systemsbiology.addama.js.TopBar = Ext.extend(Ext.util.Observable, {

    constructor: function(config) {
        Ext.apply(this, config);

        this.toolbar = new Ext.Toolbar({
            buttonAlign: "right",
            renderTo: this.contentEl
        });

        this.addEvents({
            /**
             * @event whoami
             * Fires after the whoami REST call is returned.
             * @param {Object} json represents logged-in user object
             */
            whoami: true
        });

        org.systemsbiology.addama.js.TopBar.superclass.constructor.call(this);

        Ext.Ajax.request({
            url: "/addama/users/whoami",
            method: "GET",
            scope: this,
            listeners: {
                requestcomplete: function(c, o) {
                    // TODO: check if 302, then redirect document
                    this.toolbar.add({ text: "Not logged in" });
                    this.toolbar.doLayout();
                },
                requestexception: function(c, o) {
                    // TODO: check if 302, then redirect document
                    this.toolbar.add({ text: "Errors, try again" });
                    this.toolbar.doLayout();
                }
            },
            success: function(o) {
                var json = Ext.util.JSON.decode(o.responseText);
                if (json && json.email) {
                    this.toolbar.add({ text: json.email, xtype: 'tbtext' });
                    if (json.isAdmin) {
                        this.toolbar.add({ text: "Refresh UI Version", xtype: 'tbbutton',
                            handler: function() {
                                Ext.Ajax.request({
                                    url: "/addama/apps/refresh", method: "POST",
                                    success: function() {
                                        document.location = document.location.href;
                                    }
                                });
                            }
                        });
                    }
                    this.toolbar.add({ text: 'Sign out', xtype: 'tbbutton',
                        handler:function() {
                            document.location = json.logoutUrl;
                        }
                    });
                    this.toolbar.doLayout();
                    this.fireEvent("whoami", json);
                } else {
                    this.toolbar.add({ text: "Not logged in" });
                    this.toolbar.doLayout();
                }
            }
        });
    }
});
