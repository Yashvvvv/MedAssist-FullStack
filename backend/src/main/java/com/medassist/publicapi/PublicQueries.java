package com.medassist.publicapi;

import org.springframework.data.repository.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Cross-tenant read models for the public (anonymous) surface. Native SQL on purpose:
 * these queries must span every pharmacy, and they only ever expose listed branches,
 * active products and an in-stock yes/no, never quantities, costs or customers.
 */
public interface PublicQueries extends Repository<com.medassist.tenancy.Branch, UUID> {

    @Query(value = """
            select b.id as branchId, b.name as branchName, o.name as orgName, b.address_line as addressLine,
                   b.city as city, b.latitude as latitude, b.longitude as longitude, b.phone as phone,
                   b.delivery_available as deliveryAvailable, p.id as productId, p.name as productName,
                   p.selling_price as price,
                   6371 * acos(least(1, cos(radians(:lat)) * cos(radians(b.latitude))
                        * cos(radians(b.longitude) - radians(:lng)) + sin(radians(:lat)) * sin(radians(b.latitude)))) as distanceKm
            from branches b
            join organizations o on o.id = b.org_id and o.status = 'ACTIVE'
            join products p on p.org_id = b.org_id and p.medicine_id = :medicineId and p.active
            where b.active and b.publicly_listed
              and b.latitude between :lat - (:radiusKm / 111.0) and :lat + (:radiusKm / 111.0)
              and b.longitude between :lng - (:radiusKm / (111.0 * cos(radians(:lat))))
                                  and :lng + (:radiusKm / (111.0 * cos(radians(:lat))))
              and exists (select 1 from stock_batches sb where sb.branch_id = b.id and sb.product_id = p.id
                          and sb.quantity > 0 and sb.expiry_date >= current_date)
            order by distanceKm
            limit 20
            """, nativeQuery = true)
    List<WhoHasView> whoHas(@Param("medicineId") UUID medicineId, @Param("lat") double lat,
                            @Param("lng") double lng, @Param("radiusKm") double radiusKm);

    interface WhoHasView {
        UUID getBranchId();

        String getBranchName();

        String getOrgName();

        String getAddressLine();

        String getCity();

        Double getLatitude();

        Double getLongitude();

        String getPhone();

        Boolean getDeliveryAvailable();

        UUID getProductId();

        String getProductName();

        BigDecimal getPrice();

        Double getDistanceKm();
    }
}
