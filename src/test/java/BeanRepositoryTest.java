package com.example.coffeedms;

import org.junit.jupiter.api.*;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for BeanRepository CRUD operations and custom action.
 */
class BeanRepositoryTest {

    private BeanRepository repo;

    @BeforeEach
    void setUp() {
        repo = new BeanRepository();
    }

    /**
     * Tests that loadFromFile successfully reads valid lines.
     */
    @Test
    void testLoadFromFileSuccess() throws IOException {
        // Create a temp file with two valid records
        Path temp = Files.createTempFile("beans_test", ".txt");
        List<String> lines = List.of(
                "ID1,Country1,Farm1,LIGHT,2025-01-01,1.0,5.0,notes1,0.1",
                "ID2,Country2,Farm2,MEDIUM,2025-02-02,2.0,6.0,notes2,0.2"
        );
        Files.write(temp, lines);

        List<CoffeeBean> loaded = repo.loadFromFile(temp.toString());
        assertEquals(2, loaded.size());

        CoffeeBean b1 = loaded.get(0);
        assertEquals("ID1", b1.getBeanID());
        assertEquals(LocalDate.parse("2025-01-01"), b1.getRoastDate());

        Files.deleteIfExists(temp);
    }

    /**
     * Tests that loading from a non-existent file throws IOException.
     */
    @Test
    void testLoadFromFileFailure() {
        assertThrows(IOException.class, () -> {
            repo.loadFromFile("no_such_file.txt");
        });
    }

    /**
     * Tests that adding a bean works and can be retrieved.
     */
    @Test
    void testAddAndFind() {
        CoffeeBean bean = new CoffeeBean(
                "ID3", "Country3", "Farm3",
                RoastLevel.DARK, LocalDate.now(),
                3.0, new BigDecimal("7.0"),
                "notes3", 0.3
        );
        assertTrue(repo.add(bean));
        CoffeeBean found = repo.findByID("ID3");
        assertNotNull(found);
        assertEquals("Farm3", found.getFarmName());
    }

    /**
     * Tests that removeByID deletes an existing bean.
     */
    @Test
    void testRemoveExisting() {
        CoffeeBean bean = new CoffeeBean(
                "ID4", "Country4", "Farm4",
                RoastLevel.LIGHT, LocalDate.now(),
                4.0, new BigDecimal("8.0"),
                "notes4", 0.4
        );
        repo.add(bean);
        assertTrue(repo.removeByID("ID4"));
        assertNull(repo.findByID("ID4"));
    }

    /**
     * Tests that removeByID on a non-existent ID returns false.
     */
    @Test
    void testRemoveNonexistent() {
        assertFalse(repo.removeByID("NOPE"));
    }

    /**
     * Tests updating an existing bean’s attributes.
     */
    @Test
    void testUpdateExisting() {
        CoffeeBean original = new CoffeeBean(
                "ID5", "Country5", "Farm5",
                RoastLevel.MEDIUM, LocalDate.now(),
                5.0, new BigDecimal("9.0"),
                "notes5", 0.5
        );
        repo.add(original);

        CoffeeBean updated = new CoffeeBean(
                "ID5", "CountryUpd", "FarmUpd",
                RoastLevel.DARK, LocalDate.now(),
                5.5, new BigDecimal("9.5"),
                "notesUpd", 0.55
        );
        assertTrue(repo.update(updated));

        CoffeeBean found = repo.findByID("ID5");
        assertEquals("CountryUpd", found.getOriginCountry());
        assertEquals(RoastLevel.DARK, found.getRoastLevel());
        assertEquals(5.5, found.getQuantityKg(), 1e-6);
    }

    /**
     * Tests that update(...) on a non-existent bean returns false.
     */
    @Test
    void testUpdateNonexistent() {
        CoffeeBean bean = new CoffeeBean(
                "NO_ID", "X", "Y",
                RoastLevel.LIGHT, LocalDate.now(),
                1.0, new BigDecimal("1.0"),
                "n", 0.1
        );
        assertFalse(repo.update(bean));
    }

    /**
     * Tests the custom action calculateTotalInventoryValue.
     */
    @Test
    void testCalculateTotalInventoryValue() {
        CoffeeBean b1 = new CoffeeBean(
                "ID6", "C1", "F1",
                RoastLevel.LIGHT, LocalDate.now(),
                1.0, new BigDecimal("10.0"),
                "n", 0.1
        );
        CoffeeBean b2 = new CoffeeBean(
                "ID7", "C2", "F2",
                RoastLevel.MEDIUM, LocalDate.now(),
                2.0, new BigDecimal("20.0"),
                "n", 0.2
        );
        repo.add(b1);
        repo.add(b2);

        BigDecimal expected =
                new BigDecimal("10.0").multiply(BigDecimal.valueOf(1.0))
                        .add(new BigDecimal("20.0").multiply(BigDecimal.valueOf(2.0)));

        assertEquals(0, repo.calculateTotalInventoryValue().compareTo(expected));
    }
}
