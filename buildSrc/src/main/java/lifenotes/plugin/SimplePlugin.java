package lifenotes.plugin;

import lifenotes.plugin.task.SimpleTask;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.tasks.TaskContainer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

public class SimplePlugin implements Plugin<Project> {
    private static final String TASK_GROUP = "Dynamic Tasks (type: SimpleTask)";

    @Override
    public void apply(Project project) {
        String root = new File(project.getRootProject().getRootDir().toPath().toString(), "/build/generated").getPath();
        project.getLogger().info("Setting root directory: {}", root);

        Properties properties = loadProjectProperties(project);
        List<String> environments = listAllEnvironments(properties);
        for (String env : environments) {
            project.getTasks().create("generate" + env + "file", SimpleTask.class, task -> {
                task.setGroup(TASK_GROUP);
                task.setEnvironmentName(env);
                task.setEnvironmentDescription(properties.getProperty(env));
                task.setOutputDir(root);
                task.setInputDir("src/main/resources");
                task.setTemplatePath("template/environmentTemplate.json");
                task.setDescription("Generates a file for the " + env + " environment");
            });
        }

        // Create a master class to run all dynamic tasks
        createMasterTask(project);
    }

    private void createMasterTask(Project project) {
        TaskContainer tasks = project.getTasks();
        List<Task> myTasks = tasks.stream().filter(task -> TASK_GROUP.equals(task.getGroup())).collect(Collectors.toList());

        tasks.create("generateAllEnvironmentFiles", task -> {
            task.setGroup(TASK_GROUP);
            task.setDependsOn(myTasks);
            task.setDescription("Generates files for all environments.");
        });
    }

    private Properties loadProjectProperties(Project project) {
        Properties properties = new Properties();
        File file = project.file("src/main/resources/environments.properties");
        try {
            FileInputStream fileInputStream = new FileInputStream(file);
            properties.load(fileInputStream);
        } catch (IOException e) {
            throw new IllegalStateException(String.format("Unable to open environment properties %s", file.getPath()), e);
        }
        return properties;
    }

    private List<String> listAllEnvironments(Properties properties) {
        List<String> environments = new ArrayList<>();

        Set<Object> keys = properties.keySet();

        for (Object key : keys) {
            String name = (String) key;
            environments.add(name);
        }

        return environments;
    }
}
