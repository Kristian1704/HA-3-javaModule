import med.supply.system.model.*;
import med.supply.system.repository.Repository;
import med.supply.system.service.StorageService;
import med.supply.system.util.*;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDate;

public class StorageServiceTest {

    public static void main(String[] args) {
        System.out.println("Running StorageService tests...");
        try {
            PathsConfig cfg = new PathsConfig();
            cfg.ensure();

            Repository repo = new Repository();
            LogManager logManager = new LogManager(cfg);
            StorageService service = new StorageService(repo, logManager);

            testAddVehicle(service, repo, cfg);
            testAddChargingStation(service, repo, cfg);
            testUpdateChargingLoad(service, repo, cfg);
            testAddItem(service, repo, cfg);   // ✅ updated test
            testInvalidNameThrows(service);

            System.out.println("All StorageService tests finished.");
        } catch (Exception e) {
            System.err.println("StorageService tests failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ---------- TEST 1 ----------
    private static void testAddVehicle(StorageService service, Repository repo, PathsConfig cfg) throws IOException {
        StorageVehicle v = new StorageVehicle("V001", "Van_Beta");
        service.addVehicle(v);

        assert repo.vehicles.containsKey("V001") : "Vehicle not added to repository";

        Path logFile = cfg.logsVehicles.resolve("Van_Beta").resolve(LocalDate.now().toString() + ".log");
        assert Files.exists(logFile) : "Vehicle log not created";

        String logContent = Files.readString(logFile);
        assert logContent.contains("created") : "Vehicle creation log missing";

        System.out.println("Test 1 passed (addVehicle)");
    }

    // ---------- TEST 2 ----------
    private static void testAddChargingStation(StorageService service, Repository repo, PathsConfig cfg) throws IOException {
        ChargingStation s = new ChargingStation("S001", "Station_Alpha");
        service.addChargingStation(s);

        assert repo.stations.containsKey("S001") : "Charging station not added to repository";

        Path logFile = cfg.logsCharging.resolve("Station_Alpha").resolve(LocalDate.now().toString() + ".log");
        assert Files.exists(logFile) : "Charging station log not created";

        String logContent = Files.readString(logFile);
        assert logContent.contains("added") : "Charging creation log missing";

        System.out.println("Test 2 passed  (addChargingStation)");
    }

    // ---------- TEST 3 ----------
    private static void testUpdateChargingLoad(StorageService service, Repository repo, PathsConfig cfg) throws IOException {
        ChargingStation s = new ChargingStation("S002", "Station_LoadTest");
        service.addChargingStation(s);

        // 1 = occupy, 0 = release
        service.updateChargingLoad("S002", 1);
        assert repo.stations.get("S002").isInUse() : "Charging station should be in use";

        service.updateChargingLoad("S002", 0);
        assert !repo.stations.get("S002").isInUse() : "Charging station should be free";

        System.out.println("Test 3 passed  (updateChargingLoad)");
    }

    // ---------- TEST 4 (UPDATED ✅) ----------
    private static void testAddItem(StorageService service, Repository repo, PathsConfig cfg) throws IOException {
        StorageItem item = new StorageItem("SKU123", "Bandages", 5);

        // Correct method in StorageService
        service.addItem(item);

        // Validate item is in unassigned list
        assert service.getUnassignedItemsRef().containsKey("SKU123") : "Item not added to unassigned list";

        StorageItem stored = service.getUnassignedItemsRef().get("SKU123");
        assert stored != null : "Stored item is null";
        assert stored.getSku().equals("SKU123") : "Incorrect SKU stored";
        assert stored.getQuantity() == 5 : "Incorrect quantity stored";

        // Validate system log (matches addItem logging)
        Path logFile = cfg.logsSystem.resolve(LocalDate.now().toString() + ".log");
        assert Files.exists(logFile) : "System log not created";

        String logContent = Files.readString(logFile);
        assert logContent.contains("Added unassigned item")
                : "Unassigned item addition not logged";

        System.out.println("Test 4 passed  (addItem)");
    }

    // ---------- TEST 5 ----------
    private static void testInvalidNameThrows(StorageService service) {
        try {
            StorageVehicle invalid = new StorageVehicle("V003", "###Invalid###");
            service.addVehicle(invalid);
            assert false : "Expected IllegalArgumentException not thrown for invalid name";
        } catch (IllegalArgumentException | IOException e) {
            System.out.println("Test 5 passed (requireValidName throws)");
        }
    }
}
