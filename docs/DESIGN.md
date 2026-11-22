# Design

## Layers and data flow
```mermaid
flowchart LR
    U[User] --> UI[ui: ConsoleMenu + InputReader]
    UI --> S[service: FarmService, TaskService, ReportService]
    S --> M[(In-memory data: HashMap farms and crops, ArrayList tasks)]
    UI --> R[storage: PlannerRepository - JDBC]
    R --> DB[(SQLite database planner.db)]
    UI --> C[storage: CsvTransfer - CSV files]
    T[concurrent: OverdueMonitor thread] --> S
```
1. At start-up `PlannerRepository.load()` reads every table with `ResultSet` and fills `PlannerData`.
2. Business rules and reports work on the in-memory collections (Streams, Comparators).
3. After every change the menu writes it to the database with a `PreparedStatement` inside a transaction.
4. `OverdueMonitor` is a daemon thread that recounts overdue tasks every few seconds.

## Class diagram
```mermaid
classDiagram
    class Person {
        <<abstract>>
        -name
        -phone
        +getRole()*
    }
    class Farmer
    class Worker
    Person <|-- Farmer
    Person <|-- Worker

    class CsvSerializable {
        <<interface>>
        +toCsv()
    }
    class Farm { -id -name -location }
    class CropRecord { -id -fieldName -cropType -areaAcres -sowingDate -expectedHarvestDate }
    class Task { -id -type -scheduledDate -status -estimatedCost -actualCost +isOverdue() +markCompleted() }
    class ActivityType {
        <<enumeration>>
        IRRIGATION
        FERTILIZER
        HARVESTING
    }
    CsvSerializable <|.. Farm
    CsvSerializable <|.. CropRecord
    CsvSerializable <|.. Task
    Farm "1" --> "1" Farmer : owner
    Farm "1" --> "*" CropRecord
    CropRecord "1" --> "*" Task
    Task --> Worker : responsible
    Task --> ActivityType

    class FarmService
    class TaskService
    class ReportService
    class ScheduleCatalog
    TaskService --> ScheduleCatalog
    class PlannerRepository
    class OverdueMonitor
    OverdueMonitor --> TaskService
```

## Database design
```mermaid
erDiagram
    FARMS ||--o{ CROPS : grows
    CROPS ||--o{ TASKS : has
    FARMS { int id PK
            text name UK
            text location
            text owner_name
            text owner_phone }
    CROPS { int id PK
            int farm_id FK
            text field_name
            text crop_type
            real area_acres
            text sowing_date
            text expected_harvest_date }
    TASKS { int id PK
            int crop_id FK
            text type
            text scheduled_date
            text worker_name
            text worker_phone
            real estimated_cost
            text status
            text completed_date
            real actual_cost }
```
Dates are stored as ISO text (`yyyy-MM-dd`). Foreign keys, a unique farm name, `area_acres > 0` and
`status IN ('PENDING','COMPLETED')` are enforced by the database itself.

## Key design decisions
| Decision | Reason |
|---|---|
| Collections in memory, JDBC for persistence | Reports and rules stay simple with Streams/Comparators and can be unit tested without a database |
| `Person` abstract class with `Farmer` and `Worker` | Shared validation (name, 10-digit phone) with a role-specific `getRole()` |
| `CsvStore<T extends CsvSerializable>` | One generic reader/writer instead of three copies |
| Transactions for multi-row writes | A generated schedule or a CSV import is saved completely or not at all |
| `TaskService` methods are `synchronized` | The monitor thread reads the task list while the user changes it |
| Daemon monitor thread | Tasks become overdue with the passing of time, not only after user actions |
| Cost matrix uses a `double[][]` | Fixed table of fields x activity types with row and column totals |
