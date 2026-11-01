package com.medassist.tenancy;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface BranchRepository extends JpaRepository<Branch, UUID> {

    List<Branch> findAllByOrderByNameAsc();

    long countByActiveTrue();

    boolean existsByCode(String code);

    /**
     * Public pharmacy finder. Native SQL, so the tenant filter does NOT apply (on purpose:
     * this lists listed branches of every org). Haversine distance in km; the bounding box
     * lets the planner use the (latitude, longitude) index before the trig math.
     */
    @Query(value = """
            select b.id as id, b.org_id as orgId, b.name as name, o.name as orgName, b.address_line as addressLine,
                   b.city as city, b.pincode as pincode, b.latitude as latitude, b.longitude as longitude,
                   b.phone as phone, b.opening_hours as openingHours, b.open_24x7 as open24x7,
                   b.delivery_available as deliveryAvailable,
                   6371 * acos(least(1, cos(radians(:lat)) * cos(radians(b.latitude))
                        * cos(radians(b.longitude) - radians(:lng)) + sin(radians(:lat)) * sin(radians(b.latitude)))) as distanceKm
            from branches b join organizations o on o.id = b.org_id
            where b.active and b.publicly_listed and o.status = 'ACTIVE'
              and b.latitude between :lat - (:radiusKm / 111.0) and :lat + (:radiusKm / 111.0)
              and b.longitude between :lng - (:radiusKm / (111.0 * cos(radians(:lat))))
                                  and :lng + (:radiusKm / (111.0 * cos(radians(:lat))))
            order by distanceKm
            limit :limit
            """, nativeQuery = true)
    List<NearbyBranchView> findNearby(@Param("lat") double lat, @Param("lng") double lng,
                                      @Param("radiusKm") double radiusKm, @Param("limit") int limit);

    interface NearbyBranchView {
        UUID getId();

        UUID getOrgId();

        String getName();

        String getOrgName();

        String getAddressLine();

        String getCity();

        String getPincode();

        Double getLatitude();

        Double getLongitude();

        String getPhone();

        String getOpeningHours();

        Boolean getOpen24x7();

        Boolean getDeliveryAvailable();

        Double getDistanceKm();
    }
}
