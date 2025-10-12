package com.example.coffeedms;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages an in-memory list of CoffeeBean objects.
 * Enforces unique beanID on both manual add and batch-load.
 */
public class BeanRepository {
    private final List<CoffeeBean> beans = new ArrayList<>();

    /**
     * Reads a CSV file of bean records, validates each line,
     * and adds only those with a new beanID.
     *
     * @param path filesystem path to CSV
     * @return list of beans actually loaded (i.e., non-duplicates)
     * @throws IOException if the file cannot be read
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
                    // Skip if ID already exists
                    if (findByID(b.getBeanID()) == null) {
                        beans.add(b);
                        loaded.add(b);
                    }
                } catch (Exception ex) {
                    // invalid line or duplicate → skip quietly
                    System.err.println("Skipping invalid/duplicate line: " + line);
                }
            }
        }
        return loaded;
    }

    /**
     * Adds a new bean if its beanID is unique.
     *
     * @param bean the CoffeeBean to add
     * @return true if added; false if a bean with same ID already exists
     */
    public boolean add(CoffeeBean bean) {
        if (findByID(bean.getBeanID()) != null) {
            return false;
        }
        return beans.add(bean);
    }

    public boolean removeByID(String beanID) {
        return beans.removeIf(b -> b.getBeanID().equals(beanID));
    }

    public boolean update(CoffeeBean updatedBean) {
        for (int i = 0; i < beans.size(); i++) {
            if (beans.get(i).getBeanID().equals(updatedBean.getBeanID())) {
                beans.set(i, updatedBean);
                return true;
            }
        }
        return false;
    }

    public CoffeeBean findByID(String beanID) {
        for (CoffeeBean b : beans) {
            if (b.getBeanID().equals(beanID)) return b;
        }
        return null;
    }

    public List<CoffeeBean> findAll() {
        return new ArrayList<>(beans);
    }

    /**
     * Sums the value of each bean (quantity × cost).
     */
    public BigDecimal calculateTotalInventoryValue() {
        BigDecimal sum = BigDecimal.ZERO;
        for (CoffeeBean b : beans) {
            sum = sum.add(b.calculateValue());
        }
        return sum;
    }
}
