/*
**    Copyright (C) 2003-2010 Institute for Systems Biology
**                            Seattle, Washington, USA.
**
**    This library is free software; you can redistribute it and/or
**    modify it under the terms of the GNU Lesser General Public
**    License as published by the Free Software Foundation; either
**    version 2.1 of the License, or (at your option) any later version.
**
**    This library is distributed in the hope that it will be useful,
**    but WITHOUT ANY WARRANTY; without even the implied warranty of
**    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
**    Lesser General Public License for more details.
**
**    You should have received a copy of the GNU Lesser General Public
**    License along with this library; if not, write to the Free Software
**    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307  USA
*/
package org.systemsbiology.addama.services.execution.mvc;

import org.apache.commons.lang.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.InitializingBean;
import org.systemsbiology.addama.registry.JsonConfig;
import org.systemsbiology.addama.registry.JsonConfigHandler;
import org.systemsbiology.addama.services.execution.util.EmailJsonConfigHandler;
import org.systemsbiology.addama.services.execution.util.Mailer;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author hrovira
 */
public abstract class BaseController implements InitializingBean {
    protected final Map<String, String> workDirsByUri = new HashMap<String, String>();
    protected final Map<String, String> scriptsByUri = new HashMap<String, String>();
    protected final Map<String, String> logFilesByUri = new HashMap<String, String>();
    protected final Map<String, String> viewersByUri = new HashMap<String, String>();
    protected final HashMap<String, String> jobExecutionDirectoryByUri = new HashMap<String, String>();

    private JsonConfig jsonConfig;
    private EmailJsonConfigHandler emailJsonConfigHandler = new EmailJsonConfigHandler();

    public void setJsonConfig(JsonConfig jsonConfig) {
        this.jsonConfig = jsonConfig;
    }

    public void setEmailJsonConfigHandler(EmailJsonConfigHandler emailJsonConfigHandler) {
        this.emailJsonConfigHandler = emailJsonConfigHandler;
    }

    public void afterPropertiesSet() throws Exception {
        jsonConfig.processConfiguration(new MapJsonConfigHandler(workDirsByUri, "workDir"));
        jsonConfig.processConfiguration(new MapJsonConfigHandler(scriptsByUri, "script"));
        jsonConfig.processConfiguration(new MapJsonConfigHandler(logFilesByUri, "logFile"));
        jsonConfig.processConfiguration(new MapJsonConfigHandler(viewersByUri, "viewer"));
        jsonConfig.processConfiguration(new MapJsonConfigHandler(jobExecutionDirectoryByUri, "jobExecutionDirectory"));
        jsonConfig.processConfiguration(emailJsonConfigHandler);
    }

    /*
     * Protected Methods
     */

    protected Mailer getMailer(String scriptUri, String label, String user) {
        if (this.emailJsonConfigHandler != null) {
            return this.emailJsonConfigHandler.getMailer(scriptUri, label, user);
        }
        return null;
    }

    protected String getUserEmail(HttpServletRequest request) {
        String userUri = request.getHeader("x-addama-registry-user");
        if (StringUtils.isEmpty(userUri)) {
            return null;
        }
        return StringUtils.substringAfterLast(userUri, "/addama/users/");
    }

    protected File[] getOutputFiles(File outputDirectory) throws JSONException {
        List<File> outputs = new ArrayList<File>();
        scanOutputs(outputs, outputDirectory);
        return outputs.toArray(new File[outputs.size()]);
    }

    protected void scanOutputs(List<File> outputs, File outputDir) {
        if (outputDir.isFile()) {
            outputs.add(outputDir);
        }
        if (outputDir.isDirectory()) {
            for (File f : outputDir.listFiles()) {
                scanOutputs(outputs, f);
            }
        }
    }

    protected String getJobExecutionDirectory(String scriptUri) {
        if (!jobExecutionDirectoryByUri.containsKey(scriptUri)) {
            return "outputs";
        }

        String jobExecutionDirectory = StringUtils.chomp(jobExecutionDirectoryByUri.get(scriptUri), "/");
        if (jobExecutionDirectory.startsWith("/")) {
            jobExecutionDirectory = StringUtils.substringAfter(jobExecutionDirectory, "/");
        }

        if (StringUtils.equalsIgnoreCase(jobExecutionDirectory, "null")) {
            return null;
        }

        return jobExecutionDirectory;
    }

    /*
    * Private Classes
    */

    private class MapJsonConfigHandler implements JsonConfigHandler {
        private final Map<String, String> propertiesByUri;
        private final String propertyName;

        public MapJsonConfigHandler(Map<String, String> map, String name) {
            this.propertiesByUri = map;
            this.propertyName = name;
        }

        public void handle(JSONObject configuration) throws Exception {
            if (configuration.has("locals")) {
                JSONArray locals = configuration.getJSONArray("locals");
                for (int i = 0; i < locals.length(); i++) {
                    JSONObject local = locals.getJSONObject(i);
                    if (local.has(propertyName)) {
                        propertiesByUri.put(local.getString("uri"), local.getString(propertyName));
                    }
                }
            }
        }

    }
}