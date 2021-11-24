package org.matsim.episim.model.vaccination;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.episim.EpisimPerson;
import org.matsim.episim.EpisimTestUtils;
import org.matsim.episim.VaccinationConfigGroup;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SplittableRandom;

import static org.assertj.core.api.Assertions.assertThat;

public class VaccinationFromDataTest {

	private static final String URL = "https://raw.githubusercontent.com/robert-koch-institut/COVID-19-Impfungen_in_Deutschland/master/Aktuell_Deutschland_Landkreise_COVID-19-Impfungen.csv";

	private static File input;

	private VaccinationFromData model;
	private Map<Id<Person>, EpisimPerson> persons = new HashMap<>();


	@BeforeClass
	public static void beforeClass() throws Exception {

		input = File.createTempFile( "episim-vac", "csv");
		input.deleteOnExit();

		try (InputStream is = new URL(URL).openStream()) {
			Files.copy(is, input.toPath(), StandardCopyOption.REPLACE_EXISTING);
		}

		List<String> lines = Files.readAllLines(input.toPath());

		// Check if format is still the same
		assertThat(lines.get(0)).isEqualTo("Impfdatum,LandkreisId_Impfort,Altersgruppe,Impfschutz,Anzahl");

	}

	@Before
	public void setUp() throws Exception {
		VaccinationConfigGroup config = new VaccinationConfigGroup();
		config.setFromFile(input.toString());

		SplittableRandom rnd = new SplittableRandom(0);

		persons = new HashMap<>();

		for (int i = 0; i < 1000; i++) {
			EpisimPerson p = EpisimTestUtils.createPerson(true, rnd.nextInt(12, 100));
			persons.put(p.getPersonId(), p);
		}

		model = new VaccinationFromData(rnd, config);

		// TODO Comment in when ready

		//model.init(rnd, persons, null, null);
	}

	@Test
	public void vaccination() {

		// TODO:
		//model.handleVaccination(persons, false, 0, LocalDate.now(), 1, 0);


	}
}
