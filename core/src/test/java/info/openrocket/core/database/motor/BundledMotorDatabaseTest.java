package info.openrocket.core.database.motor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import info.openrocket.core.motor.ThrustCurveMotor;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Integration tests that verify the content of the bundled initial_motors.db.
 * These tests guard against regressions in motor deduplication and common name normalization.
 */
public class BundledMotorDatabaseTest {

	@TempDir
	static Path tempDir;

	private static ThrustCurveMotorSetDatabase motorSetDatabase;

	@BeforeAll
	static void loadBundledDatabase() throws Exception {
		InputStream is = BundledMotorDatabaseTest.class.getResourceAsStream(
				"/datafiles/thrustcurves/initial_motors.db");
		assertFalse(is == null, "Bundled initial_motors.db resource not found");

		File dbFile = tempDir.resolve("initial_motors.db").toFile();
		try (OutputStream os = Files.newOutputStream(dbFile.toPath())) {
			is.transferTo(os);
		}

		List<ThrustCurveMotor> allMotors = ThrustCurveMotorSQLiteDatabase.readDatabase(dbFile);
		motorSetDatabase = new ThrustCurveMotorSetDatabase();
		for (ThrustCurveMotor motor : allMotors) {
			motorSetDatabase.addMotor(motor);
		}
	}

	/**
	 * Estes B6 must appear as exactly one motor set with designation "B6" and common name "B6".
	 * This guards against the B6 / B6-0 duplicate that arose because some data sources
	 * stored the motor with the delay embedded in the designation ("B6-0").
	 */
	@Test
	void testEstesB6HasExactlyOneMotorSet() {
		List<ThrustCurveMotorSet> estesB6Sets = motorSetDatabase.getMotorSets().stream()
				.filter(set -> set.getDesignation().equals("B6")
						&& set.getManufacturer().matches("Estes"))
				.collect(Collectors.toList());

		assertEquals(1, estesB6Sets.size(),
				"Expected exactly one Estes B6 motor set, found: " + estesB6Sets.size());
		assertEquals("B6", estesB6Sets.get(0).getCommonName(),
				"Estes B6 common name should be 'B6', not '" + estesB6Sets.get(0).getCommonName() + "'");
	}

	/**
	 * Quest B6W must have designation "B6W" and common name "B6".
	 * The common name must not retain the propellant-code suffix ("B6W").
	 */
	@Test
	void testQuestB6WCommonName() {
		List<ThrustCurveMotorSet> questB6WSets = motorSetDatabase.getMotorSets().stream()
				.filter(set -> set.getDesignation().equals("B6W")
						&& set.getManufacturer().matches("Quest"))
				.collect(Collectors.toList());

		assertFalse(questB6WSets.isEmpty(), "No Quest B6W motor set found in the bundled database");
		assertEquals("B6", questB6WSets.get(0).getCommonName(),
				"Quest B6W common name should be 'B6', not '" + questB6WSets.get(0).getCommonName() + "'");
	}
}
