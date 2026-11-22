# Problem Statement, Objectives and Scope

**Project:** JAVA-P035 – Smart Crop Activity Planner  |  **Student:** Lakshita Sharma

## Problem statement
Small and medium farms plan irrigation, fertilizer application and harvesting from memory, notebooks or
messages to workers. Dates are missed, nobody can say quickly which work is overdue, and the cost of
each field is only known at the end of the season. There is no single record of who did which work and when.

The Smart Crop Activity Planner is a Java console application that keeps this information in one place:
it registers farms and crops, builds a standard activity schedule for each crop, assigns every task to a
responsible worker, highlights overdue work and reports progress and cost per field.

## Objectives
1. Store farms and crop records (crop type, field area, sowing date, expected harvest date) in a database.
2. Provide predefined irrigation, fertilizer and harvest schedules that generate dated tasks automatically.
3. Let the user create, update and complete tasks with worker details and actual cost.
4. Identify overdue activities automatically and keep a chronological history of completed work.
5. Report crop-wise progress, upcoming tasks and field-wise (and field-by-activity) cost.

## In scope
- Single-user console application, data stored in an SQLite database through JDBC.
- Four crops with standard schedules: wheat, rice, maize, cotton.
- CSV import (into an empty database) and CSV export for backup and sharing.
- Input validation (phone numbers, dates, areas, costs) and clear error messages.

## Out of scope
- Graphical or web interface, multiple simultaneous users, login and roles.
- Weather, soil data, market prices or automatic SMS/e-mail reminders.
- Deleting farms, crops or tasks (records are kept for history).

## Use cases
| Use case | Actor | Result |
|---|---|---|
| Register farm and crop | Farm owner | Farm and crop saved |
| Generate standard schedule | Farm owner | Dated tasks created for the crop |
| Assign / reassign worker, reschedule task | Farm owner | Task updated |
| Complete task with actual cost | Farm owner / supervisor | Task moves to history |
| View overdue and upcoming work | Farm owner | Sorted lists |
| View progress and cost reports | Farm owner | Text reports |
