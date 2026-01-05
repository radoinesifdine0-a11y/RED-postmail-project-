package com.kantic.storeParcelsWS.repository;

import com.kantic.storeParcelsWS.model.Parcel;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository interface for Parcel entity.
 *
 * By extending MongoRepository, we get these methods FOR FREE:
 * - save(entity) - Insert or update
 * - findById(id) - Find by MongoDB _id
 * - findAll() - Get all documents
 * - delete(entity) - Remove document
 * - count() - Count documents
 *
 * Spring Data also generates methods based on method names!
 * Example: findByParcelId(String) → db.parcels.findOne({parcelId: value})
 *
 * For complex queries (multi-criteria search), we use ParcelRepositoryCustom
 */
@Repository
public interface ParcelRepository extends MongoRepository<Parcel, String>, ParcelRepositoryCustom {

    /**
     * Find parcel by its business ID (not MongoDB _id)
     * Spring auto-generates: db.parcels.findOne({parcelId: ?})
     */
    Optional<Parcel> findByParcelId(String parcelId);

    /**
     * Find parcel by ID excluding deleted parcels.
     * Used for update/delete operations where we only want active parcels.
     */
    Optional<Parcel> findByParcelIdAndDeletedFalse(String parcelId);

    /**
     * Check if a parcel exists with this ID
     * Spring auto-generates: db.parcels.countDocuments({parcelId: ?}) > 0
     */
    boolean existsByParcelId(String parcelId);
}
