package com.microcommerce.productservice.repository;

import com.microcommerce.productservice.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findByCategoryId(Long categoryId);

    // Single conditional UPDATE so concurrent orders cannot both take the last unit.
    // Returns 0 when the product is missing or has too little stock.
    @Modifying
    @Query("update Product p set p.stockLevel = p.stockLevel - :quantity where p.id = :id and p.stockLevel >= :quantity")
    int decrementStock(Long id, int quantity);

    @Modifying
    @Query("update Product p set p.stockLevel = p.stockLevel + :quantity where p.id = :id")
    int incrementStock(Long id, int quantity);
}
