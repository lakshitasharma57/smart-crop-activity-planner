# Smart Crop Activity Planner

**Project code:** JAVA-P035  |  **Course:** 25CSE0202 – Programming in Java
**Student:** Lakshita Sharma  |  **Roll No.:** 2510993135  |  **Section:** G-6

A Java console application that plans irrigation, fertilizer application and harvesting for each crop,
assigns tasks to workers, highlights overdue work and reports progress and cost per field.
Data is stored in an **SQLite database through JDBC**.

Documents: [Problem statement](docs/PROBLEM_STATEMENT.md) | [Design and diagrams](docs/DESIGN.md) | [Test evidence](docs/TEST_EVIDENCE.md)

## Requirements covered
| # | Functional requirement | Where it lives |
|---|---|---|
| 1 | Register farms and crop records | `FarmService`, `Farm`, `CropRecord` |
| 2 | Predefined crop schedules | `ScheduleCatalog`, `ActivityTemplate` |
| 3 | Create, update, complete tasks with worker details | `TaskService`, `Task`, `Worker` |
| 4 | Overdue activities, chronological history | `TaskService.getOverdue`, `getHistory`, `OverdueMonitor` |
| 5 | Crop progress, upcoming tasks, field-wise cost | `ReportService` |

## Course concepts used
| Concept | Where |
|---|---|
| OOP: abstraction, inheritance, polymorphism, interfaces, enums, `static`, `this` | `Person` -> `Farmer`/`Worker`, `CsvSerializable`, `ActivityType`, `Task.BY_SCHEDULE` |
| Strings, `StringBuilder`, wrapper classes | `ReportService`, `CropRecord.capitalise`, `Integer.parseInt` in `fromCsv` |
| Exceptions (checked, unchecked, custom, multi-catch, `finally`) | `exception` package, `CsvStore.load`, `Main`, `PlannerRepository.inTransaction` |
| Multi-dimensional array | `ReportService.activityCostMatrix` (`double[][]`) |
| Collections: `ArrayList`, `HashMap`, `TreeSet`, `PriorityQueue` | `PlannerData`, `ScheduleCatalog` (TreeSet of crop names), cost totals in `ReportService`, `ReportService.nextTasksReport` (PriorityQueue) |
| `Comparator`, sorting | `Task.BY_SCHEDULE`, history ordering |
| Threads, thread states, `synchronized`, daemon thread, `sleep`, `interrupt`, `join` | `OverdueMonitor` (menu option 18 shows its state), synchronized methods of `TaskService` |
| JDBC: `DriverManager`, `PreparedStatement`, `ResultSet`, CRUD, transactions | `DatabaseManager`, `PlannerRepository` |
| Lambdas, functional interfaces, method references, Streams | `ReportService`, `TaskService`, `SqlWork`, `Farm::fromCsv` |
| Generics | `CsvStore<T extends CsvSerializable>`, `List<? extends ...>` style helpers |
| File handling (text/CSV, collection import/export) | `CsvStore`, `CsvTransfer` |

## Open and run in Eclipse
1. Use **Eclipse IDE for Java Developers** with a **JDK 17 or newer** (it already contains Maven support).
2. Unzip the project. **File -> Import -> Maven -> Existing Maven Projects**, choose the unzipped folder
   (the one containing `pom.xml`), **Finish**. Eclipse downloads the SQLite driver automatically
   (internet needed the first time).
3. Right-click `src/main/java/com.cropplanner/Main.java` -> **Run As -> Java Application**.
   Type your choices in Eclipse's **Console** tab.
4. First run: choose **15** to load the sample data (it is only allowed into an empty database).
5. Tests: right-click the project -> **Run As -> JUnit Test**.

The database file `data/planner.db` is created inside the project folder on first run.

### If you do not use Maven
Create a normal Java project, copy the `src` folder and `data` folder into it, then add the
`sqlite-jdbc-3.46.1.3.jar` file: **Project -> Properties -> Java Build Path -> Libraries -> Classpath ->
Add External JARs**. Run `Main` as above.

## Menu
Register farm, add crop, generate standard schedule, add/update/complete tasks, upcoming tasks,
overdue activities, completed history, crop progress, field-wise cost summary (with a field x activity
matrix), standard schedules, CSV import and export, the 5 most urgent tasks (option 17) and the
background monitor status (option 18).

## Packages
| Package | Responsibility |
|---|---|
| `model` | Farm, crop, task, worker and enum types |
| `exception` | Custom exceptions |
| `storage` | `PlannerData` (collections), JDBC (`DatabaseManager`, `PlannerRepository`), CSV (`CsvStore`, `CsvTransfer`) |
| `service` | Business rules and reports |
| `concurrent` | `OverdueMonitor` background thread |
| `ui` | Console menu and validated input |
