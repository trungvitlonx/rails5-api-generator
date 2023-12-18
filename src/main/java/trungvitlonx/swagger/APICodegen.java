package trungvitlonx.swagger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.swagger.codegen.v3.ClientOptInput;
import io.swagger.codegen.v3.DefaultGenerator;
import io.swagger.codegen.v3.config.CodegenConfigurator;

public class APICodegen {
     private static final Logger LOGGER = LoggerFactory.getLogger(APICodegen.class);

    public static void main(String[] args) {
        try {
            final String outputDir = "/Users/trungle/rails5-api-demo";
            final CodegenConfigurator configurator = new CodegenConfigurator()
                .setLang("rails5")
                .setInputSpecURL("/Users/trungle/rails5-api-generator/src/test/resources/petstore.yaml")
                .setOutputDir(outputDir);

            final ClientOptInput clientOptInput = configurator.toClientOptInput();
            new DefaultGenerator().opts(clientOptInput).generate();
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
    }
}
