package lifenotes.plugin.task;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jsonpatch.JsonPatchException;
import com.github.fge.jsonpatch.mergepatch.JsonMergePatch;
import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class SimpleTask extends DefaultTask {
    private String environmentName;
    private String environmentDescription;
    private String templatePath;
    private String outputDir;
    private String inputDir;

    public void setEnvironmentName(String environmentName) {
        this.environmentName = environmentName;
    }

    public void setEnvironmentDescription(String environmentDescription) {
        this.environmentDescription = environmentDescription;
    }

    public void setTemplatePath(String templatePath) {
        this.templatePath = templatePath;
    }

    @OutputDirectory
    public String getOutputDir() {
        return outputDir;
    }

    public void setOutputDir(String outputDir) {
        this.outputDir = outputDir;
    }

    @InputDirectory
    public String getInputDir() {
        return inputDir;
    }

    public void setInputDir(String inputDir) {
        this.inputDir = inputDir;
    }

    @TaskAction
    public void generateJsonFileForThisEnvironment() {
        JsonNode templateNode = getFileAsJsonNode(templatePath);
        JsonMergePatch patch = getJsonMergePatch();

        JsonNode result = applyPatch(templateNode, patch);

        createFile(result.toString());
    }

    private JsonNode getFileAsJsonNode(String filepath) {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonNode;
        try {
            jsonNode = mapper.readValue(readFile(filepath), JsonNode.class);
        } catch (IOException e) {
            String error = String.format("Unable to load file as JsonNode type; File : %s", filepath);
            throw new IllegalStateException(error, e);
        }
        return jsonNode;
    }

    private JsonMergePatch getJsonMergePatch() {
        ObjectMapper mapper = new ObjectMapper();
        // Create a json string with the configured environment name and description
        String content =
            String.format("{\"environmentName\":\"%s\", \"description\":\"%s\"}", environmentName, environmentDescription);

        JsonMergePatch patch;
        try {
            patch = mapper.readValue(content, JsonMergePatch.class);
        } catch (IOException e) {
            String error = String.format("Unable generate JsonMergePatch type; Content: %s", content);
            throw new IllegalStateException(error, e);
        }
        return patch;
    }

    private String readFile(String filePath) {
        File file = getProject().file(filePath);

        String content = "{}";
        if (file.exists()) {
            try {
                content = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new IllegalStateException(String.format("Unable to read file: %s ", file.getPath()), e);
            }
        }
        return content;
    }

    private JsonNode applyPatch(JsonNode base, JsonMergePatch patch) {
        try {
            return patch.apply(base);
        } catch (JsonPatchException e) {
            throw new IllegalStateException(
                String.format("Unable to apply patch. base=%s; patch=%s", base.toString(), patch.toString()), e);
        }
    }

    private void createFile(String fileContents) {

        // Name the new file <environmentName>.json
        File file = new File(outputDir, environmentName + ".json");
        try {
            // Ensure new path exists
            file.getParentFile().mkdirs();
            file.createNewFile();
            Writer writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8);
            writer.write(prettyPrintJson(fileContents));
            writer.close();
        } catch (IOException e) {
            String error = String.format("Unable to create new file. Path: %s", file.getPath());
            throw new IllegalStateException(error, e);
        }
    }

    private String prettyPrintJson(String uglyJson) {
        Object jsonObj;
        try {
            jsonObj = new ObjectMapper().readValue(uglyJson, Object.class);
        } catch (IOException e) {
            throw new IllegalStateException(String.format("Unable to read json string as Object. String: %s", uglyJson), e);
        }
        try {
            return new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(jsonObj);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(String.format("Unable to pretty-print json object. String: %s", uglyJson), e);
        }
    }
}
