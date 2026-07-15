package com.example.plugin.database;

import java.util.Optional;

/**
 * Generic repository interface for database CRUD operations.
 *
 * @param <T> Domain entity type
 * @param <ID> Primary key type
 */
public interface Repository<T, ID> {

    /**
     * Finds an entity by its primary key.
     *
     * @param id Primary key ID
     * @return Optional containing the entity if found
     */
    Optional<T> findById(ID id);

    /**
     * Saves or updates the given entity synchronously.
     *
     * @param entity Entity to save
     */
    void save(T entity);

    /**
     * Deletes the entity by its primary key.
     *
     * @param id Primary key ID
     */
    void deleteById(ID id);
}
