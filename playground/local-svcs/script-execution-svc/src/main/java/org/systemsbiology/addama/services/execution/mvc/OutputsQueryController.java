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

import com.google.visualization.datasource.DataSourceHelper;
import com.google.visualization.datasource.DataTableGenerator;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.systemsbiology.addama.commons.web.exceptions.InvalidSyntaxException;
import org.systemsbiology.addama.commons.web.exceptions.ResourceNotFoundException;
import org.systemsbiology.addama.services.execution.datasource.CsvFileDataTableGenerator;
import org.systemsbiology.addama.services.execution.datasource.TsvFileDataTableGenerator;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.logging.Logger;

/**
 * @author hrovira
 */
@Controller
public class OutputsQueryController extends BaseController {
    private static final Logger log = Logger.getLogger(OutputsQueryController.class.getName());

    private String jobPath;

    public void setJobPath(String jobPath) {
        this.jobPath = jobPath;
    }

    @RequestMapping(method = RequestMethod.GET)
    public void queryJobOutput(HttpServletRequest request, HttpServletResponse response) throws Exception {
        log.info("queryJobOutput(" + request.getRequestURI() + ")");

        String scriptUri = StringUtils.substringBetween(request.getRequestURI(), request.getContextPath(), jobPath);
        String workDir = workDirsByUri.get(scriptUri);
        if (StringUtils.isEmpty(workDir)) {
            throw new ResourceNotFoundException("work directory for " + scriptUri);
        }

        String outputFilePath = StringUtils.substringBetween(request.getRequestURI(), jobPath, "/query");
        InputStream checkStream = new FileInputStream(workDir + jobPath + outputFilePath);
        InputStream inputStream = new FileInputStream(workDir + jobPath + outputFilePath);

        try {
            DataTableGenerator dataTableGenerator = getDataTableGeneratorByOutputType(checkStream, inputStream);
            if (dataTableGenerator == null) {
                throw new InvalidSyntaxException("file cannot be queried");
            }

            DataSourceHelper.executeDataSourceServletFlow(request, response, dataTableGenerator, false);
        } catch (Exception e) {
            log.warning("queryJobOutput(" + request.getRequestURI() + "):" + e);
        } finally {
            checkStream.close();
            inputStream.close();
        }
    }

    /*
     * Private Methods
     */

    private DataTableGenerator getDataTableGeneratorByOutputType(InputStream checkStream, InputStream inputStream) {
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(checkStream));
            String columnHeader = reader.readLine();
            if (!StringUtils.isEmpty(columnHeader)) {
                if (StringUtils.contains(columnHeader, "\t")) {
                    return new TsvFileDataTableGenerator(inputStream, columnHeader);
                }
                if (StringUtils.contains(columnHeader, ",")) {
                    return new CsvFileDataTableGenerator(inputStream, columnHeader);
                }
            }
        } catch (Exception e) {
            log.warning("getDataTableGeneratorByOutputType(): " + e);
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (IOException e) {
                log.warning("getDataTableGeneratorByOutputType(): " + e);
            }
        }
        return null;
    }
}