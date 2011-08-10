package org.systemsbiology.addama.filedbcreate.mvc;

import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.json.JSONObject;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.ServletRequestUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.systemsbiology.addama.commons.web.exceptions.InvalidSyntaxException;
import org.systemsbiology.addama.commons.web.exceptions.ResourceNotFoundException;
import org.systemsbiology.addama.commons.web.views.JsonView;
import org.systemsbiology.addama.filedbcreate.etl.TableCreatorRunnable;
import org.systemsbiology.addama.filedbcreate.etl.TableDDL;
import org.systemsbiology.addama.filedbcreate.etl.ddl.LabeledDataMatrixTableDDL;
import org.systemsbiology.addama.filedbcreate.etl.ddl.SimpleConfigurationTableDDL;
import org.systemsbiology.addama.filedbcreate.etl.ddl.SimpleTableTableDDL;
import org.systemsbiology.addama.jsonconfig.JsonConfig;
import org.systemsbiology.addama.jsonconfig.impls.StringMapJsonConfigHandler;
import org.systemsbiology.google.visualization.datasource.jdbc.JdbcTemplateJsonConfigHandler;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import static org.apache.commons.fileupload.servlet.ServletFileUpload.isMultipartContent;
import static org.apache.commons.lang.StringUtils.*;
import static org.systemsbiology.addama.filedbcreate.dao.UriTableMappings.createTable;
import static org.systemsbiology.addama.filedbcreate.dao.UriTableMappings.insertMapping;

/**
 * @author dburdick
 */
@Controller
public class FileDataSourceController implements InitializingBean {
    private static final Logger log = Logger.getLogger(FileDataSourceController.class.getName());

    private final HashMap<String, JdbcTemplate> datasourcesByUri = new HashMap<String, JdbcTemplate>();
    private final HashMap<String, String> rootPathByUri = new HashMap<String, String>();

    public void setJsonConfig(JsonConfig jsonConfig) {
        jsonConfig.visit(new StringMapJsonConfigHandler(rootPathByUri, "rootPath"));
        jsonConfig.visit(new JdbcTemplateJsonConfigHandler(datasourcesByUri));
    }

    public void afterPropertiesSet() throws Exception {
        for (Map.Entry<String, JdbcTemplate> entry : datasourcesByUri.entrySet()) {
            createTable(entry.getValue());
        }
    }

    /*
    * Controllers
    */

    @RequestMapping(method = RequestMethod.POST)
    public ModelAndView uploadFile(HttpServletRequest request, @RequestParam(value = "transform", required = false) String transformUri) throws Exception {
        log.info("uploadFile(" + request.getRequestURI() + "," + transformUri + ")");

        TableDDL tableDDL = getTransform(request);

        JSONObject json = new JSONObject();

        boolean isMultipart = isMultipartContent(request);
        if (!isMultipart) {
            log.warning("uploadFile(" + request.getRequestURI() + "): not multipart content");
            throw new InvalidSyntaxException("multipart request required");
        }

        ServletFileUpload fileUpload = new ServletFileUpload();
        FileItemIterator itemIterator = fileUpload.getItemIterator(request);
        if (!itemIterator.hasNext()) {
            log.warning("uploadFile(" + request.getRequestURI() + "): no files submitted");
            throw new InvalidSyntaxException("no files submitted");
        }

        while (itemIterator.hasNext()) {
            FileItemStream itemStream = itemIterator.next();
            if (!itemStream.isFormField()) {
                json.accumulate("uri", uploadData(request, itemStream, tableDDL));
            }
        }

        ModelAndView mav = new ModelAndView(new JsonView());
        mav.addObject("json", json);
        return mav;
    }

    /*
     * Private Methods
     */

    private TableDDL getTransform(HttpServletRequest request) {
        String tt = ServletRequestUtils.getStringParameter(request, "transform", TableDDL.Types.simpleTable.name());
        switch (TableDDL.Types.valueOf(tt)) {
            case simpleTable:
                return new SimpleTableTableDDL();
            case labeledDataMatrix:
                return new LabeledDataMatrixTableDDL();
            case typeMap:
                try {
                    String simpleConfig = ServletRequestUtils.getStringParameter(request, "typeMap");
                    return new SimpleConfigurationTableDDL(new JSONObject(simpleConfig));
                } catch (Exception e) {
                    log.warning("getTransform(" + request.getRequestURI() + "):" + e);
                }
        }
        return new SimpleTableTableDDL();
    }


    private String uploadData(HttpServletRequest request, FileItemStream itemStream, TableDDL tableDDL) throws Exception {
        log.info("uploadData(" + request.getRequestURI() + ")");

        String datasourceUri = getDatasourceUri(request);
        log.info("uploadData():datasourceUri=" + datasourceUri);

        String tempFile = prepTempFile(datasourceUri, itemStream);
        log.info("uploadData():tempFile=" + tempFile);

        JdbcTemplate jdbcTemplate = getDataSource(datasourceUri);

        String databaseUri = datasourceUri + "/" + itemStream.getName();
        String tableName = "T_" + Math.abs(databaseUri.hashCode());
        String queryUri = addToMappingTable(databaseUri, tableName, jdbcTemplate);
        new Thread(new TableCreatorRunnable(jdbcTemplate, tableDDL, tableName, tempFile)).start();
        log.info("uploadData():tableName=" + tableName);
        log.info("uploadData():queryUri=" + queryUri);
        return queryUri;
    }

    private String addToMappingTable(String databaseUri, String tableName, JdbcTemplate jdbcTemplate) {
        log.info("addToMappingTable(" + databaseUri + "," + tableName + ")");

        String queryUri = replace(databaseUri, "/write/", "/");
        insertMapping(jdbcTemplate, queryUri, tableName);
        return queryUri;
    }

    private String prepTempFile(String datasourceUri, FileItemStream itemStream) throws Exception {
        String rootPath = getRootPath(datasourceUri);

        File tempDir = new File(rootPath + datasourceUri);
        tempDir.mkdirs();

        File tempFile = new File(tempDir, itemStream.getName());

        byte[] buf = new byte[10000];
        int len;
        OutputStream outputStream = new FileOutputStream(tempFile);
        InputStream inputStream = itemStream.openStream();
        while ((len = inputStream.read(buf, 0, 1000)) > 0) {
            outputStream.write(buf, 0, len);
        }
        outputStream.close();

        return tempFile.getPath();
    }

    private String getDatasourceUri(HttpServletRequest request) {
        String databaseUri = substringAfterLast(request.getRequestURI(), request.getContextPath());
        if (databaseUri.endsWith("/query")) {
            return substringBeforeLast(databaseUri, "/query");
        }
        return databaseUri;
    }

    private JdbcTemplate getDataSource(String databaseUri) throws Exception {
        for (Map.Entry<String, JdbcTemplate> entry : datasourcesByUri.entrySet()) {
            if (databaseUri.startsWith(entry.getKey())) {
                return entry.getValue();
            }
        }
        throw new ResourceNotFoundException(databaseUri);
    }

    private String getRootPath(String databaseUri) throws Exception {
        log.info("getRootPath(" + databaseUri + ")");

        for (Map.Entry<String, String> entry : rootPathByUri.entrySet()) {
            if (databaseUri.startsWith(entry.getKey())) {
                return entry.getValue();
            }
        }
        throw new ResourceNotFoundException(databaseUri);
    }
}