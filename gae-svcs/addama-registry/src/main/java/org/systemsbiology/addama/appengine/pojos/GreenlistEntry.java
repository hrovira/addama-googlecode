package org.systemsbiology.addama.appengine.pojos;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * @author hrovira
 */
public class GreenlistEntry {
    private final String userEmail;
    private final String accessPath;
    private final boolean hasAccess;

    public GreenlistEntry(String userEmail, String accessPath, boolean hasAccess) {
        this.userEmail = userEmail;
        this.hasAccess = hasAccess;
        this.accessPath = accessPath;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public String getAccessPath() {
        return accessPath;
    }

    public boolean isHasAccess() {
        return hasAccess;
    }

    public JSONObject toJSON() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("user", userEmail);
        json.put("hasAccess", hasAccess);
        json.put("uri", accessPath);
        return json;
    }
}