# Champions League Manager

A full-stack web application that manages a UEFA Champions League **"Swiss model"**
league phase. Administrators register the qualified teams into four pots, the system
generates a valid fixture list following the official draw rules, and admins enter
results. Fans (users) browse fixtures, scorelines, the league table and statistics.

## Tech stack

| Layer    | Technology                                   |
|----------|----------------------------------------------|
| Backend  | Java 21, Spring Boot 3.4, Spring Security (JWT), Spring Data JPA |
| Database | MySQL 8                                       |
| Frontend | React 18, Vite, React Router, Axios          |

## Roles

- **ADMIN** – register/edit/delete teams, generate the draw, enter & clear results.
- **USER** – read-only: fixtures, scorelines, table, stats, teams. Anyone (even
  logged-out) can browse; only admins can change data.

Seeded accounts (created on first backend boot):

| Username | Password  | Role  |
|----------|-----------|-------|
| `admin`  | `admin123`| ADMIN |
| `user`   | `user123` | USER  |

Fans can also self-register from the login screen (always created as USER).

## Fixture rules enforced by the generator

1. Teams are split across **4 pots** (must be equal size, ≥ 3 each).
2. Each team plays **2 opponents from every pot — one home, one away** (8 matches,
   4 home + 4 away).
3. A team **never** faces a club from its **own country**.
4. A team faces **at most 2 clubs from any single country**.

The opponent graph is built with randomized backtracking (fail-first heuristic) and
retried until valid. Home/away is assigned by orienting each pot-pair's 2-regular
graph as directed cycles, guaranteeing exactly one home + one away per pot. Matches
are then spread across matchdays so no team plays twice on the same day.

---

## Running locally

### Prerequisites
- JDK 21+  ·  Maven 3.9+ / 4  ·  Node 18+  ·  MySQL 8 running on `localhost:3306`

The backend expects MySQL user `root` / password `root` and auto-creates the
`ucl_db` schema. Adjust in `backend/src/main/resources/application.properties` if
your credentials differ.

### 1. Backend
```bash
cd backend
mvn spring-boot:run
```
API runs at **http://localhost:8081**.

### 2. Frontend
```bash
cd frontend
npm install
npm run dev
```
App runs at **http://localhost:5173** (proxies `/api` to the backend).

### Quick demo
1. Open http://localhost:5173 and log in as `admin` / `admin123`.
2. Go to **Manage Teams → "Load sample (36 teams)"** to load the real 2024/25 line-up
   (or register your own — 4 equal pots).
3. Go to **Manage Fixtures → "Generate draw"**.
4. Enter some results, then view **Fixtures / Table / Stats** as any user.

---

## REST API summary

| Method | Path                         | Access | Description                  |
|--------|------------------------------|--------|------------------------------|
| POST   | `/api/auth/login`            | public | Login, returns JWT           |
| POST   | `/api/auth/register`         | public | Self-register a fan          |
| GET    | `/api/teams`                 | public | List teams                   |
| POST   | `/api/teams`                 | admin  | Register a team              |
| PUT    | `/api/teams/{id}`            | admin  | Update a team                |
| DELETE | `/api/teams/{id}`            | admin  | Delete a team                |
| POST   | `/api/teams/load-sample`     | admin  | Load 36 sample teams         |
| GET    | `/api/fixtures`              | public | All fixtures                 |
| GET    | `/api/fixtures/by-matchday`  | public | Fixtures grouped by matchday |
| POST   | `/api/fixtures/generate`     | admin  | Generate the draw            |
| PUT    | `/api/fixtures/{id}/result`  | admin  | Enter a result               |
| PATCH  | `/api/fixtures/{id}/clear`   | admin  | Clear a result               |
| DELETE | `/api/fixtures`              | admin  | Reset all fixtures           |
| GET    | `/api/standings`             | public | League table                 |
| GET    | `/api/stats`                 | public | Aggregate statistics         |
