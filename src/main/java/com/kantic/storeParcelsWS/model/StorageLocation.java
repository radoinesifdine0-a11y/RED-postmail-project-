package com.kantic.storeParcelsWS.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Field;

/**
 * Embedded document representing where the parcel is stored in the store.
 *
 * Example: Parcel is on rack "A", shelf "12" in store "ST001"
 *
 * In MongoDB:
 * {
 *   "storeParcelStorageLocation": {
 *     "storeId": "ST001",
 *     "storeStorageLocationCode": "RACK-A-12"
 *   }
 * }
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StorageLocation {

    private String storeId;

    @Field("storeStorageLocationCode")
    private String locationCode;
}
