# Test Evidence

## 1. Automated tests (JUnit)
Run in Eclipse: right-click the project -> **Run As -> JUnit Test**, or `mvn test`.
Paste a screenshot of the green JUnit view here: `docs/junit-result.png`

| Test class | What it proves |
|---|---|
| `ModelValidationTest` | Bad phone, area, dates are rejected; crop type is normalised; CSV round trip |
| `TaskServiceTest` | Schedule generation, no duplicate schedule, cost estimate, overdue logic, chronological history, completed tasks are locked, cost report |
| `OverdueMonitorTest` | Overdue count follows the clock and completed work; monitor thread starts and stops cleanly |
| `PlannerRepositoryTest` | JDBC save/load round trip, update of a completed task, rollback of a failed batch insert |

## 2. Manual test cases (console)
Fill the **Actual result** column from your own run and attach screenshots.

| # | Menu | Input | Expected result | Actual result |
|---|---|---|---|---|
| 1 | 15 | (empty database) | "Imported 2 farms, 3 crops and 20 tasks" | |
| 2 | 15 | second time | Error: import only allowed while database is empty | |
| 3 | 1 | owner phone `12345` | Invalid input: phone must be 10 digits | |
| 4 | 3 | harvest date earlier than sowing date | Invalid input: harvest date must be after sowing date | |
| 5 | 3 | crop `Barley`, answer `y` to schedule | Error: no predefined schedule, lists supported crops | |
| 6 | 5 | crop that already has tasks | Error: crop already has scheduled tasks | |
| 7 | 10 | after import | Overdue list with "days late" for each task | |
| 8 | 8 | an overdue task, cost 11250 | Task completed; overdue count in header decreases | |
| 9 | 8 | same task again | Error: already completed | |
| 10 | 7 | task id `999` | Error: Task with id 999 was not found | |
| 11 | 11 | - | Completed work in date order | |
| 12 | 13 | - | Field-wise table and field x activity matrix with totals | |
| 13 | 16 | - | Three CSV files appear in `data/export` | |
| 14 | 17 | after import | Five tasks, earliest due date first (overdue ones on top) | |
| 15 | 18 | - | Thread state (e.g. TIMED_WAITING), daemon: true | |
| 16 | 0, then run again | - | Data is still there (stored in `data/planner.db`) | |
