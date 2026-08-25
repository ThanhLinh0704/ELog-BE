# Report 5 Cypress UI runbook

Purpose: run L4 Web E2E from Cypress UI against a real backend and the fresh L3 fixture evidence.

## 1. Start backend with an isolated database

```powershell
$db = "elog_report5_ui_$(Get-Date -Format yyyyMMddHHmmss)"
& "C:\Program Files\MySQL\MySQL Server 9.7\bin\mysql.exe" -h 127.0.0.1 -P 3307 -uroot -proot -e "CREATE DATABASE IF NOT EXISTS $db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"

cd D:\Elog\ELog-BE
$env:SPRING_DATASOURCE_URL = "jdbc:mysql://localhost:3307/$db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Ho_Chi_Minh"
$env:SPRING_DATASOURCE_USERNAME = "root"
$env:SPRING_DATASOURCE_PASSWORD = "root"
java -jar target\elog-backend-0.0.1-SNAPSHOT.jar --server.port=8080 --spring.jpa.show-sql=false
```

Keep this terminal open.

## 2. Generate fresh L3 fixture evidence

Open a second terminal:

```powershell
cd D:\Elog\ELog-BE
$env:ELOG_BASE_URL = "http://localhost:8080"
$env:ELOG_MYSQL_DATABASE = $db
node test-execution\scripts\run-l3-api.mjs
```

Expected L3 result: `130 Pass / 0 Fail / 0 Not Run`.

## 3. Start frontend

Open a third terminal:

```powershell
cd D:\Elog\ELog-FE
npm run dev -- --host 127.0.0.1 --port 5173
```

## 4. Open Cypress UI for L4

Open a fourth terminal:

```powershell
cd D:\Elog\ELog-FE\src\Test
npx cypress open --e2e --config "specPattern=e2e/l4/report5-web.cy.ts" --env "REPORT5_FIXTURE_EVIDENCE=../../../ELog-BE/test-execution/evidence/l3-rerun-results.json,apiBaseUrl=http://localhost:8080"
```

In Cypress UI, choose `report5-web.cy.ts`.

## Current local Cypress blocker

On this machine, Cypress currently fails before spec execution:

- `Cypress.exe: bad option: --smoke-test`
- after reinstall/pin attempts, runner exits with native code `-1073741795`

If this appears in Cypress UI too, reinstall/repair Cypress Desktop runtime first:

```powershell
cd D:\Elog\ELog-FE\src\Test
npx cypress install --force
npx cypress verify
```

Do not mark L4 Pass from prepared code alone. Mark Pass only after Cypress UI creates fresh evidence/JUnit/screenshots.
