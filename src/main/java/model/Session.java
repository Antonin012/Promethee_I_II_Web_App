package model;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Session {
    private String id;
    private String name;
    private Timestamp createdAt;
    private List<Criterion> criteria;
    private List<Alternative> alternatives;

    public Session() {
        this.id = UUID.randomUUID().toString();
        this.createdAt = new Timestamp(System.currentTimeMillis());
        this.criteria = new ArrayList<>();
        this.alternatives = new ArrayList<>();
    }

    public Session(String id, String name, Timestamp createdAt) {
        this.id = id;
        this.name = name;
        this.createdAt = createdAt;
        this.criteria = new ArrayList<>();
        this.alternatives = new ArrayList<>();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public List<Criterion> getCriteria() { return criteria; }
    public void setCriteria(List<Criterion> criteria) { this.criteria = criteria; }

    public List<Alternative> getAlternatives() { return alternatives; }
    public void setAlternatives(List<Alternative> alternatives) { this.alternatives = alternatives; }
}
