package trungvitlonx.swagger.generator.rails5;

import io.swagger.codegen.v3.*;
import io.swagger.codegen.v3.generators.DefaultCodegenConfig;
import io.swagger.codegen.v3.generators.OperationParameters;
import io.swagger.codegen.v3.generators.util.OpenAPIUtil;
import io.swagger.v3.core.util.Yaml;
import io.swagger.v3.oas.models.OpenAPI;

import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.helper.ConditionalHelpers;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;

public class Rails5Codegen extends DefaultCodegenConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(Rails5Codegen.class);

    protected String appFolder = "app";
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
        typeMapping.put("array", "Array");

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
            return "Default";
        }

        return camelize(name);
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
        return "nil";
    }

    @Override
    public String toVarName(String name) {
        // replace - with _ e.g. created-at => created_at
        // FIXME: a parameter should not be assigned. Also declare the methods
        // parameters as 'final'.
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

        Map<String, Schema> schemas = this.openAPI.getComponents().getSchemas();
        Set<String> imports = new HashSet<>();

        for (CodegenOperation operation : operations) {
            operation.httpMethod = operation.httpMethod.toLowerCase();

            List<CodegenParameter> params = operation.allParams;
            if (params != null && params.isEmpty()) {
                operation.allParams = null;
            }

            CodegenParameter bodyParam = operation.getBodyParam();

            if (bodyParam != null) {
                CodegenParameter codegenParameter = CodegenModelFactory.newInstance(CodegenModelType.PARAMETER);
                Schema schema = schemas.get(bodyParam.dataType);

                if (schema != null) {
                    Map<String, Schema> properties = schema.getProperties();

                    for (String key : properties.keySet()) {
                        Schema property = properties.get(key);

                        if (property.get$ref() != null && StringUtils.isNotBlank(property.get$ref())) {
                            String schemaName = OpenAPIUtil.getSimpleRef(property.get$ref());
                            Schema subSchema = schemas.get(schemaName);
                        } else {
                            boolean required = false;
                            if (schema.getRequired() != null && !schema.getRequired().isEmpty()) {
                                required = schema.getRequired().contains(key);
                            }

                            Parameter parameter = new Parameter().name(key).required(required).schema(property);
                            codegenParameter = super.fromParameter(parameter, imports);
                            operation.allParams.add(codegenParameter);
                        }
                    }
                }
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

    @Override
    public void processOpts() {
        super.processOpts();

        supportingFiles.add(new SupportingFile("routes.mustache", configFolder, "api_routes.rb"));
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

    @Override
    public void addHandlebarHelpers(Handlebars handlebars) {
        super.addHandlebarHelpers(handlebars);
        handlebars.registerHelpers(ConditionalHelpers.class);
    }
}
