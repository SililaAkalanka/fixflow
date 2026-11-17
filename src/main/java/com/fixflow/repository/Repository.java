package com.fixflow.repository;

import java.util.List;

/**
 * Generic repository interface defining standard CRUD and persistence operations.
 * Demonstrates the OOP principles of Generics and Interfaces.
 *
 * @param <T> the type of entity managed by the repository
 */
public interface Repository<T> {

    /**
     * Adds a new entity to the repository and saves changes.
     *
     * @param item the entity to add
     */
    void add(T item);

    /**
     * Finds an entity by its unique numeric identifier.
     *
     * @param id entity ID
     * @return found entity, or null if not found
     */
    T findById(int id);

    /**
     * Retrieves all entities in the repository.
     *
     * @return List of all entities
     */
    List<T> findAll();

    /**
     * Updates an existing entity and saves changes.
     *
     * @param item updated entity
     */
    void update(T item);

    /**
     * Deletes an entity by its identifier and saves changes.
     *
     * @param id entity ID to remove
     * @return true if removed, false otherwise
     */
    boolean delete(int id);

    /**
     * Saves all in-memory entities to the underlying JSON file.
     */
    void save();

    /**
     * Loads entities from the underlying JSON file into memory.
     */
    void load();
}
