package org.xam.config;

import java.util.ArrayList;
import java.util.List;

public class Requirement {
    private String type; // "advancement", "craft", "kill"
    private String id;
    private String name;
    private String description;
    private List<String> dependencies = new ArrayList<>();
    private String effect = "";

    public Requirement() {}

    public Requirement(String type, String id, String name, String description) {
        this.type = type;
        this.id = id;
        this.name = name;
        this.description = description;
    }

    public Requirement(String type, String id, String name, String description, List<String> dependencies) {
        this.type = type;
        this.id = id;
        this.name = name;
        this.description = description;
        if (dependencies != null) {
            this.dependencies = new ArrayList<>(dependencies);
        }
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<String> getDependencies() {
        return java.util.Collections.unmodifiableList(dependencies);
    }

    public void setDependencies(List<String> dependencies) {
        this.dependencies = dependencies != null ? new ArrayList<>(dependencies) : new ArrayList<>();
    }

    public void addDependency(String dependency) {
        if (dependency != null && !this.dependencies.contains(dependency)) {
            this.dependencies.add(dependency);
        }
    }

    public void removeDependency(String dependency) {
        this.dependencies.remove(dependency);
    }

    public void clearDependencies() {
        this.dependencies.clear();
    }

    public String getEffect() {
        return effect != null ? effect : "";
    }

    public void setEffect(String effect) {
        this.effect = effect != null ? effect : "";
    }
}
