package org.xam.config;

import java.util.ArrayList;
import java.util.List;

public class Requirement {
    public String type; // "advancement", "craft", "kill"
    public String id;
    public String name;
    public String description;
    public List<String> dependencies = new ArrayList<>();

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
}
