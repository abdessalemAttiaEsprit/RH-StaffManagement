package com.smartpark.backend.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import com.smartpark.backend.model.Payment;

import java.util.Optional;

@Repository
public interface IPaymentRepo  extends MongoRepository<Payment, String> {
    Optional<Payment> findByMatricule(String matricule);
    boolean existsByMatriculeAndMonthAndYear(String matricule, java.time.Month  month, int year);
    Optional<Payment> findByMatriculeAndMonthAndYear(String matricule, java.time.Month month, int year);
    void deleteByMatricule(String matricule);
    void deleteByMatriculeAndMonthAndYear(String matricule, java.time.Month month, int year);
    
}
