package model;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Represents a decision-making session containing the state of the problem,
 * including its criteria, alternatives, and metadata such as creation time.
 * 
 * @author Developer
 */
public class Session {
    private String id;
    private String name;
    private Timestamp createdAt;
    private List<Criterion> criteria;
    private List<Alternative> alternatives;

    /**
     * Constructs a new Session with a randomly generated ID and the current timestamp.
     * Initializes empty lists for criteria and alternatives.
     */
    public Session() {
        this.id = UUID.randomUUID().toString();
        this.createdAt = new Timestamp(System.currentTimeMillis());
        this.criteria = new ArrayList<>();
        this.alternatives = new ArrayList<>();
    }

    /**
     * Constructs a new Session with specified metadata.
     * Initializes empty lists for criteria and alternatives.
     * 
     * @param id the unique identifier for the session
     * @param name the descriptive name of the session
     * @param createdAt the timestamp indicating when the session was created
     */
    public Session(String id, String name, Timestamp createdAt) {
        this.id = id;
        this.name = name;
        this.createdAt = createdAt;
        this.criteria = new ArrayList<>();
        this.alternatives = new ArrayList<>();
    }

    /**
     * Retrieves the unique identifier of this session.
     * 
     * @return the session's ID
     */
    public String getId() { return id; }

    /**
     * Sets the unique identifier of this session.
     * 
     * @param id the new session ID
     */
    public void setId(String id) { this.id = id; }

    /**
     * Retrieves the descriptive name of this session.
     * 
     * @return the session's name
     */
    public String getName() { return name; }

    /**
     * Sets the descriptive name of this session.
     * 
     * @param name the new session name
     */
    public void setName(String name) { this.name = name; }

    /**
     * Retrieves the creation timestamp of this session.
     * 
     * @return the creation timestamp
     */
    public Timestamp getCreatedAt() { return createdAt; }

    /**
     * Sets the creation timestamp of this session.
     * 
     * @param createdAt the new creation timestamp
     */
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    /**
     * Retrieves the list of criteria defined in this session.
     * 
     * @return the list of criteria
     */
    public List<Criterion> getCriteria() { return criteria; }

    /**
     * Sets the list of criteria for this session.
     * 
     * @param criteria the new list of criteria
     */
    public void setCriteria(List<Criterion> criteria) { this.criteria = criteria; }

    /**
     * Retrieves the list of alternatives evaluated in this session.
     * 
     * @return the list of alternatives
     */
    public List<Alternative> getAlternatives() { return alternatives; }

    /**
     * Sets the list of alternatives for this session.
     * 
     * @param alternatives the new list of alternatives
     */
    public void setAlternatives(List<Alternative> alternatives) { this.alternatives = alternatives; }
}