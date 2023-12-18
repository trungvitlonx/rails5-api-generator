package trungvitlonx.swagger.generator.rails5;

import io.swagger.codegen.v3.ClientOptInput;
import io.swagger.codegen.v3.DefaultGenerator;
import io.swagger.codegen.v3.config.CodegenConfigurator;
import trungvitlonx.swagger.generator.AbstractCodegenTest;

import org.apache.commons.io.FileUtils;
import org.junit.rules.TemporaryFolder;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;

public class Rails5CodegenTest extends AbstractCodegenTest {
    private final TemporaryFolder folder = new TemporaryFolder();

    @Test(description = "verify that all actions in controller are generated")
    public void testActionsGenerated() throws Exception {
        this.folder.create();
        final File output = this.folder.getRoot();

        final CodegenConfigurator configurator = new CodegenConfigurator()
            .setLang("rails5")
            .setInputSpecURL("src/test/resources/petstore.yaml")
            .setOutputDir(output.getAbsolutePath());

        final ClientOptInput clientOptInput = configurator.toClientOptInput();
        new DefaultGenerator().opts(clientOptInput).generate();

        final File petControllerFile = new File(output, "/app/controllers/pet_controller.rb");
        final String petControllerContent = FileUtils.readFileToString(petControllerFile, StandardCharsets.UTF_8);

        Assert.assertTrue(petControllerContent.contains("def add_pet"));
        Assert.assertTrue(petControllerContent.contains("def update_pet"));
        Assert.assertTrue(petControllerContent.contains("def find_pets_by_status"));
        Assert.assertTrue(petControllerContent.contains("def find_pets_by_tags"));
        Assert.assertTrue(petControllerContent.contains("def get_pet_by_id"));
        Assert.assertTrue(petControllerContent.contains("def update_pet_with_form"));
        Assert.assertTrue(petControllerContent.contains("def delete_pet"));
        Assert.assertTrue(petControllerContent.contains("def feed_pet"));
        Assert.assertTrue(petControllerContent.contains("def upload_file"));

        this.folder.delete();
    }
}
