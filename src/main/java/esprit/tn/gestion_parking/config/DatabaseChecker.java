package esprit.tn.gestion_parking.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class DatabaseChecker implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseChecker.class);

    @Autowired
    private MongoTemplate mongoTemplate;

    @Override
    public void run(String... args) throws Exception {
        logger.info("--- 🛡️ DATABASE CONNECTION CHECK ---");

        try {
            // 1. Check Database Name
            String dbName = mongoTemplate.getDb().getName();
            logger.info("Successfully connected to Database: [{}]", dbName);

            // 2. List Collections
            Set<String> collections = mongoTemplate.getCollectionNames();

            if (collections.isEmpty()) {
                logger.warn("⚠️ No collections found yet! (They will be created after your first POST request)");
            } else {
                logger.info("✅ Found {} collections:", collections.size());
                collections.forEach(name -> logger.info("   -> Collection: {}", name));
            }

        } catch (Exception e) {
            logger.error("❌ CRITICAL: Could not connect to MongoDB. Check if 'mongod' is running!");
        }

        logger.info("-----------------------------------");
    }
}