package com.smartpark.backend.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import com.smartpark.backend.model.CompanySettings;

@Repository
public interface ISettingsRepo extends MongoRepository<CompanySettings, String> {

}

