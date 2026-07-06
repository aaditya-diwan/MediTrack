package com.meditrack.labrotary_service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

// The Flyway migrations use PostgreSQL-only syntax (plpgsql triggers, gen_random_uuid()),
// which the in-memory H2 test database cannot execute. For the context-load smoke test we
// disable Flyway and let Hibernate generate the schema from the entities instead.
@SpringBootTest(properties = {
		"spring.flyway.enabled=false",
		"spring.jpa.hibernate.ddl-auto=create-drop"
})
class LabrotaryServiceApplicationTests {

	@Test
	void contextLoads() {
	}

}
