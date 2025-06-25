package com.example.coffeedms;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Joses De Carlos, CEN3024C 31774, 06-18-2025
 * Manages an in-memory list of CoffeeBean objects.
 * Provides methods for CRUD operations and the custom action.
 */
public class BeanRepository {
    private final List<CoffeeBean> beans = new ArrayList<>();

    /**
     * loadFromFile
     *
     * Reads a CSV file of bean records, validates each line,
     * and adds valid beans to the repository.
     *
     * @param path filesystem path to CSV
     * @return list of successfully loaded beans
     * @throws IOException if file cannot be read
     */
    public List<CoffeeBean> loadFromFile(String path) throws IOException {
        List<CoffeeBean> loaded = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(path))) {
            String line;
            while ((line = reader.readLine()) != null) {
                try {
                    String[] p = line.split(",");
                    CoffeeBean b = new CoffeeBean(
                            p[0].trim(),
                            p[1].trim(),
                            p[2].trim(),
                            RoastLevel.fromString(p[3]),
                            LocalDate.parse(p[4].trim()),
                            Double.parseDouble(p[5].trim()),
                            new BigDecimal(p[6].trim()),
                            p[7].trim(),
                            Double.parseDouble(p[8].trim())
                    );
                    beans.add(b);
                    loaded.add(b);
                } catch (Exception ex) {
                    System.err.println("Skipping invalid line: " + line);
                }
            }
        }
        return loaded;
    }

    /**
     * add
     *
     * Inserts a new CoffeeBean into the list.
     *
     * @param bean bean to add
     * @return true if added successfully
     */
    public boolean add(CoffeeBean bean) {
        return beans.add(bean);
    }

    /**
     * removeByID
     *
     * Deletes the bean whose ID matches the argument.
     *
     * @param beanID identifier of the bean to remove
     * @return true if a bean was removed
     */
    public boolean removeByID(String beanID) {
        return beans.removeIf(b -> b.getBeanID().equals(beanID));
    }

    /**
     * update
     *
     * Replaces an existing bean (by ID) with the provided object.
     *
     * @param updatedBean bean containing updated data
     * @return true if the update succeeded
     */
    public boolean update(CoffeeBean updatedBean) {
        for (int i = 0; i < beans.size(); i++) {
            if (beans.get(i).getBeanID().equals(updatedBean.getBeanID())) {
                beans.set(i, updatedBean);
                return true;
            }
        }
        return false;
    }

    /**
     * findByID
     *
     * Searches for a bean with the given ID.
     *
     * @param beanID identifier to look up
     * @return matching CoffeeBean or null if not found
     */
    public CoffeeBean findByID(String beanID) {
        for (CoffeeBean b : beans) {
            if (b.getBeanID().equals(beanID)) {
                return b;
            }
        }
        return null;
    }

    /**
     * findAll
     *
     * Returns a snapshot list of all beans.
     *
     * @return copy of the bean list
     */
    public List<CoffeeBean> findAll() {
        return new ArrayList<>(beans);
    }

    /**
     * calculateTotalInventoryValue
     *
     * Sums the value of each bean (quantity × cost).
     *
     * @return total inventory value as BigDecimal
     */
    public BigDecimal calculateTotalInventoryValue() {
        BigDecimal sum = BigDecimal.ZERO;
        for (CoffeeBean b : beans) {
            sum = sum.add(b.calculateValue());
        }
        return sum;
    }
}
