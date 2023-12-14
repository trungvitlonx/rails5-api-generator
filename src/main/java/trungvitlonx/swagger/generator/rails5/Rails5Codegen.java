package trungvitlonx.swagger.generator.rails5;

import io.swagger.codegen.v3.CodegenConstants;
import io.swagger.codegen.v3.CodegenContent;
import io.swagger.codegen.v3.CodegenOperation;
import io.swagger.codegen.v3.CodegenParameter;
import io.swagger.codegen.v3.CodegenResponse;
import io.swagger.codegen.v3.CodegenType;
import io.swagger.codegen.v3.generators.DefaultCodegenConfig;
import io.swagger.codegen.v3.generators.OperationParameters;
import io.swagger.v3.core.util.Yaml;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;

import io.swagger.v3.oas.models.media.Schema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class Rails5Codegen extends DefaultCodegenConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(Rails5Codegen.class);

    protected String appFolder = "";
    protected String configFolder = "config";

    protected String controllerFolder = "controllers";

    public Rails5Codegen() {
        super();

        outputFolder = File.separator + "generated-code" + File.separator + "rails5";
        apiTemplateFiles.put("controller.mustache", "_controller.rb");
        typeMapping.clear();
        languageSpecificPrimitives.clear();
        setReservedWordsLowerCase(
            Arrays.asList(
                "__FILE__", "and", "def", "end", "in", "or", "self", "unless", "__LINE__",
                "begin", "defined?", "ensure", "module", "redo", "super", "until", "BEGIN",
                "break", "do", "false", "next", "rescue", "then", "when", "END", "case",
                "else", "for", "nil", "retry", "true", "while", "alias", "class", "elsif",
                "if", "not", "return", "undef", "yield"));

        typeMapping.put("string", "String");
        typeMapping.put("char", "String");
        typeMapping.put("int", "Integer");
        typeMapping.put("integer", "Integer");
        typeMapping.put("long", "Integer");
        typeMapping.put("short", "Integer");
        typeMapping.put("float", "Float");
        typeMapping.put("double", "BigDecimal");
        typeMapping.put("number", "Float");
        typeMapping.put("date", "Date");
        typeMapping.put("DateTime", "DateTime");
        typeMapping.put("boolean", ":boolean");
        typeMapping.put("binary", "String");
        typeMapping.put("ByteArray", "String");
        typeMapping.put("UUID", "String");

        cliOptions.clear();
    }

    @Override
    public String getDefaultTemplateDir() {
        return "rails5";
    }

    @Override
    public CodegenType getTag() {
        return CodegenType.SERVER;
    }

    @Override
    public String getName() {
        return "rails5";
    }

    @Override
    public String getHelp() {
        return "Generates a Rails5 API library.";
    }

    @Override
    public String toApiName(String name) {
        if (name.isEmpty()) {
            return "ApiController";
        }

        return camelize(name) + "Controller";
    }

    @Override
    public String apiPackage() {
        return this.appFolder + File.separator + this.controllerFolder;
    }

    @Override
    public String toApiFilename(String name) {
        name = name.replaceAll("-", "_");
        return underscore(name);
    }

    @Override
    public String escapeReservedWord(String name) {
        if (this.reservedWordsMappings().containsKey(name)) {
            return this.reservedWordsMappings().get(name);
        }
        return "_" + name;
    }

    @Override
    public String apiFileFolder() {
        return outputFolder + File.separator + apiPackage().replace("/", File.separator);
    }

    @Override
    public String toDefaultValue(Schema p) {
        return "null";
    }

    @Override
    public String toVarName(String name) {
        // replace - with _ e.g. created-at => created_at
        // FIXME: a parameter should not be assigned. Also declare the methods parameters as 'final'.
        name = name.replaceAll("-", "_");

        // if it's all upper case, convert to lower case
        if (name.matches("^[A-Z_]*$")) {
            name = name.toLowerCase();
        }

        // camelize (lower first character) the variable name
        // petId => pet_id
        name = underscore(name);

        // for reserved word or word starting with number, append _
        if (isReservedWord(name) || name.matches("^\\d.*")) {
            name = escapeReservedWord(name);
        }

        return name;
    }

    @Override
    public String toParamName(String name) {
        // should be the same as variable name
        return toVarName(name);
    }

    @Override
    public String toOperationId(String operationId) {
        // method name cannot use reserved keyword, e.g. return
        if (isReservedWord(operationId)) {
            String newOperationId = underscore("call_" + operationId);
            LOGGER.warn(operationId + " (reserved word) cannot be used as method name. Renamed to " + newOperationId);
            return newOperationId;
        }

        return underscore(operationId);
    }

    @Override
    public Map<String, Object> postProcessOperations(Map<String, Object> objs) {
        @SuppressWarnings("unchecked")
        Map<String, Object> objectMap = (Map<String, Object>) objs.get("operations");

        @SuppressWarnings("unchecked")
        List<CodegenOperation> operations = (List<CodegenOperation>) objectMap.get("operation");
        for (CodegenOperation operation : operations) {
            operation.httpMethod = operation.httpMethod.toLowerCase();

            List<CodegenParameter> params = operation.allParams;
            if (params != null && params.isEmpty()) {
                operation.allParams = null;
            }
            List<CodegenResponse> responses = operation.responses;
            if (responses != null) {
                for (CodegenResponse resp : responses) {
                    if ("0".equals(resp.code)) {
                        resp.code = "default";
                    }
                }
            }
            if (operation.examples != null && !operation.examples.isEmpty()) {
                // Leave application/json* items only
                for (Iterator<Map<String, String>> it = operation.examples.iterator(); it.hasNext(); ) {
                    final Map<String, String> example = it.next();
                    final String contentType = example.get("contentType");
                    if (contentType == null || !contentType.startsWith("application/json")) {
                        it.remove();
                    }
                }
            }
        }
        return objs;
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> getOperations(Map<String, Object> objs) {
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        Map<String, Object> apiInfo = (Map<String, Object>) objs.get("apiInfo");
        List<Map<String, Object>> apis = (List<Map<String, Object>>) apiInfo.get("apis");
        for (Map<String, Object> api : apis) {
            result.add((Map<String, Object>) api.get("operations"));
        }
        return result;
    }

    private static List<Map<String, Object>> sortOperationsByPath(List<CodegenOperation> ops) {
        Multimap<String, CodegenOperation> opsByPath = ArrayListMultimap.create();

        for (CodegenOperation op : ops) {
            opsByPath.put(op.path, op);
        }

        List<Map<String, Object>> opsByPathList = new ArrayList<Map<String, Object>>();
        for (Map.Entry<String, Collection<CodegenOperation>> entry : opsByPath.asMap().entrySet()) {
            Map<String, Object> opsByPathEntry = new HashMap<String, Object>();
            opsByPathList.add(opsByPathEntry);
            opsByPathEntry.put("path", entry.getKey());
            opsByPathEntry.put("operation", entry.getValue());
            List<CodegenOperation> operationsForThisPath = Lists.newArrayList(entry.getValue());
            operationsForThisPath.get(operationsForThisPath.size() - 1).getVendorExtensions()
                .put(CodegenConstants.HAS_MORE_EXT_NAME, Boolean.FALSE);
            if (opsByPathList.size() < opsByPath.asMap().size()) {
                opsByPathEntry.put("hasMore", "true");
            }
        }

        return opsByPathList;
    }

    @Override
    public void processOpts() {
        super.processOpts();
    }

    @Override
    public void preprocessOpenAPI(OpenAPI openAPI) {
        this.openAPI = openAPI;

        // need vendor extensions for x-swagger-router-controller
        Paths paths = openAPI.getPaths();

        if (paths != null) {
            for (String pathname : paths.keySet()) {
                PathItem path = paths.get(pathname);
                Map<PathItem.HttpMethod, Operation> operationMap = path.readOperationsMap();
                if (operationMap != null) {
                    for (PathItem.HttpMethod method : operationMap.keySet()) {
                        Operation operation = operationMap.get(method);
                        String tag = "default";
                        if (operation.getTags() != null && !operation.getTags().isEmpty()) {
                            tag = toApiName(operation.getTags().get(0));
                        }
                        if (operation.getOperationId() == null) {
                            operation.setOperationId(getOrGenerateOperationId(operation, pathname, method.toString()));
                        }

                        if (operation.getExtensions() == null) {
                            operation.setExtensions(new HashMap<>());
                        }
                        if (operation.getExtensions() != null
                            && operation.getExtensions().get("x-swagger-router-controller") == null) {
                            operation.getExtensions().put("x-swagger-router-controller", sanitizeTag(tag));
                        }
                    }
                }
            }
        }
    }

    @Override
    public Map<String, Object> postProcessSupportingFileData(Map<String, Object> objs) {
        OpenAPI openAPI = (OpenAPI) objs.get("openAPI");

        if (openAPI != null) {
            try {
                SimpleModule module = new SimpleModule();
                module.addSerializer(Double.class, new JsonSerializer<Double>() {
                    @Override
                    public void serialize(Double val, JsonGenerator jgen,
                                          SerializerProvider provider) throws IOException, JsonProcessingException {
                        jgen.writeNumber(new BigDecimal(val));
                    }
                });
                objs.put("swagger-yaml", Yaml.mapper().registerModule(module).writeValueAsString(openAPI));
            } catch (JsonProcessingException e) {
                LOGGER.error(e.getMessage(), e);
            }
        }

        for (Map<String, Object> operations : getOperations(objs)) {
            @SuppressWarnings("unchecked")
            List<CodegenOperation> ops = (List<CodegenOperation>) operations.get("operation");

            List<Map<String, Object>> opsByPathList = sortOperationsByPath(ops);
            operations.put("operationsByPath", opsByPathList);
        }

        return super.postProcessSupportingFileData(objs);
    }


    @Override
    public String escapeUnsafeCharacters(String input) {
        return input.replace("=end", "=_end").replace("=begin", "=_begin");
    }

    @Override
    public String escapeQuotationMark(String input) {
        // remove " to avoid code injection
        return input.replace("\"", "");
    }

    protected void configuresParameterForMediaType(CodegenOperation codegenOperation,
                                                   List<CodegenContent> codegenContents) {
        if (codegenContents.isEmpty()) {
            CodegenContent content = new CodegenContent();
            content.getParameters().addAll(codegenOperation.allParams);
            codegenContents.add(content);

            codegenOperation.getContents().add(content);
            return;
        }

        for (CodegenContent content : codegenContents) {
            addParameters(content, codegenOperation.bodyParams);
            addParameters(content, codegenOperation.queryParams);
            addParameters(content, codegenOperation.pathParams);
            addParameters(content, codegenOperation.headerParams);
            addParameters(content, codegenOperation.cookieParams);
        }

        for (CodegenContent content : codegenContents) {
            OperationParameters.addHasMore(content.getParameters());
        }

        codegenOperation.getContents().addAll(codegenContents);
    }
}
